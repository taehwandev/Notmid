import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  transpilePackages: ["@notmid/api-client", "@notmid/contracts"],
};

export default nextConfig;
