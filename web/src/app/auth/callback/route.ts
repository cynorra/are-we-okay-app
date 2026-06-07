import { NextResponse } from 'next/server';
import { createClient } from '../../../utils/supabase/server';

export async function GET(request: Request) {
  const { searchParams, origin } = new URL(request.url);
  const code = searchParams.get('code');
  const next = searchParams.get('next') ?? '/checkin';

  if (code) {
    const supabase = await createClient();
    
    // Check if client is initialized (not empty object due to missing env variables)
    if (supabase && typeof supabase.auth === 'object') {
      const { data: { session }, error } = await supabase.auth.exchangeCodeForSession(code);
      
      if (!error && session?.user) {
        const user = session.user;
        
        // Check if user exists in public.users table
        const { data: existingProfile } = await supabase
          .from('users')
          .select('id')
          .eq('id', user.id)
          .maybeSingle();

        if (!existingProfile) {
          // Create a default profile
          const email = user.email || '';
          const baseUsername = email.split('@')[0] || 'user';
          // Append a small random hash to ensure uniqueness
          const randomId = Math.random().toString(36).substring(2, 6);
          const username = `${baseUsername}_${randomId}`;
          
          await supabase
            .from('users')
            .insert({
              id: user.id,
              email: email,
              username: username,
              display_name: user.user_metadata?.full_name || baseUsername,
              avatar_emoji: '😎',
              role: 'user',
            });
        }
        
        return NextResponse.redirect(`${origin}${next}`);
      }
    }
  }

  // If anything fails, redirect back to login
  return NextResponse.redirect(`${origin}/auth/login?error=auth_failed`);
}
