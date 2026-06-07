// Okayness Database and Authentication Layer
// Automatically switches between Supabase and localStorage fallback.

import { createClient as createSupabaseClient } from './supabase/client';

export interface UserProfile {
  id: string;
  email?: string;
  username: string;
  display_name?: string;
  avatar_emoji: string;
  role: string;
  created_at: string;
}

export interface Checkin {
  id: string;
  user_id: string;
  mood: 'good' | 'bad' | 'unsure';
  note?: string;
  is_public: boolean;
  created_at: string;
}

export interface Post {
  id: string;
  user_id: string;
  username: string;
  avatar_emoji: string;
  checkin_id?: string;
  content: string;
  mood?: 'good' | 'bad' | 'unsure';
  is_anonymous: boolean;
  created_at: string;
  reactions: {
    hug: number;
    feel_this: number;
    strength: number;
    you_got_this: number;
  };
  userReactions: string[]; // List of reaction types current user did
  comments: Comment[];
}

export interface Comment {
  id: string;
  post_id: string;
  user_id: string;
  username: string;
  avatar_emoji: string;
  content: string;
  created_at: string;
}

export interface Friendship {
  id: string;
  requester_id: string;
  addressee_id: string;
  status: 'pending' | 'accepted' | 'blocked';
  created_at: string;
}

// Check if Supabase keys are fully configured
export function isSupabaseConfigured(): boolean {
  return (
    typeof process.env.NEXT_PUBLIC_SUPABASE_URL === 'string' &&
    process.env.NEXT_PUBLIC_SUPABASE_URL.length > 0 &&
    process.env.NEXT_PUBLIC_SUPABASE_URL !== 'undefined' &&
    typeof process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY === 'string' &&
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY.length > 0 &&
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY !== 'undefined'
  );
}

// Safe localStorage helper for SSR
const isClient = typeof window !== 'undefined';

function getLocal<T>(key: string, defaultValue: T): T {
  if (!isClient) return defaultValue;
  const data = localStorage.getItem(key);
  if (!data) return defaultValue;
  try {
    return JSON.parse(data) as T;
  } catch {
    return defaultValue;
  }
}

function setLocal<T>(key: string, value: T): void {
  if (!isClient) return;
  localStorage.setItem(key, JSON.stringify(value));
}

// Initial seed data for demo mode
const INITIAL_USERS: UserProfile[] = [
  { id: 'usr-1', username: 'stressed_coder', avatar_emoji: '💻', role: 'user', created_at: new Date().toISOString() },
  { id: 'usr-2', username: 'exam_winner', avatar_emoji: '🎓', role: 'user', created_at: new Date().toISOString() },
  { id: 'usr-3', username: 'wanderer', avatar_emoji: '⛵', role: 'user', created_at: new Date().toISOString() },
];

const INITIAL_POSTS: Post[] = [
  {
    id: 'post-1',
    user_id: 'usr-1',
    username: 'stressed_coder',
    avatar_emoji: '💻',
    content: "Just feeling really overwhelmed with work lately. Doesn't seem to end.",
    mood: 'bad',
    is_anonymous: true,
    created_at: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2h ago
    reactions: { hug: 12, feel_this: 8, strength: 3, you_got_this: 1 },
    userReactions: [],
    comments: [
      { id: 'c-1', post_id: 'post-1', user_id: 'usr-2', username: 'exam_winner', avatar_emoji: '🎓', content: "Hang in there, take short breaks! 🫂", created_at: new Date(Date.now() - 1.5 * 60 * 60 * 1000).toISOString() }
    ]
  },
  {
    id: 'post-2',
    user_id: 'usr-2',
    username: 'exam_winner',
    avatar_emoji: '🎓',
    content: "I passed my final exam! So relieved.",
    mood: 'good',
    is_anonymous: false,
    created_at: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(), // 4h ago
    reactions: { hug: 85, feel_this: 5, strength: 42, you_got_this: 10 },
    userReactions: [],
    comments: []
  },
  {
    id: 'post-3',
    user_id: 'usr-3',
    username: 'wanderer',
    avatar_emoji: '⛵',
    content: "Not sure where I'm going in life right now, but taking it one day at a time.",
    mood: 'unsure',
    is_anonymous: true,
    created_at: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(), // 5h ago
    reactions: { hug: 45, feel_this: 32, strength: 8, you_got_this: 4 },
    userReactions: [],
    comments: []
  }
];

