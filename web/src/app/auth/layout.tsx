"use client";

import { Heart } from "lucide-react";
import Link from "next/link";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[var(--color-ok-beige)] flex flex-col items-center justify-center p-4">
      <div className="absolute top-6 left-6 md:top-12 md:left-12">
        <Link href="/" className="flex items-center gap-2 text-[var(--color-ok-black)] hover:opacity-80 transition-opacity">
          <Heart className="w-6 h-6 text-[var(--color-ok-orange)]" />
          <span className="font-bold text-xl tracking-tight">Okayness</span>
        </Link>
      </div>
      <div className="w-full max-w-md">
        {children}
      </div>
    </div>
  );
}
