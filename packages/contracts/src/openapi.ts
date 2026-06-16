import { notmidOpenApiPaths } from "./openapiPaths";
import { notmidOpenApiSchemas } from "./openapiSchemas";

export const notmidOpenApiDocument = {
  openapi: "3.1.0",
  info: {
    title: "notmid API",
    version: "0.1.0",
    description:
      "Contract surface for the notmid server-first product API. Local fixture mode is supported, but production deployments must provide real auth and environment configuration.",
  },
  servers: [
    {
      url: "http://localhost:8787",
      description: "Local development",
    },
    {
      url: "https://thdev.app",
      description: "Production host placeholder",
    },
  ],
  tags: [
    { name: "health" },
    { name: "auth" },
    { name: "feed" },
    { name: "map" },
    { name: "capture" },
    { name: "inbox" },
    { name: "profile" },
    { name: "deeplinks" },
  ],
  paths: notmidOpenApiPaths,
  components: {
    securitySchemes: {
      bearerAuth: {
        type: "http",
        scheme: "bearer",
      },
    },
    schemas: notmidOpenApiSchemas,
  },
} as const;

export type NotmidOpenApiDocument = typeof notmidOpenApiDocument;
