export function jsonResponse(description: string, schemaName: string) {
  return {
    description,
    content: {
      "application/json": {
        schema: { $ref: `#/components/schemas/${schemaName}` },
      },
    },
  };
}

export function jsonRequest(description: string, schemaName: string) {
  return {
    required: true,
    description,
    content: {
      "application/json": {
        schema: { $ref: `#/components/schemas/${schemaName}` },
      },
    },
  };
}

export function pathParameter(name: string, description: string) {
  return {
    name,
    in: "path",
    required: true,
    description,
    schema: { type: "string" },
  };
}

export function optionalBearerSecurity() {
  return [{}, { bearerAuth: [] }];
}

export function requiredBearerSecurity() {
  return [{ bearerAuth: [] }];
}

export function objectSchema(properties: Record<string, unknown>) {
  return {
    type: "object",
    properties,
  };
}

export function arrayOf(items: unknown) {
  return {
    type: "array",
    items,
  };
}
