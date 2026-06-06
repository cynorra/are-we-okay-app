"use client";

import { useEffect, useState } from "react";
import { Heart, MessageCircle, Send, Check } from "lucide-react";
import { motion, AnimatePresence, Variants } from "framer-motion";
import { getFeedPosts, addReaction, removeReaction, addComment, Post, Comment } from "../../../utils/db";

const containerVariants: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.1 }
  }
};

const itemVariants: Variants = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: "easeOut" } }
};

const REACTION_EMOJIS = {
  hug: "🫂",
  feel_this: "❤️",
  strength: "💪",
  you_got_this: "✨"
};

const REACTION_LABELS = {
  hug: "Hug",
  feel_this: "Feel this",
  strength: "Strength",
  you_got_this: "You got this"
};

export default function FeedPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'good' | 'bad' | 'unsure'>('all');
  const [commentInputs, setCommentInputs] = useState<{ [postId: string]: string }>({});
  const [activeCommentPostId, setActiveCommentPostId] = useState<string | null>(null);

  useEffect(() => {
    loadPosts();
  }, []);

  const loadPosts = async () => {
    setLoading(true);
    const data = await getFeedPosts();
    setPosts(data);
    setLoading(false);
  };

  const handleToggleReaction = async (postId: string, type: 'hug' | 'feel_this' | 'strength' | 'you_got_this') => {
    const postIndex = posts.findIndex(p => p.id === postId);
    if (postIndex === -1) return;

    const post = posts[postIndex];
    const userReacted = post.userReactions?.includes(type);

    // Optimistic UI Update
    const updatedPosts = [...posts];
    const updatedPost = { ...post };
    updatedPost.userReactions = userReacted 
      ? updatedPost.userReactions.filter(t => t !== type)
      : [...(updatedPost.userReactions || []), type];
      
    updatedPost.reactions = {
      ...updatedPost.reactions,
      [type]: Math.max(0, (updatedPost.reactions[type] || 0) + (userReacted ? -1 : 1))
    };
    updatedPosts[postIndex] = updatedPost;
    setPosts(updatedPosts);

    // Call API
    if (userReacted) {
      await removeReaction(postId, type);
    } else {
      await addReaction(postId, type);
    }
  };

  const handleAddComment = async (postId: string) => {
    const content = commentInputs[postId]?.trim();
    if (!content) return;

    // Call API
    const newComment = await addComment(postId, content);
    if (newComment) {
      setPosts(posts.map(p => {
        if (p.id === postId) {
          return {
            ...p,
            comments: [...(p.comments || []), newComment]
          };
        }
        return p;
      }));
      setCommentInputs({
        ...commentInputs,
        [postId]: ""
      });
    }
  };

  const filteredPosts = posts.filter(post => {
    if (filter === 'all') return true;
    return post.mood === filter;
  });

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full max-w-3xl mx-auto"
    >
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-4xl font-bold mb-2 tracking-tight text-[var(--color-ok-black)]">Global Feed</h1>
          <p className="text-gray-500 text-lg">Anonymous thoughts from around the world.</p>
        </div>
        
        {/* Filtering Tags */}
        <div className="flex flex-wrap gap-2">
          {(['all', 'good', 'bad', 'unsure'] as const).map((type) => (
            <button
              key={type}
              onClick={() => setFilter(type)}
              className={`px-4 py-2 rounded-full text-sm font-semibold transition-all cursor-pointer ${
                filter === type 
                  ? 'bg-[var(--color-ok-black)] text-white shadow-md' 
                  : 'bg-white/80 text-gray-600 border border-gray-100 hover:bg-white'
              }`}
            >
              {type === 'all' && '🌍 All'}
              {type === 'good' && '😎 Good'}
              {type === 'bad' && '😔 Not Good'}
              {type === 'unsure' && '🤔 Unsure'}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400">
          <div className="w-12 h-12 border-4 border-[var(--color-ok-orange)] border-t-transparent rounded-full animate-spin mb-4"></div>
          <p className="text-lg">Fetching emotional weather...</p>
        </div>
      ) : filteredPosts.length === 0 ? (
        <div className="bg-white/80 backdrop-blur-md p-16 rounded-[2.5rem] text-center border border-gray-100/50 shadow-sm flex flex-col items-center">
          <span className="text-6xl mb-6">⛅</span>
          <h3 className="text-2xl font-bold text-gray-700 mb-2">No posts here yet</h3>
          <p className="text-gray-500 max-w-md">Be the first to share how you're feeling today! Check-in and check public sharing.</p>
        </div>
      ) : (
        <motion.div 
          variants={containerVariants}
          initial="hidden"
          animate="show"
          className="space-y-6"
        >
          {filteredPosts.map((post) => (
            <motion.div 
              variants={itemVariants}
              key={post.id} 
              className="bg-white/90 backdrop-blur-md p-6 rounded-[2.5rem] shadow-sm border border-gray-100/60 hover:shadow-lg hover:border-gray-200/40 transition-all duration-300"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-gray-50 flex items-center justify-center text-2xl border border-gray-100 shadow-inner">
                    {post.mood === 'good' ? '😎' : post.mood === 'bad' ? '😔' : '🤔'}
                  </div>
                  <div>
                    <div className="font-bold text-gray-900 flex items-center gap-2">
                      <span>{post.username}</span>
                      {post.avatar_emoji && !post.is_anonymous && (
                        <span className="text-sm bg-gray-100 px-2 py-0.5 rounded-full">{post.avatar_emoji}</span>
                      )}
                    </div>
                    <div className="text-sm text-gray-400">
                      {new Date(post.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                </div>
              </div>
              
              <p className="text-gray-800 text-lg mb-6 leading-relaxed px-2 font-medium">
                {post.content}
              </p>

              {/* Support Emoji Reactions */}
              <div className="flex flex-wrap gap-2 mb-4 border-t border-gray-100 pt-4">
                {(Object.keys(REACTION_EMOJIS) as Array<keyof typeof REACTION_EMOJIS>).map((type) => {
                  const count = post.reactions[type] || 0;
                  const active = post.userReactions?.includes(type);
                  return (
                    <motion.button 
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      key={type}
                      onClick={() => handleToggleReaction(post.id, type)}
                      className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-semibold transition-all cursor-pointer ${
                        active 
                          ? 'bg-[var(--color-ok-orange)] text-white shadow-md' 
                          : 'bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-100/50'
                      }`}
                    >
                      <span className="text-base">{REACTION_EMOJIS[type]}</span>
                      <span>{REACTION_LABELS[type]}</span>
                      {count > 0 && <span className={`ml-1 text-xs px-1.5 py-0.5 rounded-full ${active ? 'bg-white/20 text-white' : 'bg-gray-200 text-gray-600'}`}>{count}</span>}
                    </motion.button>
                  );
                })}

                <button 
                  onClick={() => setActiveCommentPostId(activeCommentPostId === post.id ? null : post.id)}
                  className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-semibold transition-all ml-auto cursor-pointer ${
                    activeCommentPostId === post.id
                      ? 'bg-blue-500 text-white shadow-sm'
                      : 'bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-100/50'
                  }`}
                >
                  <MessageCircle className="w-4 h-4" />
                  <span>Comments</span>
                  {post.comments && post.comments.length > 0 && (
                    <span className="text-xs ml-1 px-1.5 py-0.5 rounded-full bg-gray-200 text-gray-600">
                      {post.comments.length}
                    </span>
                  )}
                </button>
              </div>

              {/* Collapsible Comments Section */}
              <AnimatePresence>
                {activeCommentPostId === post.id && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="overflow-hidden bg-gray-50/50 rounded-2xl border border-gray-100 p-4 mt-2 space-y-4"
                  >
                    <div className="space-y-3">
                      {post.comments && post.comments.length > 0 ? (
                        post.comments.map((comment) => (
                          <div key={comment.id} className="flex items-start gap-3 bg-white p-3 rounded-xl border border-gray-100/50">
                            <div className="w-8 h-8 rounded-full bg-gray-50 border border-gray-100 flex items-center justify-center text-lg">
                              {comment.avatar_emoji || '👤'}
                            </div>
                            <div className="flex-1">
                              <div className="flex items-center justify-between">
                                <span className="font-bold text-sm text-gray-800">{comment.username}</span>
                                <span className="text-xs text-gray-400">
                                  {new Date(comment.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </span>
                              </div>
                              <p className="text-gray-600 text-sm mt-1">{comment.content}</p>
                            </div>
                          </div>
                        ))
                      ) : (
                        <p className="text-center text-sm text-gray-400 py-3">No supportive comments yet. Be the first!</p>
                      )}
                    </div>

                    {/* New Comment Form */}
                    <div className="flex gap-2">
                      <input
                        type="text"
                        placeholder="Send a supportive message..."
                        value={commentInputs[post.id] || ""}
                        onChange={(e) => setCommentInputs({ ...commentInputs, [post.id]: e.target.value })}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') handleAddComment(post.id);
                        }}
                        className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] bg-white text-black"
                      />
                      <button
                        onClick={() => handleAddComment(post.id)}
                        className="p-2.5 bg-[var(--color-ok-black)] hover:bg-gray-800 text-white rounded-xl transition-all cursor-pointer hover:scale-105"
                      >
                        <Send className="w-4 h-4" />
                      </button>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
        </motion.div>
      )}
    </motion.div>
  );
}