// Seed storage if empty
if (isClient) {
  if (!localStorage.getItem('ok_users')) {
    setLocal('ok_users', INITIAL_USERS);
  }
  if (!localStorage.getItem('ok_posts')) {
    setLocal('ok_posts', INITIAL_POSTS);
  }
}

// -------------------------------------------------------------
// Authentication Services
// -------------------------------------------------------------

export async function getCurrentUser(): Promise<UserProfile | null> {
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) return null;
      
      const { data, error } = await supabase
        .from('users')
        .select('*')
        .eq('id', user.id)
        .single();
      
      if (error || !data) return null;
      return data as UserProfile;
    } catch (e) {
      console.error('Supabase get user error:', e);
      return null;
    }
  } else {
    return getLocal<UserProfile | null>('ok_session', null);
  }
}

export async function signUp(email: string, password: string, username: string, avatarEmoji: string = '🌙'): Promise<{ user: UserProfile | null; error: string | null }> {
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { data: authData, error: authError } = await supabase.auth.signUp({ email, password });
      
      if (authError || !authData.user) {
        return { user: null, error: authError?.message || 'Authentication signup failed' };
      }
      
      const newProfile: UserProfile = {
        id: authData.user.id,
        email,
        username,
        avatar_emoji: avatarEmoji,
        role: 'user',
        created_at: new Date().toISOString()
      };
      
      const { error: profileError } = await supabase.from('users').insert(newProfile);
      if (profileError) {
        return { user: null, error: profileError.message };
      }
      
      return { user: newProfile, error: null };
    } catch (e: any) {
      return { user: null, error: e.message || 'Unknown error occurred' };
    }
  } else {
    // Local mode simulation
    const users = getLocal<UserProfile[]>('ok_users', []);
    if (users.some(u => u.email === email)) {
      return { user: null, error: 'Email already registered.' };
    }
    if (users.some(u => u.username.toLowerCase() === username.toLowerCase())) {
      return { user: null, error: 'Username already taken.' };
    }
    
    const newUser: UserProfile = {
      id: 'usr-' + Math.random().toString(36).substr(2, 9),
      email,
      username,
      avatar_emoji: avatarEmoji,
      role: 'user',
      created_at: new Date().toISOString()
    };
    
    users.push(newUser);
    setLocal('ok_users', users);
    setLocal('ok_session', newUser);
    return { user: newUser, error: null };
  }
}

export async function signIn(email: string, password: string): Promise<{ user: UserProfile | null; error: string | null }> {
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { data: authData, error: authError } = await supabase.auth.signInWithPassword({ email, password });
      if (authError || !authData.user) {
        return { user: null, error: authError?.message || 'Invalid credentials' };
      }
      
      const { data, error: profileError } = await supabase
        .from('users')
        .select('*')
        .eq('id', authData.user.id)
        .single();
      
      if (profileError || !data) {
        return { user: null, error: profileError?.message || 'Profile load failed' };
      }
      
      return { user: data as UserProfile, error: null };
    } catch (e: any) {
      return { user: null, error: e.message || 'Unknown login error' };
    }
  } else {
    // Local mode simulation
    const users = getLocal<UserProfile[]>('ok_users', []);
    const user = users.find(u => u.email === email);
    if (!user) {
      return { user: null, error: 'User not found. Try signing up!' };
    }
    // Simulation accepts any password for existing emails
    setLocal('ok_session', user);
    return { user, error: null };
  }
}

