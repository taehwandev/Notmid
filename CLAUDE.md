---
project: notmid
status: active
---

<!-- BEGIN MANAGED TAO AGENT OS POINTER -->
## Tao Agent OS Pointer

Read this repository's `AGENTS.md` first. It contains the active shared
Tao Agent OS routing block and repo-local priority rules. Keep this file thin:
only runtime-specific notes should live here, and shared workflow or skill
guidance must route through `AGENTS.md`.

<!-- END MANAGED TAO AGENT OS POINTER -->

# Claude Instructions

Claude should treat `AGENTS.md` as the repo-local source of truth.

Read in order:

```text
AGENTS.md
VIBEGUARD.md
${TAO_HOME:-$HOME/git/tao-agent-os}/AGENTS.md
```

Do not duplicate routing, VibeGuard, security, architecture, or verification
rules here. Update `AGENTS.md` or `VIBEGUARD.md` instead.

Before documentation, code, configuration, dependency, data, deployment, or
credential changes, follow `AGENTS.md` routing and the VibeGuard command and
safety gates documented in `VIBEGUARD.md`.
