"use client";

import { useState } from "react";
import { Globe, MapPin, Users, Heart, ArrowRight } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface RegionData {
  id: string;
  name: string;
  coords: { x: string; y: string }; // Position in percent for responsive overlay
  mood: 'good' | 'bad' | 'unsure';
  checkins: number;
  breakdown: { good: number; bad: number; unsure: number };
  popularEmoji: string;
  weatherMessage: string;
}

const REGIONS: RegionData[] = [
  {
    id: "na",
    name: "North America",
    coords: { x: "20%", y: "30%" },
    mood: "good",
    checkins: 12450,
    breakdown: { good: 55, bad: 30, unsure: 15 },
    popularEmoji: "😎",
    weatherMessage: "Sunny with high optimism. Relieved smiles reported across major hubs."
  },
  {
    id: "sa",
    name: "South America",
    coords: { x: "32%", y: "70%" },
    mood: "good",
    checkins: 8940,
    breakdown: { good: 60, bad: 25, unsure: 15 },
    popularEmoji: "💃",
    weatherMessage: "Clear skies and high support. Community connections are keeping energy levels high."
  },
  {
    id: "eu",
    name: "Europe",
    coords: { x: "50%", y: "25%" },
    mood: "unsure",
    checkins: 15420,
    breakdown: { good: 40, bad: 35, unsure: 25 },
    popularEmoji: "🤔",
    weatherMessage: "Overcast with mild introspection. Many users report feeling a bit uncertain about work pacing."
  },
  {
    id: "af",
    name: "Africa",
    coords: { x: "53%", y: "58%" },
    mood: "good",
    checkins: 7420,
    breakdown: { good: 68, bad: 20, unsure: 12 },
    popularEmoji: "🌱",
    weatherMessage: "Breezy and bright. High ratings for community resilience and mutual support."
  },
  {
    id: "me",
    name: "Middle East",
    coords: { x: "62%", y: "42%" },
    mood: "unsure",
    checkins: 6310,
    breakdown: { good: 45, bad: 35, unsure: 20 },
    popularEmoji: "☕",
    weatherMessage: "Warm and cozy. Calm evening check-ins are balancing out busy workdays."
  },
  {
    id: "as",
    name: "Asia",
    coords: { x: "78%", y: "35%" },
    mood: "bad",
    checkins: 22480,
    breakdown: { good: 35, bad: 48, unsure: 17 },
    popularEmoji: "😔",
    weatherMessage: "Rainy with signs of work stress. A global call for virtual hugs is active here."
  },
  {
    id: "au",
    name: "Australia",
    coords: { x: "85%", y: "78%" },
    mood: "good",
    checkins: 4320,
    breakdown: { good: 72, bad: 18, unsure: 10 },
    popularEmoji: "🌊",
    weatherMessage: "Perfect beach weather. High feelings of gratitude and peace reported near coastlines."
  }
];

