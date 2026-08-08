import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // The Android client is the only consumer; no image or font pipeline needed.
  reactStrictMode: true,
};

export default nextConfig;