export async function signInWithGoogle(): Promise<{ error: string | null }> {
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: `${window.location.origin}/auth/callback`,
        },
      });
      return { error: error?.message || null };
    } catch (e: any) {
      return { error: e.message || 'Failed to initiate Google sign in' };
    }
  } else {
    return { error: 'Google Sign In is not configured. Please define NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_ANON_KEY in your .env.local file.' };
  }
}

export async function signOut(): Promise<void> {
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      await supabase.auth.signOut();
    } catch (e) {
      console.error(e);
    }
  } else {
    if (isClient) {
      localStorage.removeItem('ok_session');
    }
  }
}

// -------------------------------------------------------------
// Check-in & Post Services
// -------------------------------------------------------------

export async function createCheckin(mood: 'good' | 'bad' | 'unsure', note: string, isPublic: boolean): Promise<{ checkin: Checkin | null; error: string | null }> {
  const user = await getCurrentUser();
  if (!user) return { checkin: null, error: 'You must be logged in to check in' };

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const newCheckin = {
        user_id: user.id,
        mood,
        note: note || null,
        is_public: isPublic,
      };
      
      const { data, error } = await supabase
        .from('checkins')
        .insert(newCheckin)
        .select()
        .single();
        
      if (error) return { checkin: null, error: error.message };

      // If public, also create a post
      if (isPublic && note) {
        const newPost = {
          user_id: user.id,
          checkin_id: data.id,
          content: note,
          mood,
          is_anonymous: true, // Default to anonymous as per schema / rules
        };
        await supabase.from('posts').insert(newPost);
      }
      
      return { checkin: data as Checkin, error: null };
    } catch (e: any) {
      return { checkin: null, error: e.message || 'Failed to create check-in' };
    }
  } else {
    // Local mode simulation
    const checkins = getLocal<Checkin[]>('ok_checkins', []);
    const newCheckin: Checkin = {
      id: 'chk-' + Math.random().toString(36).substr(2, 9),
      user_id: user.id,
      mood,
      note,
      is_public: isPublic,
      created_at: new Date().toISOString()
    };
    
    checkins.push(newCheckin);
    setLocal('ok_checkins', checkins);

    if (isPublic && note) {
      const posts = getLocal<Post[]>('ok_posts', []);
      const newPost: Post = {
        id: 'post-' + Math.random().toString(36).substr(2, 9),
        user_id: user.id,
        username: user.username,
        avatar_emoji: user.avatar_emoji,
        checkin_id: newCheckin.id,
        content: note,
        mood,
        is_anonymous: true,
        created_at: new Date().toISOString(),
        reactions: { hug: 0, feel_this: 0, strength: 0, you_got_this: 0 },
        userReactions: [],
        comments: []
      };
      posts.unshift(newPost); // Add at the start of feed
      setLocal('ok_posts', posts);
    }
    
    return { checkin: newCheckin, error: null };
  }
}

