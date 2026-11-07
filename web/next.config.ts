import type { NextConfig } from "next";

// GitHub Pages build (CI): NEXT_OUTPUT_EXPORT=1 + NEXT_PUBLIC_BASE_PATH=/wault
// Local dev: no env needed — plain Next dev server. When self-hosted next to
// the Wault Bridge, the app goes live automatically; otherwise demo mode.
const isExport = process.env.NEXT_OUTPUT_EXPORT === "1";
const basePath = process.env.NEXT_PUBLIC_BASE_PATH || "";

const nextConfig: NextConfig = {
  ...(isExport ? { output: "export" as const } : {}),
  ...(basePath ? { basePath, assetPrefix: basePath } : {}),
  images: { unoptimized: true },
  typescript: {
    ignoreBuildErrors: true,
  },
  reactStrictMode: false,
};

export default nextConfig;
