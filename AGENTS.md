---
project: notmid
status: active
---

<!-- BEGIN MANAGED TAO AGENT OS ROUTING -->
## Tao Agent OS Active Routing

This managed block is the active shared Tao Agent OS workflow link for this
repository. Keep repo-local instructions in this file as the source of truth for
project paths, commands, domain rules, and product policy. If another
Tao Agent OS section appears elsewhere, this managed block wins for shared
workflow routing while repo-specific rules still win for local facts.

Use the existing shared Tao Agent OS root. In committed repo-local files, keep
the reference portable: set `TAO_HOME` in the local shell/runtime, or
use a repo-pinned `.agents/tao-agent-os` only when this repo intentionally owns
one. Do not clone, vendor, download, or commit a second Tao Agent OS root unless
the user explicitly approves after seeing the existing root path.

Shared entrypoints:

```text
${TAO_HOME}/AGENTS.md
${TAO_HOME}/index.md
${TAO_HOME}/scripts/agent-entry.py
${TAO_HOME}/scripts/project-discover.py
${TAO_HOME}/scripts/agent-hook.py
${TAO_HOME}/scripts/workflow.py
${TAO_HOME}/scripts/agent-preflight.py
${TAO_HOME}/scripts/agent-finish-check.py
```

Before project work, read repo-local guidance first, then use Tao Agent OS only
to select the smallest relevant shared cards. Keep shared workflow and skill
guidance in Tao Agent OS; do not create repo-local skill documents merely to
mirror shared behavior. Keep repo-local skills, workflows, wiki pages, or
runbooks only when they contain product-specific facts, commands, domain policy,
or verification that cannot be shared safely.

For every multi-step task, run the start hook before selecting shared docs,
editing, reviewing, committing, or reporting completion. When executing wrapper
commands from an agent runtime, resolve `TAO_HOME` to an absolute path
first and use that absolute script path in the command. Do not leave `$HOME`,
`${HOME}`, `~`, `$(pwd)`-based script paths, or relative Tao Agent OS paths in
commands that may be persisted as permission rules.

```bash
python3 /absolute/path/to/tao-agent-os/scripts/agent-hook.py start --project "$(pwd)" --rules /absolute/path/to/tao-agent-os --command <command> --request "<USER_REQUEST>"
```

Use the returned route manifest as the task checklist. If the route includes
`route docs read`, run the docs-read hook before triage, ambiguity handling,
implementation, review, or edits. Run the review hook after the scoped diff is
ready, and run the finish hook before final report, commit, release, or handoff.
Pass evidence for every required route gate. Missing route, preflight, docs-read,
review, finish, or gate evidence is non-compliant even when the final files look
correct.

Request intake is mandatory for requirement analysis and modifications, even
when the task does not create a PRD. `--request-classified` must include
`--classification-evidence`. Before editing, present a short alignment
checkpoint to the user when assumptions affect behavior, scope, safety, cost,
data, or external state: what is clear, what may differ, what is unknown, and the
exact question or assumption that unblocks work.

For feature, product, build, bugfix, refactor, simplification, workflow setup,
release, shipping, or general task implementation, search and open PRD, spec,
ARD, issue, design note, task doc, or source-of-truth docs before code or edits.
If none exists, record that result and whether the current user request is enough
for the slice.

Before implementation, record the `documentation impact` decision: affected doc
path or doc class, intended decision (`updated`, `created`, `unchanged`, or `not
applicable`), and why behavior, workflow policy, public contract, operator
action, or acceptance criteria do or do not require a documentation update. The
later `documentation` gate must prove the actual update or unchanged/not
applicable decision.

If the route, repo workflow, or user asks for Grill-Me, use the actual Grill-Me
protocol, skill, or `/grilling` session as the question drill. Do not replace it
with ad hoc internal questions. Record the Grill-Me output in finish evidence
when required.

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
the selected Tao Agent OS root as the rule source. Do not run VibeGuard `setup`
or `update` blindly; preserve existing guardrails unless the user explicitly
chooses a refresh/setup mode. Human-visible gate status must use only
`🐱🟢 SUCCESS` or `🐱🔴 FAIL`.
<!-- END MANAGED TAO AGENT OS ROUTING -->
# notmid Agent Instructions

This file is the thin repo-local entrypoint for agents. Keep common operating,
security, architecture, UI, and verification rules in Tao Agent OS,
`VIBEGUARD.md`, `llm-wiki`, or `docs/specs` instead of duplicating them here.

Shared Tao Agent OS root:

```text
${TAO_HOME:-$HOME/git/tao-agent-os}
```

## Priority

1. System and developer instructions from the active agent runtime.
2. The user's current request.
3. This repo-local `AGENTS.md`.
4. Repo-local memory in `llm-wiki` and `docs/specs`.
5. Shared Tao Agent OS documents selected by the workflow router.

If rules conflict, use the more specific repo-local rule unless it weakens
security, data handling, or verification.

## Required Start

1. Run `git status --short`.
2. For multi-step work, follow the active Tao Agent OS routing block above:
   run `agent-hook.py start`, read the routed docs, then run the review and
   finish hooks with gate evidence.
3. For documentation, code, configuration, dependency, data, deployment, or
   credential changes, follow `VIBEGUARD.md` before editing and again before
   finishing.
4. Use the route manifest to select the smallest relevant shared Tao Agent OS
   cards. Keep Notmid-specific facts in `llm-wiki` or `docs/specs`.
5. Do not load the whole shared library by default.

## Shared Skill Guidance

Notmid does not keep repository-local skill documents. Shared agent skill and
architecture guidance lives in Tao Agent OS. Use the route manifest to select
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
