// .NET Aspire AppHost that runs two phoss AP instances (A and B) exchanging Peppol messages with
// each other - WITHOUT official Peppol test certificates and without SMP/SML.
// For local development only. See ../README.md
using PhossAP.AppHost;

var builder = DistributedApplication.CreateBuilder(args);

string repoRoot = Path.GetFullPath(Path.Combine(builder.AppHostDirectory, "..", ".."));
string devDir = Path.Combine(repoRoot, "aspire", ".dev");
string certDir = Path.Combine(devDir, "certs");

// The locally built phoss AP jar ("mvn clean install -DskipTests")
string jarPath = Directory.Exists(Path.Combine(repoRoot, "phoss-ap-webapp", "target"))
    ? Directory.GetFiles(Path.Combine(repoRoot, "phoss-ap-webapp", "target"), "phoss-ap-webapp-*.jar")
               .Where(f => !f.EndsWith("-sources.jar") && !f.EndsWith("-tests.jar") && !f.EndsWith("-javadoc.jar"))
               .OrderByDescending(File.GetLastWriteTimeUtc)
               .FirstOrDefault() ?? ""
    : "";
if (jarPath.Length == 0)
    throw new InvalidOperationException("phoss-ap-webapp jar not found - run 'mvn clean install -DskipTests' in " + repoRoot);

(string Name, string SeatId, int Port)[] aps = [("a", "PXX000001", 8081), ("b", "PXX000002", 8082)];
DevCertificates.EnsureCreated(certDir, aps.Select(x => (x.Name, x.SeatId)).ToList());

// One PostgreSQL container with one database per AP
var pgUser = builder.AddParameter("postgres-user", "peppol");
var pgPassword = builder.AddParameter("postgres-password", "peppol", secret: true);
var postgres = builder.AddPostgres("postgres", pgUser, pgPassword)
                      .WithDataVolume("phoss-ap-aspire-pgdata");
var pgEndpoint = postgres.GetEndpoint("tcp");

var resources = new Dictionary<string, IResourceBuilder<ExecutableResource>>();
foreach (var (name, seatId, port) in aps)
{
    var db = postgres.AddDatabase($"db-{name}", $"phoss-ap-{name}");
    string dataDir = Path.Combine(devDir, $"ap-{name}");

    resources[name] = builder.AddExecutable($"ap-{name}", "java", repoRoot, "-jar", jarPath)
        .WithHttpEndpoint(port: port, name: "http", env: "SERVER_PORT", isProxied: false)
        .WithHttpHealthCheck("/actuator/health")
        .WaitFor(db)
        .WithEnvironment("GLOBAL_DATAPATH", dataDir + Path.DirectorySeparatorChar)
        .WithEnvironment("PHOSSAP_JDBC_URL",
            ReferenceExpression.Create($"jdbc:postgresql://{pgEndpoint.Property(EndpointProperty.Host)}:{pgEndpoint.Property(EndpointProperty.Port)}/phoss-ap-{name}"))
        .WithEnvironment("PHOSSAP_JDBC_USER", pgUser)
        .WithEnvironment("PHOSSAP_JDBC_PASSWORD", pgPassword)
        .WithEnvironment("PEPPOL_STAGE", "test")
        .WithEnvironment("PEPPOL_OWNER_SEATID", seatId)
        .WithEnvironment("PHASE4_API_REQUIREDTOKEN", "phoss-ap-development-token")
        .WithEnvironment("PHASE4_ENDPOINT_ADDRESS", "http://localhost:" + port + "/as4")
        // Own key store, and the own CA instead of the Peppol test CA
        .WithEnvironment("ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_FILE", Path.Combine(certDir, $"ap-{name}.p12"))
        .WithEnvironment("ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_ALIAS", DevCertificates.KeyAlias)
        .WithEnvironment("ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_PASSWORD", DevCertificates.Password)
        .WithEnvironment("ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_PRIVATE_PASSWORD", DevCertificates.Password)
        .WithEnvironment("ORG_APACHE_WSS4J_CRYPTO_MERLIN_TRUSTSTORE_FILE", Path.Combine(certDir, "dev-truststore.p12"))
        .WithEnvironment("ORG_APACHE_WSS4J_CRYPTO_MERLIN_TRUSTSTORE_PASSWORD", DevCertificates.Password)
        .WithEnvironment("PEPPOL_DEV_TRUSTED_CA_PATH", Path.Combine(certDir, "dev-ca.pem"))
        // Received documents are written to this directory
        .WithEnvironment("FORWARDING_MODE", "filesystem")
        .WithEnvironment("FORWARDING_FILESYSTEM_DIRECTORY", Path.Combine(dataDir, "fwd"))
        // Traces, metrics and logs in the Aspire dashboard
        .WithEnvironment("OTEL_ENABLED", "true")
        .WithEnvironment("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")
        .WithOtlpExporter();
}

// A sends everything to B and vice versa - no SMP lookup
foreach (var (name, _, _) in aps)
{
    string other = name == "a" ? "b" : "a";
    var otherEndpoint = resources[other].GetEndpoint("http");
    resources[name]
        .WithEnvironment("OUTBOUND_DEV_FIXED_ENDPOINT_URL", ReferenceExpression.Create($"{otherEndpoint}/as4"))
        .WithEnvironment("OUTBOUND_DEV_FIXED_ENDPOINT_CERTIFICATE_PATH", Path.Combine(certDir, $"ap-{other}.pem"));
}

builder.Build().Run();
