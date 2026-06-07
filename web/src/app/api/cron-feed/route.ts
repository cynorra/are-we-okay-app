import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

// Setup Supabase admin client using Service Role key to bypass RLS policies
const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || '';
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY || '';

const supabase = createClient(supabaseUrl, supabaseServiceKey, {
  auth: {
    autoRefreshToken: false,
    persistSession: false,
  },
});

// A pool of realistic wellbeing reflections in Turkish and English
const POST_POOL = [
  { content: "Bugün production deploy'u yaparken her şey patladı. Gerçekten çok yorucu bir gündü ama ekibin desteğiyle toparladık. 💻", mood: 'bad', email: 'coder@iyimiyiz.com', username: 'stressed_coder', avatar_emoji: '💻' },
  { content: "Kitap okurken zamanın nasıl geçtiğini unuttum. Kendime vakit ayırmak çok iyi geldi. 📚", mood: 'good', email: 'book@iyimiyiz.com', username: 'bookworm', avatar_emoji: '📚' },
  { content: "Ormanda uzun bir yürüyüş yaptım. Doğa insana cidden şifa veriyor. Zihnimdeki tüm gürültü yok oldu. 🌲🌱", mood: 'good', email: 'nature@iyimiyiz.com', username: 'nature_walks', avatar_emoji: '🌱' },
  { content: "I passed my final exam! So relieved. All those sleepless nights paid off. 🎓", mood: 'good', email: 'student@iyimiyiz.com', username: 'exam_winner', avatar_emoji: '🎓' },
  { content: "A warm cup of coffee and a quiet morning. Today is going to be a good day. ☕", mood: 'good', email: 'coffee@iyimiyiz.com', username: 'coffee_lover', avatar_emoji: '☕' },
  { content: "Not sure where I'm going in life right now, but taking it one day at a time. ⛵", mood: 'unsure', email: 'wanderer@iyimiyiz.com', username: 'wanderer', avatar_emoji: '⛵' },
  { content: "Sometimes the best thing you can do is just let go of what you cannot control. Trust the process. 🧘", mood: 'unsure', email: 'yoga@iyimiyiz.com', username: 'yoga_guru', avatar_emoji: '🧘' },
  { content: "Gece yarısı gelen o anlamsız yalnızlık hissi... Bazen sadece birilerinin sesini duymak istiyor insan. 🦉", mood: 'bad', email: 'owl@iyimiyiz.com', username: 'night_owl', avatar_emoji: '🦉' },
  { content: "Yeni bir yemek tarifi denedim ve biraz yandı ama denemek eğlenceliydi! 🍕", mood: 'unsure', email: 'chef@iyimiyiz.com', username: 'music_chef', avatar_emoji: '🎵' },
  { content: "Listening to the rain outside and reading a classic. Peace is in the small moments. 🌧️", mood: 'good', email: 'owl@iyimiyiz.com', username: 'night_owl', avatar_emoji: '🦉' },
  { content: "Just ordered a double espresso. Time to tackle the rest of the day! ☕ Let's do this.", mood: 'good', email: 'coffee@iyimiyiz.com', username: 'coffee_lover', avatar_emoji: '☕' },
  { content: "Gökyüzü bu akşam çok güzel... Umut doluyum nedense. Her şey düzelecek. ✨", mood: 'good', email: 'wanderer@iyimiyiz.com', username: 'wanderer', avatar_emoji: '⛵' },
  { content: "CSS ile hizalama yaparken ömrümden ömür gitti. Kim buldu bu dikey hizalamayı? 💻", mood: 'unsure', email: 'coder@iyimiyiz.com', username: 'stressed_coder', avatar_emoji: '💻' },
  { content: "Sabah yogası yapıldı, zihin sakinleşti. Bugün güzel kararlar alma günü. 🧘", mood: 'good', email: 'yoga@iyimiyiz.com', username: 'yoga_guru', avatar_emoji: '🧘' },
  { content: "Uzun zamandır görüşmediğim bir arkadaşımla kahve içtim, çok iyi geldi. 🫂", mood: 'good', email: 'coffee@iyimiyiz.com', username: 'coffee_lover', avatar_emoji: '☕' },
  { content: "Sınav stresi yüzünden uykularım kaçtı, umarım her şey yolunda gider. 🎓", mood: 'bad', email: 'student@iyimiyiz.com', username: 'exam_winner', avatar_emoji: '🎓' },
  { content: "Çizim yaparken kafamdaki tüm dertler uçup gidiyor. Sanat gerçekten terapi gibi. 🎨", mood: 'good', email: 'dreamer@iyimiyiz.com', username: 'art_dreamer', avatar_emoji: '🎨' },
  { content: "Biraz yalnız kalıp düşünmeye ihtiyacım var. Bazen kendimizi dinlemek en doğrusu. ⛵", mood: 'unsure', email: 'wanderer@iyimiyiz.com', username: 'wanderer', avatar_emoji: '⛵' }
];

