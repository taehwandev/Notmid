# API migration workflow moved

The API migration workflow, SQL files and full runbook belong to Notmid-server.
Use that repository's `docs/release/api-postgres-migration-workflow.md`.
This Android repository does not execute database migrations or deploy the API.
The server workflow retains explicit environment selection, manual confirmation
and secret injection. Existing database state is not changed by source extraction.
