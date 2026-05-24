"use client";

import { Heart, MessageCircle } from "lucide-react";
import { motion, Variants } from "framer-motion";

// Mock data for the feed
const MOCK_POSTS = [
  { id: 1, mood: 'bad', content: "Just feeling really overwhelmed with work lately. Doesn't seem to end.", time: "2h ago", reactions: 24, comments: 3 },
  { id: 2, mood: 'good', content: "I passed my final exam! So relieved.", time: "4h ago", reactions: 142, comments: 12 },
  { id: 3, mood: 'unsure', content: "Not sure where I'm going in life right now, but taking it one day at a time.", time: "5h ago", reactions: 89, comments: 5 },
];

const containerVariants: Variants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.15 }
  }
};

const itemVariants: Variants = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: "easeOut" } }
};

export default function FeedPage() {
  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full"
    >
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-4xl font-bold mb-2 tracking-tight">Global Feed</h1>
          <p className="text-gray-500 text-lg">Anonymous thoughts from around the world.</p>
        </div>
        <button className="bg-white px-5 py-2.5 rounded-full shadow-sm text-sm font-medium border border-gray-200 cursor-pointer hover:bg-gray-50 transition-colors">
          Filter: All
        </button>
      </div>

      <motion.div 
        variants={containerVariants}
        initial="hidden"
        animate="show"
        className="space-y-6"
      >
        {MOCK_POSTS.map((post) => (
          <motion.div 
            variants={itemVariants}
            key={post.id} 
            className="bg-white/80 backdrop-blur-md p-6 rounded-[2rem] shadow-sm border border-gray-100 hover:shadow-lg transition-all"
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-full bg-gray-50 flex items-center justify-center text-2xl border border-gray-100 shadow-sm">
                  {post.mood === 'good' ? '😎' : post.mood === 'bad' ? '😔' : '🤔'}
                </div>
                <div>
                  <div className="font-bold text-gray-900">Anonymous</div>
                  <div className="text-sm text-gray-400">{post.time}</div>
                </div>
              </div>
            </div>
            
            <p className="text-gray-800 text-lg mb-6 leading-relaxed">
              {post.content}
            </p>

            <div className="flex items-center gap-6 border-t border-gray-100 pt-4">
              <motion.button 
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                className="flex items-center gap-2 text-gray-500 hover:text-[var(--color-ok-orange)] transition-colors group cursor-pointer"
              >
                <Heart className="w-5 h-5 group-hover:fill-[var(--color-ok-orange)]" />
                <span className="font-medium">{post.reactions}</span>
              </motion.button>
              <motion.button 
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                className="flex items-center gap-2 text-gray-500 hover:text-blue-500 transition-colors cursor-pointer"
              >
                <MessageCircle className="w-5 h-5" />
                <span className="font-medium">{post.comments}</span>
              </motion.button>
            </div>
          </motion.div>
        ))}
      </motion.div>
    </motion.div>
  );
}
