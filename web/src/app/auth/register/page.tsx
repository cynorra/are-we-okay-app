"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function RegisterPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/auth/login");
  }, [router]);

  return (
    <div className="flex items-center justify-center p-8">
      <div className="w-8 h-8 border-4 border-[var(--color-ok-orange)] border-t-transparent rounded-full animate-spin"></div>
    </div>
  );
}
