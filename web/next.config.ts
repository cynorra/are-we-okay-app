import path from 'path';
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  turbopack: {
    resolveAlias: {
      '@supabase/phoenix': './node_modules/@supabase/phoenix',
      'iceberg-js': './node_modules/iceberg-js',
    },
  },
  webpack: (config) => {
    config.resolve.alias = {
      ...config.resolve.alias,
      '@supabase/phoenix': path.resolve(__dirname, 'node_modules/@supabase/phoenix'),
      'iceberg-js': path.resolve(__dirname, 'node_modules/iceberg-js'),
    };
    return config;
  },
};

export default nextConfig;
