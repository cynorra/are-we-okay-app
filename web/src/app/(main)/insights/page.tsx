import { BarChart3 } from "lucide-react";

export default function InsightsPage() {
  return (
    <div className="animate-in fade-in duration-500">
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">Your Insights</h1>
        <p className="text-gray-500">A private view of your emotional journey.</p>
      </div>
      
      <div className="grid md:grid-cols-2 gap-6 mb-6">
        <div className="bg-white p-8 rounded-[2rem] shadow-sm border border-gray-100">
          <div className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-2">Current Streak</div>
          <div className="text-5xl font-bold text-[var(--color-ok-orange)] mb-2">3 <span className="text-2xl text-gray-400">days</span></div>
          <p className="text-sm text-gray-500">You've checked in for 3 days in a row. Keep it up!</p>
        </div>
        
        <div className="bg-white p-8 rounded-[2rem] shadow-sm border border-gray-100">
          <div className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-2">Support Given</div>
          <div className="text-5xl font-bold text-[var(--color-ok-teal)] mb-2">12 <span className="text-2xl text-gray-400">hugs</span></div>
          <p className="text-sm text-gray-500">You've sent support to 12 people this week.</p>
        </div>
      </div>
      
      <div className="bg-white p-8 rounded-[2rem] shadow-sm border border-gray-100 h-64 flex flex-col items-center justify-center text-center">
        <BarChart3 className="w-12 h-12 text-gray-300 mb-4" />
        <h3 className="text-xl font-bold text-gray-700 mb-2">Weekly Mood Chart</h3>
        <p className="text-gray-500">Chart visualization will appear here after a few more check-ins.</p>
      </div>
    </div>
  );
}
