"use client";

import { useState } from "react";
import { motion } from "framer-motion";

export default function CheckinPage() {
  const [mood, setMood] = useState<string | null>(null);
  const [note, setNote] = useState("");
  const [isPublic, setIsPublic] = useState(true);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!mood) return;
    alert("Check-in saved!");
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: "easeOut" }}
      className="w-full"
    >
      <h1 className="text-4xl font-bold mb-2 tracking-tight">Daily Check-in</h1>
      <p className="text-gray-500 mb-8 text-lg">Take a moment to reflect. How are you really doing today?</p>

      <form onSubmit={handleSubmit} className="bg-white/80 backdrop-blur-xl p-8 rounded-[2.5rem] shadow-xl border border-gray-100/50">
        <div className="mb-8">
          <h2 className="text-2xl font-bold mb-6">I am feeling...</h2>
          <div className="grid grid-cols-3 gap-4">
            <motion.button 
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              type="button"
              onClick={() => setMood('good')}
              className={`p-6 rounded-[2rem] border-2 transition-all flex flex-col items-center gap-3 cursor-pointer ${mood === 'good' ? 'border-[var(--color-ok-teal)] bg-[var(--color-ok-teal)]/10 shadow-lg shadow-[var(--color-ok-teal)]/20' : 'border-gray-100 hover:border-gray-300 bg-gray-50'}`}
            >
              <span className="text-5xl drop-shadow-sm">😎</span>
              <span className="font-medium text-gray-700">Good</span>
            </motion.button>
            <motion.button 
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              type="button"
              onClick={() => setMood('bad')}
              className={`p-6 rounded-[2rem] border-2 transition-all flex flex-col items-center gap-3 cursor-pointer ${mood === 'bad' ? 'border-[var(--color-ok-orange)] bg-[var(--color-ok-orange)]/10 shadow-lg shadow-[var(--color-ok-orange)]/20' : 'border-gray-100 hover:border-gray-300 bg-gray-50'}`}
            >
              <span className="text-5xl drop-shadow-sm">😔</span>
              <span className="font-medium text-gray-700">Not Good</span>
            </motion.button>
            <motion.button 
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              type="button"
              onClick={() => setMood('unsure')}
              className={`p-6 rounded-[2rem] border-2 transition-all flex flex-col items-center gap-3 cursor-pointer ${mood === 'unsure' ? 'border-gray-800 bg-gray-100 shadow-lg' : 'border-gray-100 hover:border-gray-300 bg-gray-50'}`}
            >
              <span className="text-5xl drop-shadow-sm">🤔</span>
              <span className="font-medium text-gray-700">Unsure</span>
            </motion.button>
          </div>
        </div>

        <motion.div layout className="mb-8">
          <h2 className="text-xl font-bold mb-2">Care to share more?</h2>
          <p className="text-sm text-gray-500 mb-4">Optional. Write down what's on your mind.</p>
          <textarea 
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="I'm feeling this way because..."
            rows={4}
            className="w-full p-5 rounded-2xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] resize-none bg-gray-50/50 transition-all text-lg"
          />
        </motion.div>

        <div className="flex items-center gap-4 mb-8 p-4 bg-gray-50 rounded-2xl border border-gray-100">
          <input 
            type="checkbox" 
            id="public" 
            checked={isPublic}
            onChange={(e) => setIsPublic(e.target.checked)}
            className="w-6 h-6 accent-[var(--color-ok-orange)] cursor-pointer rounded"
          />
          <label htmlFor="public" className="text-gray-700 font-medium cursor-pointer select-none">
            Share anonymously to the global feed
          </label>
        </div>

        <motion.button 
          whileHover={mood ? { scale: 1.02 } : {}}
          whileTap={mood ? { scale: 0.98 } : {}}
          type="submit"
          disabled={!mood}
          className={`w-full py-5 rounded-2xl font-bold text-white text-lg transition-all shadow-xl ${mood ? 'bg-[var(--color-ok-black)] hover:bg-gray-800 cursor-pointer' : 'bg-gray-300 cursor-not-allowed opacity-70'}`}
        >
          Complete Check-in
        </motion.button>
      </form>
    </motion.div>
  );
}
