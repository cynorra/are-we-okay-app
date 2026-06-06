import { createBrowserClient } from '@supabase/ssr'

export function createClient() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

  if (!url || !anonKey || url === 'undefined' || anonKey === 'undefined') {
    // Return empty client mock to prevent crash during SSR/CSR init when keys are missing
    return {} as any;
  }

  return createBrowserClient(url, anonKey);
}

