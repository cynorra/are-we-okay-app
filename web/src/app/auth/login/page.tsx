"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { signIn } from "../../../utils/db";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    
    try {
      const { user, error: signInError } = await signIn(email, password);
      if (signInError) {
        setError(signInError);
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
      className="bg-white p-8 rounded-[2rem] shadow-xl border border-gray-100"
    >
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-[var(--color-ok-black)] mb-2">Welcome back</h1>
        <p className="text-gray-500">Sign in to your Okayness account</p>
      </div>

      {error && (
        <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm font-medium">
          {error}
        </div>
      )}

      <form onSubmit={handleLogin} className="space-y-4">
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
        
        <button 
          type="submit"
          disabled={loading}
          className="w-full bg-[var(--color-ok-black)] hover:bg-gray-800 disabled:bg-gray-400 disabled:cursor-not-allowed text-white py-4 rounded-xl font-medium transition-colors mt-2 cursor-pointer"
        >
          {loading ? "Signing In..." : "Sign In"}
        </button>
      </form>

      <div className="mt-8 text-center text-sm text-gray-500">
        Don't have an account?{" "}
        <Link href="/auth/register" className="text-[var(--color-ok-orange)] font-bold hover:underline">
          Create one
        </Link>
      </div>
    </motion.div>
  );
}

