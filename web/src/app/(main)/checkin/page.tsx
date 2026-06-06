"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import Link from "next/link";
import { createCheckin } from "../../../utils/db";

export default function CheckinPage() {
  const [mood, setMood] = useState<'good' | 'bad' | 'unsure' | null>(null);
  const [note, setNote] = useState("");
  const [isPublic, setIsPublic] = useState(true);
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!mood) return;

    setError(null);
    setLoading(true);

    try {
      const { checkin, error: checkinError } = await createCheckin(mood, note, isPublic);
      if (checkinError) {
        setError(checkinError);
      } else {
        setSubmitted(true);
        // Clear note on success
        setNote("");
      }
    } catch (err) {
      setError("Failed to save check-in. Please try again.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: "easeOut" }}
      className="w-full max-w-2xl mx-auto"
    >
      <AnimatePresence mode="wait">
        {!submitted ? (
          <motion.div
            key="checkin-form"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <h1 className="text-4xl font-bold mb-2 tracking-tight text-[var(--color-ok-black)]">Daily Check-in</h1>
            <p className="text-gray-500 mb-8 text-lg">Take a moment to reflect. How are you really doing today?</p>

            {error && (
              <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm font-medium">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="bg-white/80 backdrop-blur-xl p-8 rounded-[2.5rem] shadow-xl border border-gray-100/50">
              <div className="mb-8">
                <h2 className="text-2xl font-bold mb-6">I am feeling...</h2>
                <div className="grid grid-cols-3 gap-4">
                  <motion.button 
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    type="button"
                    onClick={() => setMood('good')}
                    disabled={loading}
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
                    disabled={loading}
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
                    disabled={loading}
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
                  disabled={loading}
                  className="w-full p-5 rounded-2xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] resize-none bg-gray-50/50 transition-all text-lg text-black"
                />
              </motion.div>

              <div className="flex items-center gap-4 mb-8 p-4 bg-gray-50 rounded-2xl border border-gray-100">
                <input 
                  type="checkbox" 
                  id="public" 
                  checked={isPublic}
                  onChange={(e) => setIsPublic(e.target.checked)}
                  disabled={loading}
                  className="w-6 h-6 accent-[var(--color-ok-orange)] cursor-pointer rounded"
                />
                <label htmlFor="public" className="text-gray-700 font-medium cursor-pointer select-none">
                  Share anonymously to the global feed
                </label>
              </div>

              <motion.button 
                whileHover={mood && !loading ? { scale: 1.02 } : {}}
                whileTap={mood && !loading ? { scale: 0.98 } : {}}
                type="submit"
                disabled={!mood || loading}
                className={`w-full py-5 rounded-2xl font-bold text-white text-lg transition-all shadow-xl ${mood && !loading ? 'bg-[var(--color-ok-black)] hover:bg-gray-800 cursor-pointer' : 'bg-gray-300 cursor-not-allowed opacity-70'}`}
              >
                {loading ? "Saving Check-in..." : "Complete Check-in"}
              </motion.button>
            </form>
          </motion.div>
        ) : (
          <motion.div
            key="checkin-success"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            className="bg-white/90 backdrop-blur-xl p-10 rounded-[2.5rem] shadow-xl border border-gray-100/50 text-center flex flex-col items-center"
          >
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: "spring", stiffness: 200, damping: 15 }}
              className="w-24 h-24 rounded-full bg-[var(--color-ok-teal-light)] border border-[var(--color-ok-teal)]/20 flex items-center justify-center text-5xl mb-6 shadow-inner"
            >
              🫂
            </motion.div>
            
            <h1 className="text-3xl font-bold mb-4 text-[var(--color-ok-black)]">Check-in Saved!</h1>
            <p className="text-gray-500 mb-8 max-w-md text-lg leading-relaxed">
              Thank you for taking a moment for yourself. Your feelings are completely valid. You are not walking this path alone.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 w-full justify-center">
              <Link 
                href="/feed"
                className="bg-[var(--color-ok-black)] hover:bg-gray-800 text-white px-8 py-4 rounded-2xl font-semibold transition-all hover:scale-105 active:scale-95 text-center shadow-lg"
              >
                Go to Global Feed
              </Link>
              <Link 
                href="/insights"
                className="bg-[var(--color-ok-orange-light)] border border-[var(--color-ok-orange)]/10 text-[var(--color-ok-orange-shade)] px-8 py-4 rounded-2xl font-semibold transition-all hover:scale-105 active:scale-95 text-center shadow-sm"
              >
                View Your Insights
              </Link>
            </div>

            <button 
              onClick={() => {
                setSubmitted(false);
                setMood(null);
              }}
              className="mt-8 text-sm text-gray-400 hover:text-gray-600 transition-colors underline cursor-pointer"
            >
              Log another check-in
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
