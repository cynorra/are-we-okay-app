"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { User, Palette, Check, ShieldAlert, Sparkles } from "lucide-react";
import { getCurrentUser, updateProfile, UserProfile } from "../../../utils/db";

interface DesignTheme {
  id: string;
  name: string;
  mood: string;
  displayFont: string;
  bgHex: string;
  fgHex: string;
  accentHex: string;
  surfaceHex: string;
  isDark: boolean;
  posture: string;
}

const THEMES: DesignTheme[] = [
  {
    id: "warm-soft",
    name: "Warm Soft",
    mood: "Soft, organic, and approachable. Rounded shapes and earth-inspired tones that evoke safety.",
    displayFont: "Quicksand",
    bgHex: "#FAF3EC",
    surfaceHex: "#FFFFFF",
    fgHex: "#402E2A",
    accentHex: "#E07A5F",
    isDark: false,
    posture: "Terracotta colors, 24px rounded cards, glowing soft shadows."
  },
  {
    id: "editorial-monocle",
    name: "Editorial Monocle",
    mood: "Print-magazine feel. Generous whitespace, classic serif typography, and a single striking color accent.",
    displayFont: "Georgia",
    bgHex: "#FCFAF2",
    surfaceHex: "#FFFFFF",
    fgHex: "#1F1E1A",
    accentHex: "#B84A39",
    isDark: false,
    posture: "Sharp boxy corners, 0px border-radius, clean paper borders."
  },
  {
    id: "modern-minimal",
    name: "Modern Minimal",
    mood: "Quiet, precise, software-native. Crisp gray foundations where content is the only focus.",
    displayFont: "SF Pro Display",
    bgHex: "#FAF9F6",
    surfaceHex: "#FFFFFF",
    fgHex: "#18181B",
    accentHex: "#2563EB",
    isDark: false,
    posture: "Hairline borders, compact 10px rounded corners, pure cobalt blue."
  },
  {
    id: "tech-utility",
    name: "Tech Utility",
    mood: "Cold, functional, terminal-like. Monospaced display elements, pixel grid lines, and high structural density.",
    displayFont: "Courier New",
    bgHex: "#F1F5F9",
    surfaceHex: "#FFFFFF",
    fgHex: "#0F172A",
    accentHex: "#4F46E5",
    isDark: false,
    posture: "Indicated status lights, 4px sharp corners, mono labels."
  },
  {
    id: "brutalist-experimental",
    name: "Brutalist Experimental",
    mood: "Daring and high-contrast. Thick black borders, raw shapes, zero curves, and eye-catching neon orange highlights.",
    displayFont: "Arial Black",
    bgHex: "#FFFBEB",
    surfaceHex: "#FFFFFF",
    fgHex: "#000000",
    accentHex: "#F97316",
    isDark: false,
    posture: "Hard black shadow offsets, boxy geometry, 2.5px heavy outlines."
  }
];

const EMOJIS = ["😊", "💻", "🎓", "⛵", "🌙", "🌸", "⚡", "🍀", "🧠", "☕", "🎮", "👾", "🎨", "🍜", "🥑", "🦊", "🌈", "🔥"];