// Helper to check if a user profile exists in public.users, or create it in auth and public tables
async function getOrCreateUser(profile: typeof POST_POOL[0]) {
  // Check if profile exists in users table
  const { data: userProfile, error: selectError } = await supabase
    .from('users')
    .select('id')
    .eq('email', profile.email)
    .maybeSingle();

  if (userProfile) {
    return userProfile.id;
  }

  // Create user in auth.users
  const password = `pass_${Math.random().toString(36).substring(2, 10)}`;
  const { data: authData, error: authError } = await supabase.auth.admin.createUser({
    email: profile.email,
    password: password,
    email_confirm: true,
    user_metadata: {
      username: profile.username,
      avatar_emoji: profile.avatar_emoji,
    },
  });

  if (authError || !authData.user) {
    console.error('Failed to create auth user:', authError);
    throw new Error(authError?.message || 'Auth user creation failed');
  }

  // Insert into public.users table
  const { error: profileError } = await supabase
    .from('users')
    .insert({
      id: authData.user.id,
      email: profile.email,
      username: profile.username,
      avatar_emoji: profile.avatar_emoji,
      display_name: profile.username.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '),
      role: 'user',
    });

  if (profileError) {
    console.error('Failed to insert public user profile:', profileError);
    throw new Error(profileError.message);
  }

  return authData.user.id;
}

export async function POST(request: NextRequest) {
  try {
    const authHeader = request.headers.get('authorization');
    const { searchParams } = new URL(request.url);
    const secretParam = searchParams.get('secret');

    const expectedSecret = process.env.CRON_SECRET;

    // Secure the route with a secret key
    if (expectedSecret && authHeader !== `Bearer ${expectedSecret}` && secretParam !== expectedSecret) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    if (!supabaseUrl || !supabaseServiceKey) {
      return NextResponse.json({ error: 'Supabase server variables not configured' }, { status: 500 });
    }

    // Query last 3 posts in the database to prevent posting the same message consecutively
    const { data: recentPosts } = await supabase
      .from('posts')
      .select('content')
      .order('created_at', { ascending: false })
      .limit(3);

    const recentContents = recentPosts?.map(p => p.content) || [];

    // Filter out posts that were recently posted
    const availablePosts = POST_POOL.filter(p => !recentContents.includes(p.content));
    const poolToUse = availablePosts.length > 0 ? availablePosts : POST_POOL;

    // Pick a random post from the filtered pool
    const chosenPost = poolToUse[Math.floor(Math.random() * poolToUse.length)];

    // Get or create the mock author user ID
    const userId = await getOrCreateUser(chosenPost);

    // Create a public daily check-in for this user to tie the post to
    const { data: checkin, error: checkinError } = await supabase
      .from('checkins')
      .insert({
        user_id: userId,
        mood: chosenPost.mood,
        note: chosenPost.content,
        is_public: true,
      })
      .select()
      .single();

    if (checkinError) {
      console.error('Failed to create check-in:', checkinError);
      return NextResponse.json({ error: checkinError.message }, { status: 500 });
    }

    // Insert the post into public.posts
    const { data: post, error: postError } = await supabase
      .from('posts')
      .insert({
        user_id: userId,
        checkin_id: checkin.id,
        content: chosenPost.content,
        mood: chosenPost.mood,
        is_anonymous: Math.random() > 0.4, // 60% chance to display profile, 40% anonymous
        status: 'approved',
      })
      .select()
      .single();

    if (postError) {
      console.error('Failed to create post:', postError);
      return NextResponse.json({ error: postError.message }, { status: 500 });
    }

    // Add 1 to 3 random reactions from other mock users to simulate community activity
    const otherProfiles = POST_POOL.filter(p => p.username !== chosenPost.username);
    const reactionTypes = ['hug', 'feel_this', 'strength', 'you_got_this'];
    const numReactions = Math.floor(Math.random() * 3) + 1; // 1 to 3 reactions

    const reactionsToInsert = [];
    const usedUsers = new Set<string>();

    for (let i = 0; i < numReactions; i++) {
      const reactorProfile = otherProfiles[Math.floor(Math.random() * otherProfiles.length)];
      
      if (usedUsers.has(reactorProfile.email)) continue;
      usedUsers.add(reactorProfile.email);

      try {
        const reactorId = await getOrCreateUser(reactorProfile);
        const randomType = reactionTypes[Math.floor(Math.random() * reactionTypes.length)];
        reactionsToInsert.push({
          post_id: post.id,
          user_id: reactorId,
          type: randomType,
        });
      } catch (e) {
        console.error('Failed to create reactor user:', e);
      }
    }

    if (reactionsToInsert.length > 0) {
      const { error: rxError } = await supabase.from('reactions').insert(reactionsToInsert);
      if (rxError) {
        console.error('Failed to insert reactions:', rxError);
      }
    }

    return NextResponse.json({
      success: true,
      message: 'Mock feed item posted successfully',
      post: {
        id: post.id,
        author: chosenPost.username,
        content: post.content,
        reactions_count: reactionsToInsert.length,
      },
    });
  } catch (error: any) {
    console.error('CRON route error:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
