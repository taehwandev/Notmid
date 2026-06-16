---
project: notmid
status: active
---

# notmid Agent Instructions

This file is the thin repo-local entrypoint for agents. Keep common operating,
security, architecture, UI, and verification rules in AgentPlaybook,
`VIBEGUARD.md`, repo skills, `llm-wiki`, or `docs/specs` instead of duplicating
them here.

Shared AgentPlaybook root:

```text
${AGENTPLAYBOOK_HOME:-$HOME/Documents/KeyFlowVault/AgentPlaybook}
```

## Priority

1. System and developer instructions from the active agent runtime.
2. The user's current request.
3. This repo-local `AGENTS.md`.
4. Repo-local skills and memory in `.agents/skills`, `llm-wiki`, and
   `docs/specs`.
5. Shared AgentPlaybook documents selected by the workflow router.

If rules conflict, use the more specific repo-local rule unless it weakens
security, data handling, or verification.

## Required Start

1. Run `git status --short`.
2. For documentation, code, configuration, dependency, data, deployment, or
   credential changes, follow `VIBEGUARD.md` before editing and again before
   finishing.
3. For multi-step work, generate a route manifest:

   ```bash
   AGENTPLAYBOOK_ROOT="${AGENTPLAYBOOK_HOME:-$HOME/Documents/KeyFlowVault/AgentPlaybook}"
   python3 "$AGENTPLAYBOOK_ROOT/scripts/workflow.py" route <command> --request "<user request>" [--platform <platform>] [--concern <concern>]
   python3 "$AGENTPLAYBOOK_ROOT/scripts/workflow.py" validate
   ```

4. When the wrappers exist, run preflight before editing:

   ```bash
   AGENTPLAYBOOK_ROOT="${AGENTPLAYBOOK_HOME:-$HOME/Documents/KeyFlowVault/AgentPlaybook}"
   python3 "$AGENTPLAYBOOK_ROOT/scripts/agent-preflight.py" --project "$(pwd)" --rules "$AGENTPLAYBOOK_ROOT" --command <command> --request "<user request>" [--platform <platform>] [--concern <concern>]
   ```

5. Read the route's documents in order, follow its gates, and stop if `missing`
   is not empty.
6. Before final reports, commits, releases, or handoffs, run
   `agent-finish-check.py` with evidence for every required route gate. Missing
   route, preflight, finish-check, or gate evidence is non-compliant.
7. In human-visible reports, use only two gate signals:
   `🐱🟢 SUCCESS` and `🐱🔴 FAIL`. Do not report a third state.

Do not load the whole shared library by default.

## Local Skill Routing

Use the matching internal skill for project-specific rules:

- Product features, Compose UI, design-system components, feature `api` /
  `impl`, routes, deep links, fake data, web/API contracts, Firebase-safe
  open-source setup: `.agents/skills/notmid-product-engineering/SKILL.md`
- Android architecture, Gradle/build-logic, module boundaries, shared UI
  placement, data/domain layers, fake services, verification strategy:
  `.agents/skills/glassnavlab-android-stewardship/SKILL.md`
- Liquid Glass navigation, AGSL, backdrop capture, gestures, screenshots,
  release polish: `.agents/skills/android-liquid-glass-compose/SKILL.md`

The skill owns its detailed rules. Add or update skill guidance there, not in
this file.

## Repo Knowledge Pointers

Use these repo-local documents only when relevant to the task:

```text
llm-wiki/implementation-checklist.md
llm-wiki/notmid-overview.md
llm-wiki/module-map.md
llm-wiki/design-system.md
llm-wiki/routing-deeplinks.md
llm-wiki/platform-backend.md
llm-wiki/firebase-open-source.md
docs/specs/notmid-router-architecture.md
docs/specs/notmid-screen-detail-plan.md
docs/specs/notmid-monorepo-platform.md
docs/specs/notmid-product-design-concept.md
docs/specs/notmid-design-system.md
docs/specs/firebase-auth-open-source-security.md
docs/todo/notmid-subagent-todo.md
```

Keep this file as routing only. Detailed product, platform, security,
verification, and workflow rules belong in the routed docs above.

<!-- vibeguard:start version=1 -->
## VibeGuard

For every task that may change code, configuration, dependencies, data,
deployment, or credentials:

1. Run `vibeguard audit .` before editing.
2. If the audit reports stale VibeGuard guardrails, run `npx --yes @taehwandev/vibeguard@latest update .` once, then rerun `vibeguard audit .`. The default refresh interval is 7 days; do not update more often unless the user asks or the audit reports stale guardrails.
3. If `vibeguard` is unavailable, run `npx --yes @taehwandev/vibeguard@latest audit .` instead and use the same `npx --yes @taehwandev/vibeguard@latest ...` form for fixes.
4. If fixable findings exist, run `vibeguard audit . --fix` before implementing.
5. Never print detected secret values. Keep real secrets only in ignored runtime env files and keep env templates such as `.env.example` and `.env.sample` value-free.
6. Ask before deleting data, running migrations, deploying to production, increasing paid API/model usage, adding recurring infrastructure, or changing credentials.
7. Prefer cost-aware architecture. Before adding a paid service, database, queue, background worker, model call, analytics SDK, or cloud resource, explain why existing code or a simpler local/server-side design is insufficient.
8. For web apps, commonize repeated API/model/provider calls behind shared server-side helpers or endpoints. Prefer server-side caching, batching, and rate limits before adding new client-side call paths.
9. Before commit or push, verify `git remote -v`, repository visibility, and changed files. If the repository is public or visibility is unknown, stop before pushing secrets, env files, credentials, deployment, infrastructure, or paid-service changes.
10. After editing, run relevant tests and `vibeguard audit .` again before finishing.
11. Before creating a commit, run `vibeguard audit .`; before pushing or publishing, run `vibeguard audit . --strict`.
12. If execution evidence is available, run `vibeguard evidence .` before the final response and do not claim tests or audits ran unless they were observed.
13. Keep secrets server-side. Do not expose provider keys, database URLs, signing secrets, service-role keys, or webhook secrets to client code.
14. If the user pastes a secret in chat, treat it as exposed. Do not repeat it, put it in commands/logs/files/GitHub secrets/deployment settings/servers, or continue with deployment using that value. Guide the user to rotate it and enter a new value only through a local provider UI or secret-store prompt.
15. Keep VibeGuard scoped to guardrails. Do not clone, vendor, install, or link external playbooks or rule libraries unless the user explicitly asks for that separate setup.
16. Preserve existing repo-local instructions. Only update the managed VibeGuard block between the `vibeguard:start` and `vibeguard:end` markers.

Refresh this managed block only when `vibeguard audit .` reports stale guardrails, or manually with `vibeguard update .` / `npx --yes @taehwandev/vibeguard@latest update .`.
<!-- vibeguard:end -->
