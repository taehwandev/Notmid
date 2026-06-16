import postgres from "postgres";
import type { PostgresQueryClient, PostgresQueryResult } from "./postgresNotmidRepository";

export type PostgresSqlClientLike = {
  end: (options?: { timeout?: number }) => Promise<void>;
  unsafe: <Row>(sql: string, values?: readonly unknown[]) => Promise<Row[]>;
};

export type RuntimePostgresQueryClient = PostgresQueryClient & {
  close: () => Promise<void>;
};

export type RuntimePostgresQueryClientOptions = {
  connectionTimeoutMs?: number;
  databaseUrl: string;
  idleTimeoutMs?: number;
  maxConnections?: number;
};

export function createRuntimePostgresQueryClient(
  options: RuntimePostgresQueryClientOptions,
): RuntimePostgresQueryClient {
  return createPostgresQueryClientFromSql(
    postgres(options.databaseUrl, postgresClientOptionsForOptions(options)) as PostgresSqlClientLike,
  );
}

export function createPostgresQueryClientFromSql(
  sqlClient: PostgresSqlClientLike,
): RuntimePostgresQueryClient {
  return {
    async query<Row>(sql: string, values: readonly unknown[] = []) {
      const rows = await sqlClient.unsafe<Row>(sql, values);

      return {
        rows,
      };
    },
    async close() {
      await sqlClient.end({ timeout: 5 });
    },
  };
}

export type RuntimePostgresClientOptions = {
  connect_timeout: number;
  idle_timeout: number;
  max: number;
};

export function postgresClientOptionsForOptions(
  options: RuntimePostgresQueryClientOptions,
): RuntimePostgresClientOptions {
  return {
    max: options.maxConnections ?? 5,
    connect_timeout: millisecondsToSeconds(options.connectionTimeoutMs ?? 5_000),
    idle_timeout: millisecondsToSeconds(options.idleTimeoutMs ?? 30_000),
  };
}

function millisecondsToSeconds(value: number): number {
  return Math.max(1, Math.ceil(value / 1_000));
}
