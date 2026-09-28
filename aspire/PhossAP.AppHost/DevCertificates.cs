using System.Diagnostics;

namespace PhossAP.AppHost;

/// <summary>
/// Creates a self-made CA and one AP certificate per instance with the JDK "keytool", so that the
/// phoss AP instances can exchange messages without official Peppol test certificates.
/// For local development only. Existing files are reused.
/// </summary>
internal static class DevCertificates
{
    public const string Password = "peppol";
    public const string KeyAlias = "ap";

    public static void EnsureCreated(string certDir, IReadOnlyList<(string Name, string SeatId)> aps)
    {
        Directory.CreateDirectory(certDir);

        string caKeyStore = Path.Combine(certDir, "dev-ca.p12");
        string caPem = Path.Combine(certDir, "dev-ca.pem");
        string trustStore = Path.Combine(certDir, "dev-truststore.p12");

        if (!File.Exists(caPem))
        {
            // Start from scratch, because all AP certificates must be issued by the same CA
            foreach (string f in Directory.GetFiles(certDir))
                File.Delete(f);

            KeyTool("-genkeypair", "-alias", "ca", "-keyalg", "RSA", "-keysize", "2048", "-validity", "730",
                    "-dname", "CN=phoss AP DEV CA, O=phoss AP local development, C=XX",
                    "-ext", "bc:c=ca:true", "-ext", "ku:c=keyCertSign,cRLSign",
                    "-keystore", caKeyStore, "-storetype", "PKCS12", "-storepass", Password);
            KeyTool("-exportcert", "-rfc", "-alias", "ca", "-file", caPem,
                    "-keystore", caKeyStore, "-storepass", Password);
            KeyTool("-importcert", "-noprompt", "-alias", "dev-ca", "-file", caPem,
                    "-keystore", trustStore, "-storetype", "PKCS12", "-storepass", Password);
        }

        foreach (var (name, seatId) in aps)
        {
            string apKeyStore = Path.Combine(certDir, $"ap-{name}.p12");
            string apPem = Path.Combine(certDir, $"ap-{name}.pem");
            if (File.Exists(apPem))
                continue;

            string csr = Path.Combine(certDir, $"ap-{name}.csr");
            File.Delete(apKeyStore);

            // The CN is the Peppol Seat ID
            KeyTool("-genkeypair", "-alias", KeyAlias, "-keyalg", "RSA", "-keysize", "2048", "-validity", "730",
                    "-dname", $"CN={seatId}, OU=PEPPOL TEST AP, O=phoss AP local development, C=XX",
                    "-keystore", apKeyStore, "-storetype", "PKCS12", "-storepass", Password);
            KeyTool("-certreq", "-alias", KeyAlias, "-file", csr,
                    "-keystore", apKeyStore, "-storepass", Password);
            KeyTool("-gencert", "-rfc", "-alias", "ca", "-validity", "730", "-infile", csr, "-outfile", apPem,
                    "-ext", "bc:c=ca:false", "-ext", "ku:c=digitalSignature,keyEncipherment,dataEncipherment,keyAgreement",
                    "-keystore", caKeyStore, "-storepass", Password);
            // The CA must be known in the key store, so that the signed certificate can be imported
            KeyTool("-importcert", "-noprompt", "-alias", "ca", "-file", caPem,
                    "-keystore", apKeyStore, "-storepass", Password);
            KeyTool("-importcert", "-alias", KeyAlias, "-file", apPem,
                    "-keystore", apKeyStore, "-storepass", Password);
            File.Delete(csr);
        }
    }

    private static string FindKeyTool()
    {
        string exe = OperatingSystem.IsWindows() ? "keytool.exe" : "keytool";
        string? javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrEmpty(javaHome))
        {
            string candidate = Path.Combine(javaHome, "bin", exe);
            if (File.Exists(candidate))
                return candidate;
        }
        // Rely on the PATH
        return exe;
    }

    private static void KeyTool(params string[] args)
    {
        var psi = new ProcessStartInfo(FindKeyTool())
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
        };
        foreach (string arg in args)
            psi.ArgumentList.Add(arg);

        using Process process = Process.Start(psi)
            ?? throw new InvalidOperationException("Failed to start keytool - is a JDK installed and JAVA_HOME set?");
        string stdout = process.StandardOutput.ReadToEnd();
        string stderr = process.StandardError.ReadToEnd();
        process.WaitForExit();
        if (process.ExitCode != 0)
            throw new InvalidOperationException($"keytool {args[0]} failed with exit code {process.ExitCode}:\n{stdout}\n{stderr}");
    }
}
