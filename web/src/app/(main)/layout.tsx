"use client";

import { Heart, Compass, CheckCircle, Users, Map as MapIcon, BarChart3, Settings, LogOut } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { signOut } from "../../utils/db";

export default function MainLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();

  const handleSignOut = async () => {
    await signOut();
    router.push("/");
    router.refresh();
  };


  const navItems = [
    { name: "Check-in", href: "/checkin", icon: CheckCircle },
    { name: "Feed", href: "/feed", icon: Compass },
    { name: "Friends", href: "/friends", icon: Users },
    { name: "Mood Map", href: "/map", icon: MapIcon },
    { name: "Insights", href: "/insights", icon: BarChart3 },
  ];

  return (
    <div className="min-h-screen text-[var(--color-ok-black)] flex flex-col md:flex-row relative">
      {/* Dynamic Background */}
      <div className="fixed inset-0 z-[-1] bg-gradient-to-br from-[var(--color-ok-beige)] via-[#FAF9F5] to-[var(--color-ok-beige)]" />
      
      {/* Sidebar Navigation */}
      <nav className="w-full md:w-72 glass-panel md:min-h-screen flex flex-col p-6 md:p-8 md:sticky md:top-0 z-40 border-r border-white/40">
        <Link href="/" className="flex items-center gap-3 text-[var(--color-ok-black)] hover:opacity-80 transition-opacity mb-12 group">
          <div className="p-2 bg-[var(--color-ok-orange-light)] rounded-xl group-hover:scale-105 transition-transform">
            <Heart className="w-6 h-6 text-[var(--color-ok-orange)]" />
          </div>
          <span className="font-bold text-2xl tracking-tight">Okayness</span>
        </Link>

        <div className="flex-1 flex flex-col gap-3">
          <div className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 px-4">Menu</div>
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            return (
              <Link key={item.name} href={item.href} className="relative block group">
                {isActive && (
                  <motion.div 
                    layoutId="activeNavIndicator"
                    className="absolute inset-0 bg-white rounded-2xl shadow-sm border border-gray-100"
                    initial={false}
                    transition={{ type: "spring", stiffness: 300, damping: 30 }}
                  />
                )}
                <div className={`relative flex items-center gap-4 px-4 py-3.5 rounded-2xl transition-colors font-semibold z-10 ${isActive ? 'text-[var(--color-ok-orange)]' : 'text-gray-600 hover:bg-white/50 hover:text-gray-900'}`}>
                  <item.icon className={`w-5 h-5 ${isActive ? 'text-[var(--color-ok-orange)]' : 'text-gray-400 group-hover:text-gray-600 transition-colors'}`} />
                  {item.name}
                </div>
              </Link>
            );
          })}
        </div>

        <div className="mt-auto pt-8 flex flex-col gap-3">
          <Link href="/settings" className="flex items-center gap-4 px-4 py-3.5 rounded-2xl hover:bg-white/50 transition-colors text-gray-600 font-semibold group">
            <Settings className="w-5 h-5 text-gray-400 group-hover:text-gray-600 transition-colors" />
            Settings
          </Link>
          <button 
            onClick={handleSignOut}
            className="flex items-center gap-4 px-4 py-3.5 rounded-2xl hover:bg-red-50/80 text-red-600 transition-colors font-semibold cursor-pointer w-full text-left"
          >
            <LogOut className="w-5 h-5 text-red-400" />
            Sign Out
          </button>
        </div>
      </nav>

      {/* Main Content Area */}
      <main className="flex-1 p-6 md:p-12 lg:p-16 overflow-y-auto">
        <div className="max-w-5xl mx-auto h-full relative">
          {children}
        </div>
      </main>
    </div>
  );
}
