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

## Status

Early bootstrap. The first milestone is a deliberately small Kotlin + Jetpack Compose client with local request/response state before the real Bifröst transport is connected.

## Public-ready boundary

This repository must never contain:

- production endpoints or credentials;
- household-specific entity identifiers;
- private topology or deployment details;
- personal data;
- raw biometric material or voiceprints;
- private runtime configuration.

Runtime configuration belongs outside source control.

## License

Apache License 2.0.
