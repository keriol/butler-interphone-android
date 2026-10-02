# Butler Interphone 📱🎩

**The Android client for the Butler ecosystem.**

Butler Interphone is the public Android frontend for talking to a Butler through
the reusable communication stack.

```text
Android
  |
Butler Interphone
  |
Bifröst
  |
Midgard
  |
Butler Core
  |
shared capabilities / concrete Butler
```

Interphone does not embed concrete Butler behavior. It discovers compatible
runtime metadata, transports user requests through Bifröst and renders the
result returned by the network.

## Current release

**Public Alpha: 0.1.0 — Ignition**

Interphone 0.1.0 is the Android endpoint of **IGNITION-001**, the first
coordinated Butler-to-Android network baseline.

The release proves:

- signed Android STABLE builds;
- runtime Butler discovery and selection;
- Bifröst node-manifest compatibility checks;
- explicit Butler targeting;
- request correlation and source-Butler diagnostics;
- real Core/HAP READ -> ACTION -> READ/VERIFY;
- real concrete-Butler request/reply through Bifröst, Midgard and a
  Butler-owned Asgard boundary.

Voice, proactive notifications, structured confirmation controls and richer
interaction UX remain post-0.1.0 work.

## Product surface

The current app provides:

- **Talk** — text request/response with the selected Butler;
- **Butler** — runtime identity and hierarchical manifest explorer;
- **Config** — Bifröst endpoint and credential setup;
- **Version** — build and release information.

Connection details stay secondary to the Butler interaction itself.

## Runtime configuration

Production endpoints and tokens are never committed.

Provide them at build time or configure them locally using supported runtime
settings. Development builds may use Gradle properties, environment variables,
or the ignored Android `local.properties` file.

Example:

```text
INTERPHONE_BIFROST_URL=https://example.invalid
INTERPHONE_BIFROST_TOKEN=replace-at-build-time
```

The client uses the Bifröst HTTP API and never requires private Butler
implementation details.

## Release signing

Debug CI remains intentionally secret-free.

Installable STABLE APKs use a persistent Android signing identity provided only
through GitHub Actions secrets:

```text
INTERPHONE_KEYSTORE_B64
INTERPHONE_KEYSTORE_PASSWORD
INTERPHONE_KEY_ALIAS
INTERPHONE_KEY_PASSWORD
```

The keystore is materialized only on the ephemeral runner and is never stored in
the repository.

## Android stack

- Android Gradle Plugin 9.4.0;
- Gradle 9.6.0;
- JDK 17;
- compile/target SDK 36;
- Kotlin + Jetpack Compose;
- Material 3;
- kotlinx.coroutines;
- kotlinx.serialization.

## Build

Requirements:

- JDK 17;
- Android SDK platform 36;
- Android SDK Build Tools 36.0.0;
- Gradle 9.6.0, or a compatible Android Studio installation.

```bash
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
```

CI exercises the same public, secret-free test/build path.

## Architecture boundary

Interphone owns Android presentation and client-side interaction state.

It does **not** own:

- Butler routing policy;
- concrete Butler runtime behavior;
- Home Assistant semantics;
- household configuration;
- Bifröst/Midgard server implementation.

See [Architecture](docs/architecture.md).

## Public boundary

This repository must never contain:

- production endpoints or credentials;
- private household identifiers;
- deployment-specific topology;
- personal data;
- raw biometric material or voiceprints;
- private Butler implementation.

Runtime configuration belongs outside source control.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

Active work and release evidence are tracked in GitHub Issues.

## Security

See [SECURITY.md](SECURITY.md).

## License

Apache License 2.0.