export default function MapPage() {
  const [selectedRegion, setSelectedRegion] = useState<RegionData>(REGIONS[0]);

  const getMoodColor = (mood: 'good' | 'bad' | 'unsure') => {
    if (mood === 'good') return 'bg-[var(--color-ok-teal)]';
    if (mood === 'bad') return 'bg-[var(--color-ok-orange)]';
    return 'bg-gray-700';
  };

  const getMoodPulse = (mood: 'good' | 'bad' | 'unsure') => {
    if (mood === 'good') return 'shadow-[0_0_15px_rgba(31,122,140,0.5)] border-[var(--color-ok-teal)]';
    if (mood === 'bad') return 'shadow-[0_0_15px_rgba(232,93,42,0.5)] border-[var(--color-ok-orange)]';
    return 'shadow-[0_0_15px_rgba(50,50,50,0.3)] border-gray-700';
  };

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full max-w-5xl mx-auto"
    >
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2 tracking-tight text-[var(--color-ok-black)]">Global Mood Map</h1>
        <p className="text-gray-500 text-lg">See how the world is feeling right now, completely anonymized.</p>
      </div>

      <div className="grid lg:grid-cols-3 gap-8 items-start">
        {/* MAP PANEL (Takes 2 columns on large screens) */}
        <div className="lg:col-span-2 bg-white/90 backdrop-blur-md rounded-[2.5rem] shadow-sm border border-gray-100/50 p-6 overflow-hidden relative min-h-[380px] sm:min-h-[460px] flex items-center justify-center">
          
          {/* Decorative Grid/Lines for stylized Map */}
          <div className="absolute inset-0 opacity-[0.03] bg-[radial-gradient(circle_at_center,_var(--color-ok-black)_1px,_transparent_1px)] bg-[size:24px_24px] pointer-events-none" />
          
          {/* Stylized Minimal World Map SVG in background */}
          <svg className="w-full h-full max-w-lg opacity-10 absolute pointer-events-none" viewBox="0 0 1000 500" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M150 120H250V180H150V120ZM300 280H400V350H300V280ZM480 100H580V160H480V100ZM500 240H580V320H500V240ZM700 120H850V250H700V120ZM780 320H880V380H780V320Z" stroke="currentColor" strokeWidth="8" strokeDasharray="10 10" />
            <circle cx="200" cy="150" r="100" stroke="currentColor" strokeWidth="2" />
            <circle cx="350" cy="315" r="90" stroke="currentColor" strokeWidth="2" />
            <circle cx="530" cy="130" r="80" stroke="currentColor" strokeWidth="2" />
            <circle cx="540" cy="280" r="110" stroke="currentColor" strokeWidth="2" />
            <circle cx="775" cy="185" r="120" stroke="currentColor" strokeWidth="2" />
            <circle cx="830" cy="350" r="60" stroke="currentColor" strokeWidth="2" />
          </svg>

          {/* Interactive pulsed pins */}
          <div className="w-full h-full absolute inset-0">
            {REGIONS.map((region) => {
              const isSelected = selectedRegion.id === region.id;
              const color = getMoodColor(region.mood);
              const pulseBorder = getMoodPulse(region.mood);
              
              return (
                <button
                  key={region.id}
                  onClick={() => setSelectedRegion(region)}
                  className="absolute group transition-transform hover:scale-110 active:scale-95 cursor-pointer z-10"
                  style={{ left: region.coords.x, top: region.coords.y }}
                >
                  {/* Glowing Pulse */}
                  <span className="absolute -inset-2 rounded-full animate-ping opacity-35 bg-inherit pointer-events-none" />
                  
                  {/* Pin Circle */}
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center border-2 bg-white ${isSelected ? 'scale-125 border-[var(--color-ok-orange)] shadow-md' : 'border-white'} ${pulseBorder} transition-transform`}>
                    <MapPin className={`w-4 h-4 ${isSelected ? 'text-[var(--color-ok-orange)]' : 'text-gray-600'} fill-current`} />
                  </div>

                  {/* Label (Desktop tooltip on hover) */}
                  <div className="absolute top-10 left-1/2 -translate-x-1/2 bg-[var(--color-ok-black)] text-white text-[10px] sm:text-xs font-bold px-2 py-1.5 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap shadow-md">
                    {region.name}
                  </div>
                </button>
              );
            })}
          </div>

          <div className="absolute bottom-6 left-6 text-xs text-gray-400 font-bold flex items-center gap-1.5 bg-gray-50 border border-gray-100 px-3 py-1.5 rounded-full select-none">
            <Globe className="w-3.5 h-3.5 animate-spin-slow" /> Tap pins to explore regional emotional weather
          </div>
        </div>

        {/* DETAILS CARD (Takes 1 column) */}
        <div className="w-full">
          <AnimatePresence mode="wait">
            <motion.div
              key={selectedRegion.id}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
              className="bg-white/90 backdrop-blur-md rounded-[2.5rem] border border-gray-100/60 p-8 shadow-sm flex flex-col justify-between h-full min-h-[380px] sm:min-h-[460px]"
            >
              <div>
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <h2 className="text-2xl font-bold text-gray-900">{selectedRegion.name}</h2>
                    <span className="text-xs text-gray-400 font-bold uppercase tracking-wider flex items-center gap-1 mt-1">
                      <Users className="w-3.5 h-3.5" /> {selectedRegion.checkins.toLocaleString()} logs today
                    </span>
                  </div>
                  <div className="w-14 h-14 rounded-2xl bg-gray-50 border border-gray-100 flex items-center justify-center text-3xl shadow-inner select-none">
                    {selectedRegion.popularEmoji}
                  </div>
                </div>

                <div className="bg-gray-50 border border-gray-100 p-5 rounded-2xl mb-6 shadow-inner">
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-1.5">Climate Summary</h4>
                  <p className="text-gray-700 italic text-sm leading-relaxed">
                    "{selectedRegion.weatherMessage}"
                  </p>
                </div>

                {/* Regional Breakdown progress bars */}
                <div className="space-y-4 mb-4">
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-1">Emotion Share</h4>
                  
                  {/* Good */}
                  <div>
                    <div className="flex justify-between text-xs font-bold text-gray-500 mb-1">
                      <span>😎 Good</span>
                      <span>{selectedRegion.breakdown.good}%</span>
                    </div>
                    <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
                      <div className="bg-[var(--color-ok-teal)] h-full rounded-full" style={{ width: `${selectedRegion.breakdown.good}%` }} />
                    </div>
                  </div>

                  {/* Bad */}
                  <div>
                    <div className="flex justify-between text-xs font-bold text-gray-500 mb-1">
                      <span>😔 Not Good</span>
                      <span>{selectedRegion.breakdown.bad}%</span>
                    </div>
                    <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
                      <div className="bg-[var(--color-ok-orange)] h-full rounded-full" style={{ width: `${selectedRegion.breakdown.bad}%` }} />
                    </div>
                  </div>

                  {/* Unsure */}
                  <div>
                    <div className="flex justify-between text-xs font-bold text-gray-500 mb-1">
                      <span>🤔 Unsure</span>
                      <span>{selectedRegion.breakdown.unsure}%</span>
                    </div>
                    <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
                      <div className="bg-gray-700 h-full rounded-full" style={{ width: `${selectedRegion.breakdown.unsure}%` }} />
                    </div>
                  </div>
                </div>
              </div>

              <div className="border-t border-gray-100 pt-6 mt-4 flex items-center justify-between text-xs text-gray-400 font-bold select-none">
                <span className="flex items-center gap-1">
                  <Heart className="w-3.5 h-3.5 fill-[var(--color-ok-orange)] text-[var(--color-ok-orange)]" /> Shared Empathy Active
                </span>
                <span className="text-[var(--color-ok-orange-shade)] flex items-center gap-0.5 hover:underline cursor-pointer">
                  See Global Feed <ArrowRight className="w-3 h-3" />
                </span>
              </div>
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </motion.div>
  );
}
