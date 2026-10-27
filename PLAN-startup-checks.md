# Plan: pluggable startup checks (port from the SilvaConnect PoC AP)

Status: proposal, not started. Target version: `0.13.3` (current snapshot). Delete this file when done.

## 1. Decision

**Port it, in a reduced form.** The part worth porting is the *mechanism* - a check interface, a runner that
collects every finding before aborting, and a warning level. The *checks* are mostly SilvaConnect specific; only a
few are candidates here (section 5).

Not a goal: replacing the initialisation code of `APServletInit` or `APCoreMetaManager`. A lot of what looks like
validation there is really "load and configure" and stays where it is.

## 2. Why it makes sense for phoss-ap

Current state (measured, `phoss-ap` at `0367e89`):

| Aspect | phoss-ap today | What the SilvaConnect mechanism adds |
|---|---|---|
| Reporting | Fail on the **first** problem: `InitializationException` at 56 sites (`APServletInit` 14, `APCoreMetaManager` 12, `DocumentPayloadManagerS3` 4, `DocumentPayloadManagerFileSystem` 6, `CleanupScheduler` 5, `DirectorySenderScheduler` 5, ...). The operator fixes one, restarts, meets the next. | One start reports **all** findings at once. |
| Warnings | None. Things that are worth a look but not fatal are a `LOGGER.warn` somewhere (e.g. receiver check mode `none`, "No Peppol certificate present"). | A warning level that is collected and logged in one place, does not abort. |
| Where checks run | Spread over `APServletInit.init`, manager constructors and `*Scheduler.start ()`. Scheduler preconditions only run **if the scheduler is enabled**, and only when `start ()` is reached, i.e. after the DB, the managers and `StartupRecovery` have already been touched. | Config-only checks run **before** any side effect. |
| Extension | The other extension points are SPIs (`IAPLifecycleEventSPI`, `IAPNotificationHandlerSPI`, `IDocumentForwarderProviderSPI`, ...), `phoss-ap-extension-demo` shows them. An extension that needs its own configuration validated has no hook and must fail in its own initialiser. | A real SPI via `ServiceLoader`. |
| Testability | The checks are interleaved with static initialisation, so they are not unit tested (the only test touching `InitializationException` is `APCoreMetaManagerIDUniquenessTest`). | A check is a small class over an `ErrorList`; positive and negative case are plain unit tests. |

The SilvaConnect reference implementation (read it before starting):

* `silvaconnect-ap-api/.../spi/IAPStartupCheckSPI.java` - `getCheckName ()`, `performCheck (ErrorList)`.
* `silvaconnect-ap-core/.../startup/SilvaConnectStartupCheckRunner.java` - runs all, a check that **throws** counts as
  a failed check (never a passed one), warnings are logged but do not abort, one aggregated exception at the end.
* `silvaconnect-ap-core/.../startup/SilvaConnectStartupCheckError.java` - builds `error`/`warning` findings with the
  check name as field name.
* `silvaconnect-ap-webapp/.../config/SilvaConnectStartupRunner.java` - the call site.

Note: in SilvaConnect the interface is annotated `@IsSPIInterface`, but the checks are **assembled in code**, not
discovered. phoss-ap needs real discovery to deliver the extension benefit, see 3.3.

## 3. Design

### 3.1 API (`phoss-ap-api`, package `...api.spi`)

```java
@IsSPIInterface
public interface IAPStartupCheckSPI
{
  @NonNull @Nonempty String getCheckName ();
  @NonNull EAPStartupCheckPhase getPhase ();           // PRE_INIT or POST_INIT, see 3.2
  void performCheck (@NonNull ErrorList aErrors);      // error level aborts, warning does not
}
```

`EAPStartupCheckPhase` (`...api.codelist`, `E` prefix, `getID`): `PRE_INIT`, `POST_INIT`.

`ErrorList` and `SingleError` come from `ph-diagnostics` (`com.helger.diagnostics.error.*`). It is already on the
`phoss-ap-api` class path (transitive, 12.5.3, checked with `dependency:tree`) and `InboundOrchestrator` uses
`ErrorList` today; declare it explicitly in the `phoss-ap-api` POM if the rules of the project demand that.

### 3.2 Two phases, because `init ()` is imperative

SilvaConnect has one phase (Spring `ApplicationReadyEvent`, after all beans). `APServletInit.init ()` is a fixed
sequence, so one hook point is not enough:

* `PRE_INIT` - **config only**, no DB, no managers, no phase4 state. Runs right after `_initGlobalSettings`,
  *before* `APBasicMetaManager.getInstance ()`. Nothing has been created yet when it fails.
* `POST_INIT` - needs the crypto factory or other initialised state. Runs after `_initPeppolAS4 ()` and
  `APCoreMetaManager.init ()`, *before* `StartupRecovery.run ()` and the schedulers start.

Each phase aggregates on its own: all `PRE_INIT` findings are reported, and if there is an error the start stops
there. It does not run `POST_INIT` on top of a broken configuration.

### 3.3 Runner and discovery (`phoss-ap-core`, package `...core.startup`)

