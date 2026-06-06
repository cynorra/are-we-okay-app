"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { signUp } from "../../../utils/db";

const AVATARS = ['🌙', '🪐', '🧸', '🐱', '🦊', '🍀', '🌊', '🌸', '⚡', '☕'];

export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [username, setUsername] = useState("");
  const [avatar, setAvatar] = useState("🌙");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    if (username.length < 3) {
      setError("Username must be at least 3 characters long.");
      setLoading(false);
      return;
    }

    try {
      const { user, error: signUpError } = await signUp(email, password, username, avatar);
      if (signUpError) {
        setError(signUpError);
      } else if (user) {
        router.push("/checkin");
        router.refresh();
      }
    } catch (err: any) {
      setError("An unexpected error occurred. Please try again.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white p-8 rounded-[2rem] shadow-xl border border-gray-100 max-w-md w-full mx-auto"
    >
      <div className="text-center mb-6">
        <h1 className="text-3xl font-bold text-[var(--color-ok-black)] mb-2">Join Okayness</h1>
        <p className="text-gray-500">Create your anonymous safe space</p>
      </div>

      {error && (
        <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm font-medium">
          {error}
        </div>
      )}

      <form onSubmit={handleRegister} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
          <input 
            type="text" 
            required
            value={username}
            onChange={(e) => setUsername(e.target.value.replace(/[^a-zA-Z0-9_]/g, ""))}
            className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] bg-[var(--color-ok-off-white)] text-black"
            placeholder="username_here"
            disabled={loading}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input 
            type="email" 
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] bg-[var(--color-ok-off-white)] text-black"
            placeholder="you@example.com"
            disabled={loading}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
          <input 
            type="password" 
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] bg-[var(--color-ok-off-white)] text-black"
            placeholder="••••••••"
            disabled={loading}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Choose Avatar Emoji</label>
          <div className="flex flex-wrap gap-2 justify-center py-2 bg-gray-50 rounded-2xl border border-gray-100">
            {AVATARS.map((emoji) => (
              <button
                key={emoji}
                type="button"
                onClick={() => setAvatar(emoji)}
                className={`w-10 h-10 rounded-full flex items-center justify-center text-xl transition-all cursor-pointer hover:scale-110 active:scale-95 ${avatar === emoji ? 'bg-white border-2 border-[var(--color-ok-orange)] shadow-md' : 'opacity-60 hover:opacity-100'}`}
              >
                {emoji}
              </button>
            ))}
          </div>
        </div>
        
        <button 
          type="submit"
          disabled={loading}
          className="w-full bg-[var(--color-ok-orange)] hover:bg-[var(--color-ok-orange-shade)] disabled:bg-gray-400 disabled:cursor-not-allowed text-white py-4 rounded-xl font-medium transition-colors mt-2 cursor-pointer shadow-md"
        >
          {loading ? "Creating Account..." : "Sign Up"}
        </button>
        <p className="text-xs text-center text-gray-400 mt-2">
          By signing up, you agree to our zero-judgment community rules.
        </p>
      </form>

      <div className="mt-6 text-center text-sm text-gray-500">
        Already have an account?{" "}
        <Link href="/auth/login" className="text-[var(--color-ok-black)] font-bold hover:underline">
          Sign In
        </Link>
      </div>
    </motion.div>
  );
}