export default function SettingsPage() {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [username, setUsername] = useState("");
  const [selectedEmoji, setSelectedEmoji] = useState("😊");
  const [activeTheme, setActiveTheme] = useState("warm-soft");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  useEffect(() => {
    async function loadData() {
      const currentUser = await getCurrentUser();
      if (currentUser) {
        setUser(currentUser);
        setUsername(currentUser.username);
        setSelectedEmoji(currentUser.avatar_emoji || "😊");
      }
      
      const savedTheme = localStorage.getItem("ok_theme") || "warm-soft";
      setActiveTheme(savedTheme);
      setLoading(false);
    }
    loadData();
  }, []);

  const handleThemeChange = (themeId: string) => {
    setActiveTheme(themeId);
    localStorage.setItem("ok_theme", themeId);
    document.documentElement.setAttribute("data-theme", themeId);

    // Call dynamic Android Bridge if available
    const theme = THEMES.find((t) => t.id === themeId);
    if (theme && typeof window !== "undefined" && (window as any).AndroidThemeBridge) {
      try {
        (window as any).AndroidThemeBridge.setThemeColor(theme.bgHex, theme.isDark);
      } catch (e) {
        console.error("Failed to notify Android Theme Bridge", e);
      }
    }
  };

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim()) return;

    setSaving(true);
    setMessage(null);

    const { user: updatedUser, error } = await updateProfile(username.trim(), selectedEmoji);

    if (error) {
      setMessage({ text: error, type: "error" });
    } else if (updatedUser) {
      setUser(updatedUser);
      setMessage({ text: "Profile updated successfully! ✨", type: "success" });
    }
    setSaving(false);
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-gray-400">
        <div className="w-12 h-12 border-4 border-[var(--color-ok-orange)] border-t-transparent rounded-full animate-spin mb-4"></div>
        <p className="text-lg font-medium">Loading settings...</p>
      </div>
    );
  }

  return (
    <div className="w-full max-w-4xl mx-auto space-y-12">
      <div>
        <h1 className="text-4xl font-bold mb-2 tracking-tight text-[var(--color-ok-black)] font-display">Settings</h1>
        <p className="text-gray-500 text-lg">Customize your wellbeing experience and visual styles.</p>
      </div>

      {message && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className={`p-4 rounded-2xl border ${
            message.type === "success"
              ? "bg-green-50 border-green-200 text-green-700"
              : "bg-red-50 border-red-200 text-red-700"
          } text-sm font-semibold flex items-center gap-2`}
        >
          <Sparkles className="w-5 h-5" />
          {message.text}
        </motion.div>
      )}

      <div className="grid md:grid-cols-3 gap-8">
        {/* Profile Card */}
        <div className="md:col-span-1">
          <div className="bg-white/80 backdrop-blur-md p-6 rounded-[2.5rem] premium-card h-full flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 bg-[var(--color-ok-orange-light)] rounded-xl">
                  <User className="w-5 h-5 text-[var(--color-ok-orange)]" />
                </div>
                <h2 className="text-xl font-bold text-gray-900">Profile</h2>
              </div>

              <form onSubmit={handleProfileSubmit} className="space-y-6">
                <div className="flex flex-col items-center mb-6">
                  <div className="w-20 h-20 rounded-full bg-gray-50 border border-gray-100 flex items-center justify-center text-4xl shadow-inner mb-2">
                    {selectedEmoji}
                  </div>
                  <span className="text-xs text-gray-400 font-medium">Your Avatar</span>
                </div>

                <div className="space-y-2">
                  <label htmlFor="username" className="text-sm font-semibold text-gray-700">Username</label>
                  <input
                    type="text"
                    id="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                    placeholder="Enter username"
                    className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] bg-gray-50/50 transition-all font-semibold text-black"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-gray-700">Choose Avatar Emoji</label>
                  <div className="grid grid-cols-6 gap-2">
                    {EMOJIS.map((emoji) => (
                      <button
                        key={emoji}
                        type="button"
                        onClick={() => setSelectedEmoji(emoji)}
                        className={`w-9 h-9 rounded-lg flex items-center justify-center text-lg hover:bg-gray-100 transition-colors cursor-pointer ${
                          selectedEmoji === emoji ? "bg-[var(--color-ok-orange-light)] border border-[var(--color-ok-orange)]/30 scale-110" : ""
                        }`}
                      >
                        {emoji}
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={saving}
                  className="w-full bg-[var(--color-ok-black)] hover:bg-gray-800 text-white py-3.5 rounded-xl font-bold transition-all shadow-md cursor-pointer"
                >
                  {saving ? "Saving Changes..." : "Save Profile"}
                </button>
              </form>
            </div>
          </div>
        </div>

        {/* Theme Card */}
        <div className="md:col-span-2">
          <div className="bg-white/80 backdrop-blur-md p-6 rounded-[2.5rem] premium-card">
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-[var(--color-ok-teal-light)] rounded-xl">
                <Palette className="w-5 h-5 text-[var(--color-ok-teal)]" />
              </div>
              <h2 className="text-xl font-bold text-gray-900">Design System (Open Design)</h2>
            </div>

            <p className="text-gray-500 text-sm mb-6 leading-relaxed">
              Choose a design school from <code className="bg-gray-100 px-1.5 py-0.5 rounded font-mono text-xs">open-design</code>. 
              The layout density, corner roundness, shadows, typography, and accent colors will adjust dynamically.
            </p>

            <div className="space-y-4">
              {THEMES.map((theme) => {
                const isActive = activeTheme === theme.id;
                return (
                  <button
                    key={theme.id}
                    onClick={() => handleThemeChange(theme.id)}
                    className={`w-full text-left p-5 rounded-3xl border transition-all duration-300 flex items-start justify-between cursor-pointer ${
                      isActive
                        ? "border-[var(--color-ok-orange)] bg-[var(--color-ok-orange-light)]/20 shadow-md"
                        : "border-gray-100 bg-gray-50/50 hover:bg-white hover:border-gray-200"
                    }`}
                  >
                    <div className="space-y-2 max-w-[80%]">
                      <div className="flex items-center gap-2">
                        <span
                          className="font-bold text-lg text-gray-900"
                          style={{ fontFamily: theme.id === "editorial-monocle" ? "Georgia" : theme.id === "tech-utility" ? "Courier New" : "inherit" }}
                        >
                          {theme.name}
                        </span>
                        {isActive && (
                          <span className="text-[10px] bg-[var(--color-ok-orange)] text-white px-2 py-0.5 rounded-full font-bold uppercase tracking-wider flex items-center gap-0.5">
                            <Check className="w-3 h-3" /> Active
                          </span>
                        )}
                      </div>
                      <p className="text-gray-500 text-sm leading-relaxed">{theme.mood}</p>
                      <div className="text-xs text-gray-400 font-semibold flex items-center gap-1.5">
                        <span className="bg-white/80 px-2 py-0.5 rounded-md border border-gray-100">{theme.posture}</span>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-3 justify-between h-full">
                      {/* Color Palette Preview Swatches */}
                      <div className="flex items-center gap-1 bg-white p-1 rounded-xl shadow-inner border border-gray-100">
                        <span className="w-4 h-4 rounded-md inline-block border border-gray-100" style={{ backgroundColor: theme.bgHex }} title="Background"></span>
                        <span className="w-4 h-4 rounded-md inline-block border border-gray-100" style={{ backgroundColor: theme.surfaceHex }} title="Surface"></span>
                        <span className="w-4 h-4 rounded-md inline-block" style={{ backgroundColor: theme.fgHex }} title="Text"></span>
                        <span className="w-4 h-4 rounded-md inline-block" style={{ backgroundColor: theme.accentHex }} title="Accent"></span>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
