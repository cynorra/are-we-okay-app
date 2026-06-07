"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { signInWithGoogle } from "../../../utils/db";

export default function LoginPage() {
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleGoogleLogin = async () => {
    setError(null);
    setLoading(true);
    
    try {
      const { error: signInError } = await signInWithGoogle();
      if (signInError) {
        setError(signInError);
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
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-[var(--color-ok-black)] mb-2">Welcome back</h1>
        <p className="text-gray-500">Sign in to your Okayness account</p>
      </div>

      {error && (
        <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm font-medium">
          {error}
        </div>
      )}

      <div className="space-y-4">
        <button 
          onClick={handleGoogleLogin}
          disabled={loading}
          className="w-full flex items-center justify-center bg-white hover:bg-gray-50 text-gray-700 font-semibold py-4 px-6 border border-gray-200 rounded-xl transition-all shadow-sm hover:shadow-md cursor-pointer disabled:bg-gray-100 disabled:cursor-not-allowed"
        >
          <svg className="w-5 h-5 mr-3" viewBox="0 0 24 24" width="24" height="24" xmlns="http://www.w3.org/2000/svg">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" fill="#FBBC05"/>
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
          </svg>
          {loading ? "Connecting to Google..." : "Google ile Devam Et"}
        </button>
      </div>

      <p className="text-xs text-center text-gray-400 mt-6 leading-relaxed">
        By continuing, you agree to our terms and zero-judgment community rules.
      </p>
    </motion.div>
  );
}
