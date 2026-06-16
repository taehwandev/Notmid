import {
  applyPostgresMigrations,
  closePostgresMigrationSqlClient,
  createPostgresMigrationSqlClient,
  describePostgresMigrationPlan,
  loadPostgresMigrations,
} from "./postgresMigrations";

type MigrationMode = "apply" | "plan";

const mode = parseMigrationMode(process.argv.slice(2));
const migrations = loadPostgresMigrations();

if (mode === "plan") {
  const plan = describePostgresMigrationPlan(migrations);
  console.log(`notmid Postgres migration plan: ${plan.length} migration(s)`);

  for (const migration of plan) {
    console.log(`${migration.id} ${migration.checksum} ${migration.label}`);
  }

  process.exit(0);
}

const databaseUrl = process.env.DATABASE_URL?.trim();

if (!databaseUrl) {
  fail("DATABASE_URL is required to apply Postgres migrations.");
}

if (process.env.NOTMID_MIGRATION_CONFIRM !== "apply") {
  fail("Refusing to apply migrations without NOTMID_MIGRATION_CONFIRM=apply.");
}

const client = createPostgresMigrationSqlClient(databaseUrl);

try {
  const result = await applyPostgresMigrations(client, migrations);

  console.log(
    `notmid Postgres migrations complete: applied=${result.applied.length} skipped=${result.skipped.length}`,
  );

  for (const migration of result.applied) {
    console.log(`applied ${migration.id}`);
  }

  for (const migration of result.skipped) {
    console.log(`skipped ${migration.id}`);
  }
} finally {
  await closePostgresMigrationSqlClient(client);
}

function parseMigrationMode(args: string[]): MigrationMode {
  if (args.length === 0 || args[0] === "--plan") {
    return "plan";
  }

  if (args[0] === "--apply") {
    return "apply";
  }

  fail("Usage: tsx src/runPostgresMigrations.ts [--plan|--apply]");
}

function fail(message: string): never {
  console.error(message);
  process.exit(1);
}
