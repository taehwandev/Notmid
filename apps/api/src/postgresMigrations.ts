import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import postgres from "postgres";
import {
  postgresClientOptionsForOptions,
  type PostgresSqlClientLike,
} from "./postgresQueryClient";

type PostgresMigrationCatalogEntry = {
  id: string;
  label: string;
  relativePath: string;
};

export type PostgresMigrationDefinition = {
  checksum: string;
  id: string;
  label: string;
  path: string;
  sql: string;
};

export type PostgresMigrationApplyResult = {
  applied: PostgresMigrationDefinition[];
  skipped: PostgresMigrationDefinition[];
};

type AppliedPostgresMigrationRow = {
  applied_at: string;
  checksum: string;
  id: string;
};

const apiRootDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");

const postgresMigrationCatalog: PostgresMigrationCatalogEntry[] = [
  {
    id: "0001_initial_schema",
    label: "Initial notmid Postgres schema",
    relativePath: "db/postgres/schema.sql",
  },
  {
    id: "0002_chat_thread_access",
    label: "Chat thread access and invite state",
    relativePath: "db/postgres/0002_chat_thread_access.sql",
  },
  {
    id: "0003_user_relationships",
    label: "User relationship state for chat policy",
    relativePath: "db/postgres/0003_user_relationships.sql",
  },
];

const destructiveMigrationPatterns = [
  /\bDROP\b/i,
  /\bTRUNCATE\b/i,
  /\bDELETE\s+FROM\b/i,
  /\bALTER\s+TABLE\b[\s\S]*\bDROP\b/i,
];

export function loadPostgresMigrations(rootDir = apiRootDir): PostgresMigrationDefinition[] {
  return postgresMigrationCatalog.map((entry) => {
    const path = resolve(rootDir, entry.relativePath);
    const sql = readFileSync(path, "utf8");

    return {
      checksum: checksumForPostgresMigration(sql),
      id: entry.id,
      label: entry.label,
      path,
      sql,
    };
  });
}

export function validatePostgresMigrations(migrations: PostgresMigrationDefinition[]): void {
  const ids = new Set<string>();

  for (const migration of migrations) {
    if (!/^\d{4}_[a-z0-9_]+$/.test(migration.id)) {
      throw new Error(`Invalid Postgres migration id: ${migration.id}`);
    }

    if (ids.has(migration.id)) {
      throw new Error(`Duplicate Postgres migration id: ${migration.id}`);
    }

    ids.add(migration.id);

    if (migration.sql.trim().length === 0) {
      throw new Error(`Postgres migration is empty: ${migration.id}`);
    }

    for (const pattern of destructiveMigrationPatterns) {
      if (pattern.test(migration.sql)) {
        throw new Error(`Postgres migration ${migration.id} contains destructive SQL.`);
      }
    }
  }
}

export function describePostgresMigrationPlan(
  migrations = loadPostgresMigrations(),
): Pick<PostgresMigrationDefinition, "checksum" | "id" | "label" | "path">[] {
  validatePostgresMigrations(migrations);

  return migrations.map(({ checksum, id, label, path }) => ({
    checksum,
    id,
    label,
    path,
  }));
}

export async function applyPostgresMigrations(
  client: PostgresSqlClientLike,
  migrations = loadPostgresMigrations(),
): Promise<PostgresMigrationApplyResult> {
  validatePostgresMigrations(migrations);
  await ensureMigrationLedger(client);

  const appliedRows = await readAppliedMigrations(client);
  const appliedById = new Map(appliedRows.map((row) => [row.id, row]));
  const applied: PostgresMigrationDefinition[] = [];
  const skipped: PostgresMigrationDefinition[] = [];

  for (const migration of migrations) {
    const existing = appliedById.get(migration.id);

    if (existing) {
      if (existing.checksum !== migration.checksum) {
        throw new Error(`Postgres migration checksum changed after apply: ${migration.id}`);
      }

      skipped.push(migration);
      continue;
    }

    await applySinglePostgresMigration(client, migration);
    applied.push(migration);
  }

  return {
    applied,
    skipped,
  };
}

export function createPostgresMigrationSqlClient(databaseUrl: string): PostgresSqlClientLike {
  return postgres(
    databaseUrl,
    postgresClientOptionsForOptions({
      databaseUrl,
      maxConnections: 1,
    }),
  ) as PostgresSqlClientLike;
}

