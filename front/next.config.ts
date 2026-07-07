import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactCompiler: true,
  turbopack: {},
  webpack: (config, { dev }) => {
    if (dev) {
      config.watchOptions = {
        ...(config.watchOptions ?? {}),
        ignored: ['**/node_modules/**', '**/.next/**', '**/.git/**'],
      };
    }

    return config;
  },
};

export default nextConfig;
