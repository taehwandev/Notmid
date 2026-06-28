---
project: notmid
status: active
---

<!-- BEGIN MANAGED AGENTPLAYBOOK ROUTING -->
## AgentPlaybook Active Routing

This managed block is the active shared AgentPlaybook workflow link for this
repository. Keep repo-local instructions in this file as the source of truth for
project paths, commands, domain rules, and product policy. If an older
AgentPlaybook section appears elsewhere in this file, this managed block wins
for shared workflow routing while repo-specific rules still win for local facts.

Resolve the shared root before running shared scripts:

```bash
AGENTPLAYBOOK_ROOT="${AGENTPLAYBOOK_HOME:-$HOME/Documents/KeyFlowVault/AgentPlaybook}"
```

Shared entrypoints:

```text
${AGENTPLAYBOOK_ROOT}/AGENTS.md
${AGENTPLAYBOOK_ROOT}/index.md
${AGENTPLAYBOOK_ROOT}/scripts/agent-entry.py
${AGENTPLAYBOOK_ROOT}/scripts/project-discover.py
${AGENTPLAYBOOK_ROOT}/scripts/agent-hook.py
${AGENTPLAYBOOK_ROOT}/scripts/workflow.py
${AGENTPLAYBOOK_ROOT}/scripts/agent-preflight.py
${AGENTPLAYBOOK_ROOT}/scripts/agent-finish-check.py
```

Before project work, read repo-local guidance first, then use AgentPlaybook only
to select the smallest relevant shared cards. Keep shared workflow and skill
guidance in AgentPlaybook; do not create repo-local skill documents merely to
mirror shared behavior. Keep repo-local skills, workflows, wiki pages, or
runbooks only when they contain product-specific facts, commands, domain
policy, or verification that cannot be shared safely. Do not copy, vendor, or download a
second AgentPlaybook root unless the user explicitly approves after seeing the
existing root path. Do not commit personal absolute AgentPlaybook paths; use
`${AGENTPLAYBOOK_HOME}` for shared local installs or a repo-pinned root only
when the repo intentionally owns one.

For every multi-step task, run the start hook before selecting shared docs,
editing, reviewing, committing, or reporting completion:

```bash
python3 "${AGENTPLAYBOOK_ROOT}/scripts/agent-hook.py" start --project "$(pwd)" --rules "${AGENTPLAYBOOK_ROOT}" --command <command> --request "<USER_REQUEST>"
```

Use the returned route manifest as the task checklist. Run the review hook after
the scoped diff is ready, and run the finish hook before final report, commit,
release, or handoff. Pass evidence for every required route gate. Missing route,
preflight, review, finish, or gate evidence is non-compliant even when the final
files look correct.

Request intake is mandatory for requirement analysis and modifications, even
when the task does not create a PRD. Before editing, present a short alignment
checkpoint to the user when assumptions affect behavior, scope, safety, cost,
data, or external state: what is clear, what is uncertain or different between
user intent and agent interpretation, whether PRD/ARD is being created or
skipped, and the exact question or assumption that unblocks work. Skipping a PRD
is not permission to skip this checkpoint.

If the route, repo workflow, or user asks for Grill-Me, use the actual Grill-Me
skill/service/session as the question drill. Do not replace Grill-Me with ad hoc
internal questions. Record the Grill-Me or alignment evidence in the finish
check when the route requires it.

For code work, decide whether to use subagents only after the target project,
owned files, boundaries, forbidden files, and verification commands are clear.
Use subagents for separable research, review, or implementation streams; keep
small single-boundary changes in the main agent. Record the split decision in
the route gates when requested.

If a required gate or hook fails, do not finalize. Return to the first missed
gate only and retry that same scope once. If it fails again, run the shared
retrospective-learning workflow and record the durable lesson before handoff or
another attempt.

VibeGuard is required before documentation, code, configuration, dependency,
data, deployment, or credential changes and again before finishing. Run it with
the selected AgentPlaybook root as the rule source. Do not run VibeGuard `setup`
or `update` blindly; preserve existing guardrails unless the user explicitly
chooses a refresh/setup mode. Human-visible gate status must use only
`🐱🟢 SUCCESS` or `🐱🔴 FAIL`.
<!-- END MANAGED AGENTPLAYBOOK ROUTING -->

# notmid Agent Instructions

This file is the thin repo-local entrypoint for agents. Keep common operating,
security, architecture, UI, and verification rules in AgentPlaybook,
`VIBEGUARD.md`, `llm-wiki`, or `docs/specs` instead of duplicating them here.

Shared AgentPlaybook root:

```text
${AGENTPLAYBOOK_HOME:-$HOME/Documents/KeyFlowVault/AgentPlaybook}
```

## Priority

1. System and developer instructions from the active agent runtime.
2. The user's current request.
3. This repo-local `AGENTS.md`.
4. Repo-local memory in `llm-wiki` and `docs/specs`.
5. Shared AgentPlaybook documents selected by the workflow router.

If rules conflict, use the more specific repo-local rule unless it weakens
security, data handling, or verification.

## Required Start

1. Run `git status --short`.
2. For multi-step work, follow the active AgentPlaybook routing block above:
   run `agent-hook.py start`, read the routed docs, then run the review and
   finish hooks with gate evidence.
3. For documentation, code, configuration, dependency, data, deployment, or
   credential changes, follow `VIBEGUARD.md` before editing and again before
   finishing.
4. Use the route manifest to select the smallest relevant shared AgentPlaybook
   cards. Keep Notmid-specific facts in `llm-wiki` or `docs/specs`.
5. Do not load the whole shared library by default.

## Shared Skill Guidance

Notmid does not keep repository-local skill documents. Shared agent skill and
architecture guidance lives in AgentPlaybook. Use the route manifest to select
the smallest relevant shared cards, especially:

```text
common/agent-operating-skill.md
common/agent-skill-card-anatomy.md
platforms/android/android-architecture.md
platforms/android/android-module-structure.md
platforms/android/android-viewmodel-state.md
platforms/android/android-state-data.md
platforms/android/android-compose-ui.md
platforms/android/android-security.md
platforms/android/android-review.md
platforms/android/android-external-skill-source-coverage.md
```

Keep Notmid-specific product facts, module inventory, route behavior, backend
facts, Firebase/open-source policy, and implementation plans in `llm-wiki` or
`docs/specs`. Do not recreate `.agents/skills` in this repository unless the
user explicitly asks to reintroduce repo-local skill discovery.

## Repo Knowledge Pointers

Use these repo-local documents only when relevant to the task:

```text
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