export async function closePostgresMigrationSqlClient(
  client: PostgresSqlClientLike,
): Promise<void> {
  await client.end({ timeout: 5 });
}

export function splitPostgresStatements(sql: string): string[] {
  const statements: string[] = [];
  let buffer = "";
  let inSingleQuote = false;
  let inDoubleQuote = false;
  let inLineComment = false;
  let inBlockComment = false;
  let dollarQuoteTag: string | null = null;

  for (let index = 0; index < sql.length; index += 1) {
    const char = sql[index];
    const next = sql[index + 1];

    if (inLineComment) {
      buffer += char;
      if (char === "\n") {
        inLineComment = false;
      }
      continue;
    }

    if (inBlockComment) {
      buffer += char;
      if (char === "*" && next === "/") {
        buffer += next;
        index += 1;
        inBlockComment = false;
      }
      continue;
    }

    if (dollarQuoteTag) {
      if (sql.startsWith(dollarQuoteTag, index)) {
        buffer += dollarQuoteTag;
        index += dollarQuoteTag.length - 1;
        dollarQuoteTag = null;
      } else {
        buffer += char;
      }
      continue;
    }

    if (inSingleQuote) {
      buffer += char;
      if (char === "'" && next === "'") {
        buffer += next;
        index += 1;
      } else if (char === "'") {
        inSingleQuote = false;
      }
      continue;
    }

    if (inDoubleQuote) {
      buffer += char;
      if (char === '"' && next === '"') {
        buffer += next;
        index += 1;
      } else if (char === '"') {
        inDoubleQuote = false;
      }
      continue;
    }

    if (char === "-" && next === "-") {
      buffer += char;
      buffer += next;
      index += 1;
      inLineComment = true;
      continue;
    }

    if (char === "/" && next === "*") {
      buffer += char;
      buffer += next;
      index += 1;
      inBlockComment = true;
      continue;
    }

    if (char === "'") {
      buffer += char;
      inSingleQuote = true;
      continue;
    }

    if (char === '"') {
      buffer += char;
      inDoubleQuote = true;
      continue;
    }

    if (char === "$") {
      const tag = /^\$[A-Za-z_][A-Za-z0-9_]*\$|^\$\$/.exec(sql.slice(index))?.[0];

      if (tag) {
        buffer += tag;
        index += tag.length - 1;
        dollarQuoteTag = tag;
        continue;
      }
    }

    if (char === ";") {
      const statement = buffer.trim();

      if (statement.length > 0) {
        statements.push(statement);
      }

      buffer = "";
      continue;
    }

    buffer += char;
  }

  const finalStatement = buffer.trim();

  if (finalStatement.length > 0) {
    statements.push(finalStatement);
  }

  return statements;
}

async function ensureMigrationLedger(client: PostgresSqlClientLike): Promise<void> {
  await client.unsafe(`
    CREATE TABLE IF NOT EXISTS notmid_schema_migrations (
      id TEXT PRIMARY KEY,
      checksum TEXT NOT NULL,
      applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    )
  `);
}

async function readAppliedMigrations(
  client: PostgresSqlClientLike,
): Promise<AppliedPostgresMigrationRow[]> {
  return client.unsafe<AppliedPostgresMigrationRow>(`
    SELECT id, checksum, applied_at::TEXT AS applied_at
    FROM notmid_schema_migrations
    ORDER BY id ASC
  `);
}

async function applySinglePostgresMigration(
  client: PostgresSqlClientLike,
  migration: PostgresMigrationDefinition,
): Promise<void> {
  await client.unsafe("BEGIN");

  try {
    for (const statement of splitPostgresStatements(migration.sql)) {
      await client.unsafe(statement);
    }

    await client.unsafe(
      `
        INSERT INTO notmid_schema_migrations (id, checksum)
        VALUES ($1, $2)
      `,
      [migration.id, migration.checksum],
    );
    await client.unsafe("COMMIT");
  } catch (error) {
    await client.unsafe("ROLLBACK");
    throw error;
  }
}

function checksumForPostgresMigration(sql: string): string {
  return createHash("sha256").update(normalizePostgresSql(sql)).digest("hex");
}

function normalizePostgresSql(sql: string): string {
  return sql.replace(/\r\n/g, "\n").trim();
}