* `APStartupCheckRunner` - port of `SilvaConnectStartupCheckRunner`: `performAllChecks (EAPStartupCheckPhase)`
  returning the `ErrorList`, and `performAllChecksOrThrow (EAPStartupCheckPhase)`.
  * a check that throws -> finding "The check itself failed: ..." (error);
  * warnings logged at WARN, errors aggregated into **one** `InitializationException` whose message lists every
    finding, so the exception type the servlet container already sees does not change;
  * log "Startup check 'x' passed" per check at INFO.
* `APStartupCheckManager` - same shape as `LifecycleEventManager`: `initSPI ()` with `ServiceLoader.load
  (IAPStartupCheckSPI.class)`, `registerCheck (...)`, `getAllChecks ()`. Built-in checks are registered first, in
  code order; discovered ones follow, sorted by `getCheckName ()` so the order is deterministic (`ServiceLoader`
  order is not part of the contract).
* `APStartupCheckError` - port of `SilvaConnectStartupCheckError` (`create`, `createWarning`).
* Register the built-in checks in one place (e.g. `APCoreMetaManager` or a `APBuiltInStartupChecks` factory).

### 3.4 Call sites in `APServletInit.init ()`

```
_initGlobalSettings (aSC);
APStartupCheckManager.initSPI ();
APStartupCheckRunner.performAllChecksOrThrow (PRE_INIT);     // new
APBasicMetaManager.getInstance (); ... SMPClientManager.init (); _initAS4 (); _initPeppolAS4 (); APCoreMetaManager.init ();
APStartupCheckRunner.performAllChecksOrThrow (POST_INIT);    // new
StartupRecovery.run (); schedulers start
```

## 4. Migration inventory (existing validations -> checks)

Move only **pure validation**. Side effects stay in the init code. Line numbers are of `0367e89`.

