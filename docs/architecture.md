# Architecture

Butler Interphone is a client, not a Butler runtime.

```text
Compose UI
    |
ViewModel
    |
InterphoneClient
    |
 Bifröst
    |
  Asgard
    |
active Butler
```

## Ownership

- Compose owns presentation.
- The ViewModel owns interaction state and orchestration for the screen.
- `InterphoneClient` is the replaceable client boundary.
- Bifröst owns client/runtime transport and correlation.
- Asgard is the runtime-side Butler boundary.
- A concrete Butler runtime owns domains, capabilities, policy and execution.

The Android app must not acquire direct knowledge of Alfred domains or household entity identifiers.

## Bootstrap seam

INT-001 starts with a `LocalEchoClient` so UI and state management can be proven without networking.

The next transport slice replaces that implementation with a Bifröst client without restructuring the UI.
