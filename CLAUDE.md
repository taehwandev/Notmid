---
project: notmid
status: active
---

# Claude Instructions

Claude should treat `AGENTS.md` as the repo-local source of truth.

Read in order:

```text
AGENTS.md
VIBEGUARD.md
${AGENTPLAYBOOK_HOME:-$HOME/Documents/KeyFlowVault/AgentPlaybook}/AGENTS.md
```

Do not duplicate routing, VibeGuard, security, architecture, or verification
rules here. Update `AGENTS.md` or `VIBEGUARD.md` instead.

Before documentation, code, configuration, dependency, data, deployment, or
credential changes, follow `AGENTS.md` routing and the VibeGuard command and
safety gates documented in `VIBEGUARD.md`.
