# Butler Interphone 📱🎩

**The Android client for the Butler ecosystem.**

Butler Interphone is a replaceable Android frontend for communicating with a compatible Butler runtime through the Butler communication boundaries.

```text
Butler Interphone
       |
    Bifröst
       |
     Asgard
       |
 active Butler
```

The project is currently in private incubation and is being developed with a future public release in mind. Public/private boundaries are therefore enforced from the first commit.

## Current milestone

INT-001 proves the local Android architecture before networking is introduced:

```text
Compose UI
   |
InterphoneViewModel
   |
InterphoneClient
   |
LocalEchoClient
```

The next slice replaces `LocalEchoClient` with a Bifröst transport without restructuring the UI.

The current screen provides:

- a message field;
- a Send button;
- asynchronous request state;
- a local echo response;
- a visible request/correlation ID;
- visible validation/correlation errors.

## Android stack

The bootstrap is pinned to stable tooling:

- Android Gradle Plugin 9.4.0;
- Gradle 9.6.0;
- JDK 17;
- compile/target SDK 36;
- Kotlin 2.4.10 Compose compiler plugin with AGP built-in Kotlin;
- Compose BOM 2026.09.00;
- Activity Compose 1.13.0;
- Lifecycle 2.11.0;
- kotlinx.coroutines 1.11.0.

## Build

Requirements:

- JDK 17;
- Android SDK platform 36;
- Android SDK Build Tools 36.0.0;
- Gradle 9.6.0, or Android Studio with compatible tooling.

From a clean checkout:

```bash
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
```

CI performs the same unit-test and debug-build path.

## Kotlin map for C# developers

A few useful mental translations:

```text
Kotlin data class        ~ C# record
val                      ~ read-only reference
var                      ~ mutable reference
suspend fun              ~ async-style suspendable operation
StateFlow<T>             ~ observable state stream
ViewModel                ~ UI-facing state/logic owner
Composable               ~ function that renders UI from state
```

The important architectural rule is that the Composable does not perform the request itself. It emits user intent to the ViewModel; the ViewModel talks to an `InterphoneClient`.

## Public-ready boundary

This repository must never contain:

- production endpoints or credentials;
- household-specific entity identifiers;
- private topology or deployment details;
- personal data;
- raw biometric material or voiceprints;
- private runtime configuration.

Runtime configuration belongs outside source control.

See [Architecture](docs/architecture.md), [Contributing](CONTRIBUTING.md), and [Security](SECURITY.md).

## License

Apache License 2.0.
