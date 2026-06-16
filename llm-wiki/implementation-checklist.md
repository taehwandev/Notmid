# Implementation Checklist

## Before Editing

1. Run `git status --short`.
2. Identify the smallest relevant module.
3. Read the matching wiki page:
   - product: `notmid-overview.md`
   - modules: `module-map.md`
   - UI: `design-system.md`
   - routing: `routing-deeplinks.md`
   - web/API: `platform-backend.md`
   - Firebase/security: `firebase-open-source.md`
4. Search with `rg` or `rg --files`.
5. Keep unrelated dirty files untouched.

## During Implementation

- Prefer existing module patterns.
- Keep `api` and `impl` responsibilities separate.
- Use `Notmid*` design components in feature/app code.
- Add route contracts in feature API, not app.
- Let app shell register and execute routes.
- Keep fake data deterministic.
- Do not add Firebase or heavy libraries until the code actually needs them.
- Keep TypeScript workspace changes out of Android Gradle modules unless an explicit integration is needed.

## Tests To Add Or Update

Routing:

```text
AppRouterTest
AppDeepLinkResolverTest
```

Build/module/design-system:

```bash
./gradlew :app:compileDebugKotlin
```

Domain or router behavior:

```bash
./gradlew test
```

Android data/auth boundary:

```bash
./gradlew :core:data:test
./gradlew :core:auth:impl:test
```

Formatting/patch hygiene:

```bash
git diff --check
```

Web/API after dependencies are installed:

```bash
pnpm typecheck
pnpm build:web
```

Full local verification:

```bash
bash scripts/verify-local.sh
bash scripts/smoke-web-api.sh
```

## Final Response

Mention:

- what changed
- important file paths
- verification commands run
- any blocked or intentionally deferred work

Keep it concise.
