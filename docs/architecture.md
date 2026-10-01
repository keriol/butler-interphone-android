# Architecture

Butler Interphone is a client, not a Butler runtime.

\`\`\`text
Compose UI
    |
ViewModel
    |
InterphoneClient
    |
 Bifröst
    |
 Midgard
    |
Butler Core
   /      \
plugin   Asgard -> concrete Butler
\`\`\`

## Ownership

- Compose owns presentation.
- The ViewModel owns interaction state and orchestration for the screen.
- \`InterphoneClient\` is the replaceable client boundary.
- Bifröst owns client transport/correlation.
- Midgard carries the request inward.
- Core-facing capabilities do not require a concrete Butler target.
- Asgard is used only when an explicitly addressed concrete Butler runtime is required.
- Provider plugins own provider integration behavior.

The Android app must not acquire direct knowledge of Alfred domains, Home
Assistant entity identifiers, HAP internals, Midgard routing or Asgard.

## Transport seam

INT-001 proved the UI using \`LocalEchoClient\`.

INT-002 swaps in \`BifrostHttpClient\` without restructuring the UI. A request
with no \`target_butler_name\` remains Core-facing. Explicit Butler addressing
can be added per request later through the same client contract.
