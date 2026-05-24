"use client";

import { useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";

export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    // Supabase auth logic will go here
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white p-8 rounded-[2rem] shadow-xl border border-gray-100"
    >
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-[var(--color-ok-black)] mb-2">Join Okayness</h1>
        <p className="text-gray-500">Create your anonymous safe space</p>
      </div>

      <form onSubmit={handleRegister} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input 
            type="email" 
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] bg-[var(--color-ok-off-white)]"
            placeholder="you@example.com"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
          <input 
            type="password" 
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] bg-[var(--color-ok-off-white)]"
            placeholder="••••••••"
          />
        </div>
        
        <button 
          type="submit"
          className="w-full bg-[var(--color-ok-orange)] hover:bg-[var(--color-ok-orange-shade)] text-white py-4 rounded-xl font-medium transition-colors mt-2"
        >
          Sign Up
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