export async function getFeedPosts(): Promise<Post[]> {
  const user = await getCurrentUser();
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      // Fetch posts, join with profiles
      const { data, error } = await supabase
        .from('posts')
        .select(`
          *,
          users:user_id (username, avatar_emoji)
        `)
        .is('deleted_at', null)
        .eq('status', 'approved')
        .order('created_at', { ascending: false });
        
      if (error || !data) return [];
      
      // Fetch reactions to map
      const postsWithReactions = await Promise.all(data.map(async (p: any) => {
        const { data: rxList } = await supabase
          .from('reactions')
          .select('type, user_id')
          .eq('post_id', p.id);
          
        const rxCounts = { hug: 0, feel_this: 0, strength: 0, you_got_this: 0 };
        const myReactions: string[] = [];
        
        rxList?.forEach((rx: any) => {
          if (rx.type in rxCounts) {
            rxCounts[rx.type as keyof typeof rxCounts]++;
          }
          if (user && rx.user_id === user.id) {
            myReactions.push(rx.type);
          }
        });

        return {
          id: p.id,
          user_id: p.user_id,
          username: p.is_anonymous ? 'Anonymous' : (p.users?.username || 'User'),
          avatar_emoji: p.is_anonymous ? '🌙' : (p.users?.avatar_emoji || '👤'),
          checkin_id: p.checkin_id,
          content: p.content,
          mood: p.mood,
          is_anonymous: p.is_anonymous,
          created_at: p.created_at,
          reactions: rxCounts,
          userReactions: myReactions,
          comments: [] // Handle comments simply or fetch separately
        } as Post;
      }));
      
      return postsWithReactions;
    } catch (e) {
      console.error(e);
      return [];
    }
  } else {
    // Local mode simulation
    const posts = getLocal<Post[]>('ok_posts', []);
    // Map username/avatar emojis based on latest local profile states
    const users = getLocal<UserProfile[]>('ok_users', []);
    return posts.map(p => {
      const author = users.find(u => u.id === p.user_id);
      return {
        ...p,
        username: p.is_anonymous ? 'Anonymous' : (author?.username || p.username),
        avatar_emoji: p.is_anonymous ? '🌙' : (author?.avatar_emoji || p.avatar_emoji),
        userReactions: p.userReactions || []
      };
    });
  }
}

export async function addReaction(postId: string, type: 'hug' | 'feel_this' | 'strength' | 'you_got_this'): Promise<boolean> {
  const user = await getCurrentUser();
  if (!user) return false;

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { error } = await supabase.from('reactions').insert({
        post_id: postId,
        user_id: user.id,
        type,
      });
      return !error;
    } catch {
      return false;
    }
  } else {
    // Local mode simulation
    const posts = getLocal<Post[]>('ok_posts', []);
    const post = posts.find(p => p.id === postId);
    if (post) {
      if (!post.userReactions) post.userReactions = [];
      if (!post.userReactions.includes(type)) {
        post.userReactions.push(type);
        post.reactions[type] = (post.reactions[type] || 0) + 1;
        setLocal('ok_posts', posts);
        return true;
      }
    }
    return false;
  }
}

export async function removeReaction(postId: string, type: 'hug' | 'feel_this' | 'strength' | 'you_got_this'): Promise<boolean> {
  const user = await getCurrentUser();
  if (!user) return false;

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { error } = await supabase
        .from('reactions')
        .delete()
        .eq('post_id', postId)
        .eq('user_id', user.id)
        .eq('type', type);
      return !error;
    } catch {
      return false;
    }
  } else {
    // Local mode simulation
    const posts = getLocal<Post[]>('ok_posts', []);
    const post = posts.find(p => p.id === postId);
    if (post && post.userReactions) {
      const index = post.userReactions.indexOf(type);
      if (index > -1) {
        post.userReactions.splice(index, 1);
        post.reactions[type] = Math.max(0, (post.reactions[type] || 0) - 1);
        setLocal('ok_posts', posts);
        return true;
      }
    }
    return false;
  }
}

export async function addComment(postId: string, content: string): Promise<Comment | null> {
  const user = await getCurrentUser();
  if (!user) return null;

  const newComment: Comment = {
    id: 'comm-' + Math.random().toString(36).substr(2, 9),
    post_id: postId,
    user_id: user.id,
    username: user.username,
    avatar_emoji: user.avatar_emoji,
    content,
    created_at: new Date().toISOString()
  };

  if (isSupabaseConfigured()) {
    // In real app we could have a comments table. For this mock layout we can return it.
    return newComment;
  } else {
    const posts = getLocal<Post[]>('ok_posts', []);
    const post = posts.find(p => p.id === postId);
    if (post) {
      if (!post.comments) post.comments = [];
      post.comments.push(newComment);
      setLocal('ok_posts', posts);
      return newComment;
    }
    return null;
  }
}

