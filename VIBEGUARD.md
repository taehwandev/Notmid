---
project: notmid
status: active
---

# VIBEGUARD.md

This project uses VibeGuard before AI-generated documentation, code,
configuration, dependency, data, deployment, or credential changes.

Latest safety model source:

```text
https://vibeguard.thdev.app/
```

## Command

Use the local Tao Agent OS rule source:

```bash
TAO_ROOT="${TAO_HOME:-$HOME/git/tao-agent-os}"
npm exec --cache /private/tmp/notmid-npm-cache --no-update-notifier --yes --package @taehwandev/vibeguard -- vibeguard audit . --rules "$TAO_ROOT"
```

Run the command before editing and again before finishing.

When the user explicitly approves refreshing an existing managed VibeGuard
block, use the same rule source:

```bash
TAO_ROOT="${TAO_HOME:-$HOME/git/tao-agent-os}"
npm exec --cache /private/tmp/notmid-npm-cache --no-update-notifier --yes --package @taehwandev/vibeguard -- vibeguard update . --rules "$TAO_ROOT"
npm exec --cache /private/tmp/notmid-npm-cache --no-update-notifier --yes --package @taehwandev/vibeguard -- vibeguard audit . --fix --rules "$TAO_ROOT"
npm exec --cache /private/tmp/notmid-npm-cache --no-update-notifier --yes --package @taehwandev/vibeguard -- vibeguard audit . --rules "$TAO_ROOT"
```

## Safety Gates

| Gate | Pause Or Block When |
| --- | --- |
| Secret Quarantine | A secret, private key, token, service account, or credential-like value may be hard-coded or exposed. |
| Env Protection | Local env/config files are tracked, examples contain real values, or ignored local config is missing. |
| Data Loss Pause | A change may delete, reset, migrate, overwrite, or mutate important local, staging, or production data. |
| Cost-Aware Architecture | A change may add paid APIs, recurring infrastructure, unbounded usage, missing quotas, or weak test/prod separation. |
| Git Gates | Repository state, changed-file risk, hooks, generated files, or push/commit readiness is unclear. |
| Server/Client Boundary | Server-only secrets, privileged credentials, admin APIs, or private integration details could reach client-visible code. |

The local audit may also report structure warnings for oversized files. Treat
those as a requirement for a small-step plan or a split before adding broad new
behavior.

## Operating Rules

- Do not print detected secret values.
- Keep real values in ignored local files such as `local.properties` or `.env`.
- Keep `.env.example` and `local.properties.example` value-free.
- Ask before destructive data work, production deploys, credential changes, or
  increased paid-service usage.
- Show changed files and verification evidence before finishing.

## Local Shared Rules

```text
${TAO_HOME:-$HOME/git/tao-agent-os}
```