| Existing code | New check | Phase | Notes |
|---|---|---|---|
| `APServletInit._checkOutboundTlsTrustStore` (l. 187) | `outbound-tls-truststore` | PRE_INIT | Already a "fail before the DB" pre-flight (see News, 0.13.x), the natural first check. |
| `APServletInit` stage missing/invalid (l. 314-317) | `peppol-stage` | PRE_INIT | |
| `APServletInit` seat ID syntax (l. 348-354) | `peppol-seat-id` | PRE_INIT | |
| `APServletInit` owner country code (l. 357-363) | `peppol-owner-country` | PRE_INIT | |
| `APServletInit` receiver check mode requirements (l. 370-420) | `peppol-receiver-check` | PRE_INIT | Only the "`smp` needs `peppol.smp.url` and `phase4.endpoint.address`" part. The `setReceiverCheckEnabled`/`setSMPClient` calls stay in `_initPeppolAS4`. `none` becomes a **warning** instead of a log line. |
| `APServletInit` key store / private key load (l. 295-310) | `as4-keystore` | POST_INIT | Needs `AS4CryptoFactoryConfiguration`. Check whether it can run earlier (it needs `_initAS4`'s `AS4Configuration.setConfig` first - then POST_INIT is right). |
| `APServletInit` Peppol AP certificate check (l. 320-340) | `peppol-ap-certificate` | POST_INIT | **Keep** the escape hatch `phossap.internal.skip-peppol-certificate-check` (tests rely on it) and keep `Phase4PeppolDefaultReceiverConfiguration.setAPCAChecker` in the init code. |
| `RetryScheduler.start` (l. 360), `ArchivalScheduler.start` (l. 168), `CleanupScheduler.start` (l. 181-209) | `scheduler-configuration` (one class, or one per scheduler) | PRE_INIT | Batch size >= 1, interval >= 1 ms, "cleanup requires archival", retention >= minimum and > 2x archival interval. Evaluated **only when the scheduler is enabled**, as today. The runtime guards in `start ()` may stay as a cheap second line or go - decide in review. |
| `DirectorySenderScheduler` (l. 80-100) | `dirsender-configuration` | PRE_INIT | Lives in `phoss-ap-dirsender`; register it from there through `META-INF/services` - the first real SPI user, and a good proof of the extension path. |

**Deliberately not migrated** (interleaved with object construction, little to gain, high regression risk):
`APCoreMetaManager` forwarder / verifier / provider-ID validation (l. 97-321), `DocumentForwarderFactory`,
`APBasicMetaManager` storage-provider selection, `DocumentPayloadManagerFileSystem.verifyConfiguration ()` and the S3
manager (they create directories / probe the bucket - side effects), `APJdbcMetaManager`. Revisit once the
mechanism has proven itself.

## 5. New checks to consider (from SilvaConnect) - each needs verification first

Do not copy these blindly; SilvaConnect's rules come from its own specifications. For each: confirm the rule exists
in the Peppol specification, then implement as **warning first**, promote to error in a later release if no false
positives are reported.

| SilvaConnect check | Idea for phoss-ap | Verify |
|---|---|---|
| `StartupCheckOwnParticipantID` | The configured Peppol seat ID equals the subject CN of the AP certificate. Today only the *syntax* of the seat ID is checked. A mismatch would make every message be rejected by the counterpart while the AP looks fine from the inside. | Whether the Peppol AS4 profile really requires `eb:From/PartyId` == certificate CN. |
| `StartupCheckEndpointURL` | `phase4.endpoint.address` is set, is a well-formed `https` URL (a `http` URL as warning), and is the one receiver check mode `smp`/`sml` will compare against. | Whether non-https must be an error for Peppol production. |
| `StartupCheckApiToken` | `phase4.api.requiredtoken` is set; a missing or short token is an **error in production stage, warning otherwise**. | Whether an unset token leaves `/api/**` open in phoss-ap (check `phoss-ap-webapp`; I did not verify). |
| `StartupCheckTLSSeparation` | The AP (network) certificate is not also the TLS server certificate. | Only if the Peppol profile forbids it; otherwise drop. Needs access to the TLS certificate, which an AP behind a reverse proxy does not have - SilvaConnect then reports "cannot be checked". |

Not applicable to phoss-ap: `StartupCheckAS4Profile` (the profile is enforced programmatically via
`AS4ProfileSelector.setCustomDefaultAS4ProfileID`), `StartupCheckDuplicateRetention` (phoss-ap has its own
JDBC-backed duplicate manager), `StartupCheckTrustStore` / `StartupCheckNetworkCertificate` (SilvaConnect PKI
specifics; the Peppol equivalents are the certificate check already in section 4), `StartupCheckCodeListVersion`,
`StartupCheckForwarder` (covered by `APCoreMetaManager`).

## 6. Steps

1. **Infrastructure** (no behaviour change yet): SPI, phase enum, error helper, runner, manager. Unit tests for the
   runner - see 7. Add to `phoss-ap-api` / `phoss-ap-core` only.
2. **Wire the hook points** in `APServletInit.init ()` with an empty built-in list. Start still behaves exactly as before.
3. **Migrate `outbound-tls-truststore`** first (smallest, already a pre-flight). Remove it from `APServletInit`.
4. **Migrate the Peppol configuration checks** (stage, seat ID, country, receiver check mode).
5. **Migrate the key store and certificate checks** (POST_INIT), keeping the skip property.
6. **Scheduler checks** incl. `phoss-ap-dirsender` through `META-INF/services`.
7. **Demo**: add a check to `phoss-ap-extension-demo` (+ `META-INF/services/com.helger.phoss.ap.api.spi.IAPStartupCheckSPI`).
8. **New checks** from section 5 that survived verification, as warnings.
9. **Docs** - section 8.

Steps 1-2 are one commit's worth; every later step is independently releasable, so the work can stop anywhere.

## 7. Tests

* Runner: no checks -> empty list; several failing checks -> **all** findings reported, one exception;
  warning only -> no exception; a check that throws -> error finding naming the check; phases are independent;
  order = built-ins in registration order, SPI ones by name.
* Each migrated check: one positive and one negative case, measured against the *same input the old code rejected*
  (copy the failing configuration from the old `InitializationException` branch, do not paraphrase it).
* Equivalence of messages is **not** required, but every old `InitializationException` branch must have a
  corresponding failing test, otherwise the migration silently drops a validation.
* `APServletInit` itself is not unit tested today. Keep the migration steps small enough that a manual start
  (`docker-compose`, see `Running-phoss-AP`) with a deliberately broken property per check is a realistic
  acceptance test; list the properties used in the PR.

## 8. Documentation (project rules)

* `News-and-noteworthy.md` in `../phoss-ap.wiki`: add to the `v0.13.3 - work in progress` entry, in the existing
  bullet style (British English).
* `Runtime-Extensions.md`: document `IAPStartupCheckSPI` next to the other SPIs, including the two phases and the
  `META-INF/services` registration.
* `Running-phoss-AP.md` / `Configuration-Properties.md`: a short "what is checked at startup" table; note that all
  findings are now reported together.
* If new checks add configuration, update `APConfigurationProperties` and `Configuration-Properties.md`; update
  `_Sidebar.md` / `Home.md` only if a new page is created.

## 9. Risks and open questions

* **Behavioural change for existing deployments:** a configuration that only produced a log line before can now be
  an error. Mitigation: new checks start as warnings (section 5); migrated checks keep the old severity.
* **Ordering inside `init ()`** is load-bearing (comments say so: JDBC before the AS4 duplicate manager, SMP client
  before the receiver check). The new hook points must not move anything; step 2 is the proof.
* **Static state:** the managers are static singletons. `APStartupCheckManager.initSPI ()` must follow the
  "may only be called once" rule of `LifecycleEventManager`, and the tests must be able to reset it
  (`@VisibleForTesting`).
* **Decide:** should an operator be able to turn a single check off? SilvaConnect says no (a check that can be
  switched off gets switched off). Recommendation: no, except the existing `phossap.internal.skip-peppol-certificate-check`.
* **Decide:** keep or remove the duplicated guards in `Retry-/Archival-/CleanupScheduler.start ()` after migration.
* **Not verified here:** everything marked "verify" in section 5.