// -------------------------------------------------------------
// Friends & Relationship Services
// -------------------------------------------------------------

export async function searchUsers(query: string): Promise<UserProfile[]> {
  const user = await getCurrentUser();
  if (!user) return [];

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { data, error } = await supabase
        .from('users')
        .select('*')
        .ilike('username', `%${query}%`)
        .neq('id', user.id)
        .limit(10);
      return error ? [] : (data as UserProfile[]);
    } catch {
      return [];
    }
  } else {
    // Local mode simulation
    const users = getLocal<UserProfile[]>('ok_users', []);
    return users.filter(
      u => u.id !== user.id && u.username.toLowerCase().includes(query.toLowerCase())
    );
  }
}

export async function sendFriendRequest(targetUserId: string): Promise<boolean> {
  const user = await getCurrentUser();
  if (!user) return false;

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { error } = await supabase.from('friendships').insert({
        requester_id: user.id,
        addressee_id: targetUserId,
        status: 'pending',
      });
      return !error;
    } catch {
      return false;
    }
  } else {
    // Local mode simulation
    const friendships = getLocal<Friendship[]>('ok_friendships', []);
    const exists = friendships.some(
      f =>
        (f.requester_id === user.id && f.addressee_id === targetUserId) ||
        (f.requester_id === targetUserId && f.addressee_id === user.id)
    );
    if (exists) return false;

    friendships.push({
      id: 'frnd-' + Math.random().toString(36).substr(2, 9),
      requester_id: user.id,
      addressee_id: targetUserId,
      status: 'pending',
      created_at: new Date().toISOString()
    });
    setLocal('ok_friendships', friendships);
    return true;
  }
}

export async function getFriendRequests(): Promise<{ id: string; user: UserProfile }[]> {
  const user = await getCurrentUser();
  if (!user) return [];

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { data, error } = await supabase
        .from('friendships')
        .select(`
          id,
          requester:requester_id (id, username, avatar_emoji)
        `)
        .eq('addressee_id', user.id)
        .eq('status', 'pending');
        
      if (error || !data) return [];
      return data.map((d: any) => ({
        id: d.id,
        user: d.requester as UserProfile
      }));
    } catch {
      return [];
    }
  } else {
    // Local mode simulation
    const friendships = getLocal<Friendship[]>('ok_friendships', []);
    const users = getLocal<UserProfile[]>('ok_users', []);
    const incoming = friendships.filter(f => f.addressee_id === user.id && f.status === 'pending');
    
    return incoming.map(f => {
      const reqUser = users.find(u => u.id === f.requester_id) || {
        id: f.requester_id,
        username: 'Unknown',
        avatar_emoji: '👤',
        role: 'user',
        created_at: ''
      };
      return {
        id: f.id,
        user: reqUser as UserProfile
      };
    });
  }
}

export async function acceptFriendRequest(requestId: string): Promise<boolean> {
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { error } = await supabase
        .from('friendships')
        .update({ status: 'accepted' })
        .eq('id', requestId);
      return !error;
    } catch {
      return false;
    }
  } else {
    // Local mode simulation
    const friendships = getLocal<Friendship[]>('ok_friendships', []);
    const fr = friendships.find(f => f.id === requestId);
    if (fr) {
      fr.status = 'accepted';
      setLocal('ok_friendships', friendships);
      return true;
    }
    return false;
  }
}

