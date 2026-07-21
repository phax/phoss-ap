# phoss-ap-extension-demo

A minimal, **logging-only** example of a phoss-ap runtime extension. It exists to demonstrate the
supported extension mechanism end-to-end: build a thin jar of your own SPI implementations, drop it
into the Access Point's `/ext` directory, and see it discovered and invoked at runtime — **without
rebuilding the Access Point**.

## What it contains

Two SPI implementations that do nothing but log every callback they receive:

| Class | Implements | Fires on |
|-------|-----------|----------|
| `DemoLifecycleEventSPI` | `com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI` | positive events (document received / forwarded / sent, reporting success, scheduler cycles, …) |
| `DemoNotificationHandlerSPI` | `com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI` | failure events (verification rejections, permanent send/forward failures, unexpected exceptions, …) |

Each is registered the same way phoss-ap's own modules register their SPIs:

- annotated with `@com.helger.annotation.style.IsSPIImplementation`, and
- listed in a `META-INF/services/<fully-qualified-interface-name>` file under
  `src/main/resources`.

Both are discovered through the standard `java.util.ServiceLoader`, so once the jar is on the
classpath there is no further wiring.

## How the packaging works (the important bit)

Look at `pom.xml`: the `phoss-ap-api` dependency is scoped **`provided`**. The extension compiles
against the SPI interfaces but does **not** bundle them, because the running AP already provides
them. The result is a *thin* jar containing only this module's own classes and its
`META-INF/services` files — exactly what belongs in `/ext`.

Apply the same rule to your own extension: mark **everything the AP already ships**
(`phoss-ap-*`, `phase4`, `ph-commons`, `slf4j`, the AWS SDK, …) as `provided`, and bundle only the
dependencies that are genuinely unique to your extension. That keeps the classpath conflict-free.
Because the parent pom is a Maven BOM, you can inherit aligned versions for the shared,
`provided`-scoped dependencies.

## How the AP loads it

The phoss-ap Spring Boot jar is repackaged with the `ZIP` layout, which uses Spring Boot's
`PropertiesLauncher`. That launcher appends everything listed in the `LOADER_PATH` environment
variable (or `-Dloader.path`) to the runtime classpath. The Docker image sets `LOADER_PATH=/ext`
and creates that directory, so any jar placed in `/ext` is picked up on startup.

## Build it

From this module directory:

```bash
./assemble.sh --jar-only
```

or with plain Maven from the repository root:

```bash
mvn -pl phoss-ap-extension-demo -am package
```

The thin jar is produced at `target/phoss-ap-extension-demo-<version>.jar`.

## Run it — option A: mount into an existing image

Drop the jar into a host directory and bind-mount it at `/ext`:

```bash
mkdir -p ./ext
cp target/phoss-ap-extension-demo-*.jar ./ext/
docker run --rm -p 8080:8080 \
  -v "$(pwd)/ext:/ext" \
  -e PHOSSAP_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/phoss-ap \
  -e PHOSSAP_JDBC_USER=peppol \
  -e PHOSSAP_JDBC_PASSWORD=peppol \
  phelger/phoss-ap
```

## Run it — option B: bake it into a derived image

Build a small image that layers the extension on top of a phoss-ap image (see `Dockerfile`):

```bash
./assemble.sh
docker run --rm -p 8080:8080 \
  -e PHOSSAP_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/phoss-ap \
  -e PHOSSAP_JDBC_USER=peppol \
  -e PHOSSAP_JDBC_PASSWORD=peppol \
  phoss-ap-with-demo-ext
```

## Confirm it is working

During startup you should see:

```
[extension-demo] DemoLifecycleEventSPI was loaded via SPI
[extension-demo] DemoNotificationHandlerSPI was loaded via SPI
```

and then, as documents flow through the AP, lines such as:

```
[extension-demo] Lifecycle event onInboundDocumentReceived: transactionID=..., senderID=...
[extension-demo] Notification onOutboundPermanentSendingFailure: transactionID=..., ...
```

If you see those lines, your external SPI extension is being loaded and invoked correctly. Replace
the logging bodies with your real integration (message queue, custom store, webhook, …) and you have
a production extension.
