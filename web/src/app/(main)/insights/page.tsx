"use client";

import { useEffect, useState } from "react";
import { BarChart3, Flame, Heart, Smile, Sparkles, TrendingUp } from "lucide-react";
import { motion } from "framer-motion";
import { getUserStats, getWeeklyMoods } from "../../../utils/db";

interface WeeklyMood {
  day: string;
  mood: 'good' | 'bad' | 'unsure' | null;
}

export default function InsightsPage() {
  const [stats, setStats] = useState<{ streak: number; supportGiven: number; moodCounts: { good: number; bad: number; unsure: number } } | null>(null);
  const [weeklyMoods, setWeeklyMoods] = useState<WeeklyMood[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    setLoading(true);
    const userStats = await getUserStats();
    const moods = await getWeeklyMoods();
    setStats(userStats);
    setWeeklyMoods(moods);
    setLoading(false);
  };

  const getMoodConfig = (mood: 'good' | 'bad' | 'unsure' | null) => {
    switch (mood) {
      case 'good':
        return {
          height: "85%",
          bg: "from-[var(--color-ok-teal)] to-[#4FB3C3]",
          shadow: "shadow-[var(--color-ok-teal)]/20",
          emoji: "😎",
          label: "Good"
        };
      case 'bad':
        return {
          height: "40%",
          bg: "from-[var(--color-ok-orange)] to-[#F0855A]",
          shadow: "shadow-[var(--color-ok-orange)]/20",
          emoji: "😔",
          label: "Not Good"
        };
      case 'unsure':
        return {
          height: "60%",
          bg: "from-gray-700 to-gray-400",
          shadow: "shadow-gray-500/10",
          emoji: "🤔",
          label: "Unsure"
        };
      default:
        return {
          height: "15%",
          bg: "from-gray-100 to-gray-200",
          shadow: "shadow-none",
          emoji: "💤",
          label: "None"
        };
    }
  };

  const totalCheckins = stats 
    ? stats.moodCounts.good + stats.moodCounts.bad + stats.moodCounts.unsure 
    : 0;

  const mostCommonMood = stats 
    ? Object.entries(stats.moodCounts).reduce((a, b) => a[1] > b[1] ? a : b)[0]
    : 'none';

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full max-w-4xl mx-auto"
    >
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2 tracking-tight text-[var(--color-ok-black)]">Your Insights</h1>
        <p className="text-gray-500 text-lg">A private view of your emotional journey.</p>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400">
          <div className="w-12 h-12 border-4 border-[var(--color-ok-orange)] border-t-transparent rounded-full animate-spin mb-4"></div>
          <p className="text-lg">Analyzing emotional trends...</p>
        </div>
      ) : (
        <div className="space-y-8">
          {/* STATS OVERVIEW CARDS */}
          <div className="grid md:grid-cols-3 gap-6">
            {/* Streak Card */}
            <motion.div 
              whileHover={{ y: -4 }}
              className="bg-white/80 backdrop-blur-md p-6 rounded-[2.5rem] border border-gray-100/50 shadow-sm flex items-center justify-between"
            >
              <div>
                <div className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-1">Current Streak</div>
                <div className="text-4xl font-bold text-[var(--color-ok-orange)] mb-1">
                  {stats?.streak} <span className="text-lg font-medium text-gray-400">days</span>
                </div>
                <p className="text-xs text-gray-500 leading-tight">Keep checking in daily to maintain your self-reflection streak!</p>
              </div>
              <div className="w-12 h-12 rounded-2xl bg-[var(--color-ok-orange-light)] flex items-center justify-center text-[var(--color-ok-orange)] shadow-inner">
                <Flame className="w-6 h-6 fill-[var(--color-ok-orange)]" />
              </div>
            </motion.div>

            {/* Support Given Card */}
            <motion.div 
              whileHover={{ y: -4 }}
              className="bg-white/80 backdrop-blur-md p-6 rounded-[2.5rem] border border-gray-100/50 shadow-sm flex items-center justify-between"
            >
              <div>
                <div className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-1">Support Given</div>
                <div className="text-4xl font-bold text-[var(--color-ok-teal)] mb-1">
                  {stats?.supportGiven} <span className="text-lg font-medium text-gray-400">times</span>
                </div>
                <p className="text-xs text-gray-500 leading-tight">Total support actions sent to people globally. Kindness returns! 🫂</p>
              </div>
              <div className="w-12 h-12 rounded-2xl bg-[var(--color-ok-teal-light)] flex items-center justify-center text-[var(--color-ok-teal)] shadow-inner">
                <Heart className="w-6 h-6 fill-[var(--color-ok-teal)]" />
              </div>
            </motion.div>

            {/* Total Reflections Card */}
            <motion.div 
              whileHover={{ y: -4 }}
              className="bg-white/80 backdrop-blur-md p-6 rounded-[2.5rem] border border-gray-100/50 shadow-sm flex items-center justify-between"
            >
              <div>
                <div className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-1">Total Logs</div>
                <div className="text-4xl font-bold text-gray-800 mb-1">
                  {totalCheckins} <span className="text-lg font-medium text-gray-400">entries</span>
                </div>
                <p className="text-xs text-gray-500 leading-tight">
                  {totalCheckins > 0 
                    ? `Most frequent state is: ${mostCommonMood === 'good' ? 'Good 😎' : mostCommonMood === 'bad' ? 'Not Good 😔' : 'Unsure 🤔'}`
                    : 'Log your mood to see stats here!'
                  }
                </p>
              </div>
              <div className="w-12 h-12 rounded-2xl bg-gray-50 flex items-center justify-center text-gray-500 shadow-inner">
                <TrendingUp className="w-6 h-6" />
              </div>
            </motion.div>
          </div>

          {/* WEEKLY MOOD CHART (SVG Premium Capsule) */}
          <div className="bg-white/90 backdrop-blur-md p-8 rounded-[2.5rem] border border-gray-100/60 shadow-sm">
            <div className="flex items-center gap-3 mb-8">
              <BarChart3 className="w-6 h-6 text-[var(--color-ok-orange)]" />
              <h2 className="text-2xl font-bold text-gray-900">Weekly Emotional Weather</h2>
            </div>

            {totalCheckins === 0 ? (
              <div className="h-64 flex flex-col items-center justify-center text-center text-gray-400 border border-dashed border-gray-200 rounded-3xl p-6">
                <Smile className="w-12 h-12 mb-3 text-gray-300" />
                <h4 className="font-bold text-gray-700">No Check-in History</h4>
                <p className="text-sm max-w-xs mt-1">Complete check-ins over the week to view your interactive mood chart.</p>
              </div>
            ) : (
              <div>
                {/* Visual SVG capsules */}
                <div className="grid grid-cols-7 gap-3 sm:gap-6 h-64 items-end px-2 mb-6 select-none">
                  {weeklyMoods.map((w, index) => {
                    const config = getMoodConfig(w.mood);
                    return (
                      <div key={index} className="h-full flex flex-col justify-end items-center group relative">
                        {/* Tooltip on Hover */}
                        <div className="absolute top-0 transform -translate-y-8 bg-gray-900 text-white text-[10px] sm:text-xs font-bold px-2 py-1 rounded-md opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap shadow-md z-10">
                          {config.label}
                        </div>

                        {/* Capsule Bar */}
                        <motion.div
                          initial={{ height: 0 }}
                          animate={{ height: config.height }}
                          transition={{ type: "spring", stiffness: 100, damping: 15, delay: index * 0.05 }}
                          className={`w-full max-w-[42px] bg-gradient-to-t ${config.bg} rounded-full flex flex-col justify-end items-center pb-2 shadow-sm ${config.shadow} transition-all hover:scale-105 border border-white/25`}
                        >
                          <span className="text-lg sm:text-xl drop-shadow-sm select-none">{config.emoji}</span>
                        </motion.div>
                      </div>
                    );
                  })}
                </div>

                {/* Days Label Row */}
                <div className="grid grid-cols-7 gap-3 sm:gap-6 text-center border-t border-gray-100 pt-4">
                  {weeklyMoods.map((w, index) => (
                    <div key={index} className="text-xs sm:text-sm font-bold text-gray-400">
                      {w.day}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* MOOD DISTRIBUTION & TIPS */}
          {totalCheckins > 0 && (
            <div className="grid md:grid-cols-2 gap-6">
              {/* Distribution */}
              <div className="bg-white/80 backdrop-blur-md p-6 rounded-[2.5rem] border border-gray-100/50 shadow-sm">
                <h3 className="text-xl font-bold mb-4 text-gray-900">Feelings Breakdown</h3>
                <div className="space-y-4">
                  {/* Good progress bar */}
                  <div>
                    <div className="flex justify-between text-sm font-semibold text-gray-600 mb-1.5">
                      <span className="flex items-center gap-1.5">😎 Good</span>
                      <span>{stats?.moodCounts.good} logs ({Math.round((stats?.moodCounts.good || 0) / totalCheckins * 100)}%)</span>
                    </div>
                    <div className="w-full bg-gray-100 h-3 rounded-full overflow-hidden">
                      <div className="bg-[var(--color-ok-teal)] h-full rounded-full" style={{ width: `${((stats?.moodCounts.good || 0) / totalCheckins * 100)}%` }}></div>
                    </div>
                  </div>

                  {/* Bad progress bar */}
                  <div>
                    <div className="flex justify-between text-sm font-semibold text-gray-600 mb-1.5">
                      <span className="flex items-center gap-1.5">😔 Not Good</span>
                      <span>{stats?.moodCounts.bad} logs ({Math.round((stats?.moodCounts.bad || 0) / totalCheckins * 100)}%)</span>
                    </div>
                    <div className="w-full bg-gray-100 h-3 rounded-full overflow-hidden">
                      <div className="bg-[var(--color-ok-orange)] h-full rounded-full" style={{ width: `${((stats?.moodCounts.bad || 0) / totalCheckins * 100)}%` }}></div>
                    </div>
                  </div>

                  {/* Unsure progress bar */}
                  <div>
                    <div className="flex justify-between text-sm font-semibold text-gray-600 mb-1.5">
                      <span className="flex items-center gap-1.5">🤔 Unsure</span>
                      <span>{stats?.moodCounts.unsure} logs ({Math.round((stats?.moodCounts.unsure || 0) / totalCheckins * 100)}%)</span>
                    </div>
                    <div className="w-full bg-gray-100 h-3 rounded-full overflow-hidden">
                      <div className="bg-gray-700 h-full rounded-full" style={{ width: `${((stats?.moodCounts.unsure || 0) / totalCheckins * 100)}%` }}></div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Wellbeing suggestion card */}
              <div className="bg-[var(--color-ok-black)] text-white p-6 rounded-[2.5rem] shadow-xl flex flex-col justify-between">
                <div>
                  <div className="flex items-center gap-2 text-[var(--color-ok-orange)] text-sm font-bold uppercase tracking-wider mb-2">
                    <Sparkles className="w-4 h-4 fill-[var(--color-ok-orange)]" /> Mindfulness tip
                  </div>
                  <h3 className="text-xl font-bold mb-3">Embrace the Unsure</h3>
                  <p className="text-white/80 text-sm leading-relaxed">
                    Some days don't fit into clean boxes. Having 'unsure' days is a completely natural part of the emotional cycle. Pausing to log uncertainty is just as meaningful as celebrating joy or acknowledging struggle.
                  </p>
                </div>
                <div className="mt-6 text-xs text-white/50 border-t border-white/10 pt-4 flex items-center justify-between">
                  <span>Okayness Insights Engine</span>
                  <span>🌱 Grow together</span>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </motion.div>
  );
}