export async function getFriendsWithMoods(): Promise<{ user: UserProfile; lastCheckin?: Checkin }[]> {
  const user = await getCurrentUser();
  if (!user) return [];

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      // Fetch accepted friendships
      const { data: frList } = await supabase
        .from('friendships')
        .select('*')
        .or(`requester_id.eq.${user.id},addressee_id.eq.${user.id}`)
        .eq('status', 'accepted');
        
      if (!frList || frList.length === 0) return [];
      
      const friendIds = frList.map((f: any) => f.requester_id === user.id ? f.addressee_id : f.requester_id);
      
      const { data: profiles } = await supabase
        .from('users')
        .select('*')
        .in('id', friendIds);
        
      if (!profiles) return [];
      
      // Get latest check-in for each friend today (last 24 hours)
      const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
      const { data: checkins } = await supabase
        .from('checkins')
        .select('*')
        .in('user_id', friendIds)
        .gt('created_at', oneDayAgo)
        .order('created_at', { ascending: false });

      return profiles.map((p: any) => {
        const friendCheckin = checkins?.find((c: any) => c.user_id === p.id);
        return {
          user: p as UserProfile,
          lastCheckin: friendCheckin as Checkin | undefined
        };
      });
    } catch {
      return [];
    }
  } else {
    // Local mode simulation
    const friendships = getLocal<Friendship[]>('ok_friendships', []);
    const users = getLocal<UserProfile[]>('ok_users', []);
    const checkins = getLocal<Checkin[]>('ok_checkins', []);
    
    const acceptedList = friendships.filter(
      f => (f.requester_id === user.id || f.addressee_id === user.id) && f.status === 'accepted'
    );
    
    const friendIds = acceptedList.map(f => f.requester_id === user.id ? f.addressee_id : f.requester_id);
    const oneDayAgo = Date.now() - 24 * 60 * 60 * 1000;

    return friendIds.map(fId => {
      const friendProfile = users.find(u => u.id === fId) || {
        id: fId,
        username: 'Friend',
        avatar_emoji: '😊',
        role: 'user',
        created_at: ''
      };
      
      // Find latest checkin from last 24h
      const friendCheckin = checkins
        .filter(c => c.user_id === fId && new Date(c.created_at).getTime() > oneDayAgo)
        .sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime())[0];

      return {
        user: friendProfile as UserProfile,
        lastCheckin: friendCheckin
      };
    });
  }
}

// -------------------------------------------------------------
// Insights & Streak Calculation
// -------------------------------------------------------------

export async function getUserStats(): Promise<{ streak: number; supportGiven: number; moodCounts: { good: number; bad: number; unsure: number } }> {
  const user = await getCurrentUser();
  if (!user) return { streak: 0, supportGiven: 0, moodCounts: { good: 0, bad: 0, unsure: 0 } };

  let userCheckins: Checkin[] = [];
  let supportCount = 0;

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { data: chk } = await supabase
        .from('checkins')
        .select('*')
        .eq('user_id', user.id)
        .order('created_at', { ascending: false });
        
      userCheckins = chk ? (chk as Checkin[]) : [];
      
      const { count } = await supabase
        .from('reactions')
        .select('*', { count: 'exact', head: true })
        .eq('user_id', user.id);
        
      supportCount = count || 0;
    } catch (e) {
      console.error(e);
    }
  } else {
    // Local simulation
    const checkins = getLocal<Checkin[]>('ok_checkins', []);
    userCheckins = checkins
      .filter(c => c.user_id === user.id)
      .sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());
      
    // Count reactions where user is in userReactions list
    const posts = getLocal<Post[]>('ok_posts', []);
    posts.forEach(p => {
      if (p.userReactions && p.userReactions.length > 0 && p.user_id !== user.id) {
        supportCount += p.userReactions.length;
      }
    });
  }

  // Calculate Streak
  let streak = 0;
  if (userCheckins.length > 0) {
    const dates = userCheckins.map(c => new Date(c.created_at).toDateString());
    const uniqueDates = Array.from(new Set(dates)); // Distinct days checked in
    
    let currentCheckDate = new Date();
    // Check if user checked in today or yesterday to continue streak
    const todayStr = currentCheckDate.toDateString();
    currentCheckDate.setDate(currentCheckDate.getDate() - 1);
    const yesterdayStr = currentCheckDate.toDateString();
    
    if (uniqueDates.includes(todayStr) || uniqueDates.includes(yesterdayStr)) {
      let tempDate = uniqueDates.includes(todayStr) ? new Date() : currentCheckDate;
      while (true) {
        if (uniqueDates.includes(tempDate.toDateString())) {
          streak++;
          tempDate.setDate(tempDate.getDate() - 1);
        } else {
          break;
        }
      }
    }
  }

  // Mood distributions
  const moodCounts = { good: 0, bad: 0, unsure: 0 };
  userCheckins.forEach(c => {
    if (c.mood in moodCounts) {
      moodCounts[c.mood]++;
    }
  });

  return { streak, supportGiven: supportCount, moodCounts };
}

