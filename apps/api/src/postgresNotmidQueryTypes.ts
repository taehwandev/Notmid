export type PostgresQueryResult<Row> = {
  rows: Row[];
};

export type PostgresQueryClient = {
  query: <Row>(sql: string, values?: readonly unknown[]) => Promise<PostgresQueryResult<Row>>;
};
