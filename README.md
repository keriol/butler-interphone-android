# Butler Interphone 📱🎩

**The Android client for the Butler ecosystem.**

Butler Interphone is a replaceable Android frontend that talks only to the
Bifröst client boundary.

\`\`\`text
Butler Interphone
       |
    Bifröst
       |
    Midgard
       |
 Butler Core
   /      \
plugin   Asgard -> concrete Butler
\`\`\`

The Android app does not know Alfred, Home Assistant, HAP, Midgard or Asgard
implementation details.

The project is currently in private incubation and is being developed with a
future public release in mind. Public/private boundaries are enforced from the
first commit.

## Current milestone

INT-004 builds on the real Bifröst HTTP client with persistent local
connection settings while preserving the same Compose/ViewModel architecture:

\`\`\`text
Compose UI
   |
InterphoneViewModel
   |
InterphoneClient
   |
BifrostHttpClient
   |
Bifröst HTTP
\`\`\`

The current screen provides:

- separate protocol, host and port fields;
- `http` as the default protocol;
- a masked bearer-token field;
- a message field;
- a Send button;
- asynchronous request state;
- real Bifröst text request/response;
- a visible request/correlation ID;
- visible typed transport/protocol errors.

A request with no \`target_butler_name\` is intentionally Core-facing. A future
interaction that explicitly addresses a Butler may set the target without
changing the UI architecture.

## Runtime configuration

Endpoint and token are never committed.

Provide them as Gradle properties, environment variables, or entries in the
ignored Android \`local.properties\` file:

\`\`\`text
INTERPHONE_BIFROST_URL=https://example.invalid
INTERPHONE_BIFROST_TOKEN=replace-at-build-time
\`\`\`

The URL is the Bifröst host base URL. The client appends
\`/bifrost/v1/text\`.

For local proving, \`~/.gradle/gradle.properties\` is convenient because it
remains outside the repository.

## Android stack

- Android Gradle Plugin 9.4.0;
- Gradle 9.6.0;
- JDK 17;
- compile/target SDK 36;
- Kotlin 2.4.10 Compose compiler plugin with AGP built-in Kotlin;
- Compose BOM 2026.04.01;
- Activity Compose 1.11.0;
- Lifecycle 2.10.0;
- kotlinx.coroutines 1.11.0;
- kotlinx.serialization JSON 1.11.0;
- Android/JDK \`HttpURLConnection\` for HTTP transport.

## Build

Requirements:

- JDK 17;
- Android SDK platform 36;
- Android SDK Build Tools 36.0.0;
- Gradle 9.6.0, or Android Studio with compatible tooling.

From a clean checkout:

\`\`\`bash
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
\`\`\`

CI performs the same unit-test and debug-build path without private runtime
configuration.

## Kotlin map for C# developers

\`\`\`text
Kotlin data class        ~ C# record
val                      ~ read-only reference
var                      ~ mutable reference
suspend fun              ~ async-style suspendable operation
StateFlow<T>             ~ observable state stream
ViewModel                ~ UI-facing state/logic owner
Composable               ~ function that renders UI from state
\`\`\`

The Composable never performs the network request. It emits user intent to the
ViewModel; the ViewModel talks to an \`InterphoneClient\`.

## Public-ready boundary

This repository must never contain:

- production endpoints or credentials;
- household-specific entity identifiers;
- private topology or deployment details;
- personal data;
- raw biometric material or voiceprints;
- private runtime configuration.

Runtime configuration belongs outside source control.

See [Architecture](docs/architecture.md), [Contributing](CONTRIBUTING.md), and
[Security](SECURITY.md).

## License

Apache License 2.0.