export async function getWeeklyMoods(): Promise<{ day: string; mood: 'good' | 'bad' | 'unsure' | null }[]> {
  const user = await getCurrentUser();
  if (!user) return [];

  let userCheckins: Checkin[] = [];
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();
      const { data } = await supabase
        .from('checkins')
        .select('*')
        .eq('user_id', user.id)
        .gt('created_at', sevenDaysAgo)
        .order('created_at', { ascending: true });
      userCheckins = data ? (data as Checkin[]) : [];
    } catch {}
  } else {
    const checkins = getLocal<Checkin[]>('ok_checkins', []);
    const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
    userCheckins = checkins
      .filter(c => c.user_id === user.id && new Date(c.created_at).getTime() > sevenDaysAgo)
      .sort((a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime());
  }

  const daysOfWeek = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const result = [];
  
  for (let i = 6; i >= 0; i--) {
    const targetDate = new Date();
    targetDate.setDate(targetDate.getDate() - i);
    const dateStr = targetDate.toDateString();
    
    const dayCheckin = userCheckins.find(c => new Date(c.created_at).toDateString() === dateStr);
    
    result.push({
      day: daysOfWeek[targetDate.getDay()],
      mood: dayCheckin ? dayCheckin.mood : null
    });
  }

  return result;
}

// -------------------------------------------------------------
// Waitlist Services
// -------------------------------------------------------------

export async function joinWaitlist(email: string, mood: string): Promise<boolean> {
  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const { error } = await supabase.from('waitlist').insert({ email, mood });
      return !error;
    } catch {
      return false;
    }
  } else {
    const waitlist = getLocal<any[]>('ok_waitlist', []);
    if (!waitlist.some(w => w.email === email)) {
      waitlist.push({ email, mood, created_at: new Date().toISOString() });
      setLocal('ok_waitlist', waitlist);
    }
    return true;
  }
}

export async function updateProfile(username: string, avatarEmoji: string): Promise<{ user: UserProfile | null; error: string | null }> {
  const user = await getCurrentUser();
  if (!user) return { user: null, error: 'You must be logged in' };

  if (isSupabaseConfigured()) {
    try {
      const supabase = createSupabaseClient();
      const updates = {
        username,
        avatar_emoji: avatarEmoji,
      };
      
      const { data, error } = await supabase
        .from('users')
        .update(updates)
        .eq('id', user.id)
        .select()
        .single();
        
      if (error) return { user: null, error: error.message };
      return { user: data as UserProfile, error: null };
    } catch (e: any) {
      return { user: null, error: e.message || 'Failed to update profile' };
    }
  } else {
    // Local mode simulation
    const users = getLocal<UserProfile[]>('ok_users', []);
    const userIndex = users.findIndex(u => u.id === user.id);
    if (userIndex > -1) {
      // Check if username is taken by another user
      if (users.some((u, idx) => idx !== userIndex && u.username.toLowerCase() === username.toLowerCase())) {
        return { user: null, error: 'Username already taken.' };
      }
      users[userIndex].username = username;
      users[userIndex].avatar_emoji = avatarEmoji;
      setLocal('ok_users', users);
      setLocal('ok_session', users[userIndex]);
      return { user: users[userIndex], error: null };
    }
    return { user: null, error: 'User session not found' };
  }
}
