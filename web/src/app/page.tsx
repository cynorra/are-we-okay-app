"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowDown, X, Heart, Shield, EyeOff, Users, Smile } from "lucide-react";
import Image from "next/image";

import { joinWaitlist } from "../utils/db";

export default function LandingPage() {
  const [mood, setMood] = useState<string | null>(null);
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleMoodSelect = (selectedMood: string) => {
    setMood(selectedMood);
    setSubmitted(false);
  };

  const handleWaitlistSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const success = await joinWaitlist(email, mood || "unsure");
    if (success) {
      setSubmitted(true);
      setEmail("");
    } else {
      alert("Could not join waitlist. Please try again.");
    }
  };


  return (
    <div className="min-h-screen bg-[var(--color-ok-beige)] text-[var(--color-ok-black)] selection:bg-[var(--color-ok-orange)] selection:text-white font-sans overflow-x-hidden">
      {/* HEADER */}
      <header className="fixed top-0 left-0 right-0 z-50 bg-[var(--color-ok-black)] text-[var(--color-ok-off-white)] py-4 px-6 md:px-12 flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-2">
          <Heart className="w-6 h-6 text-[var(--color-ok-orange)]" />
          <span className="font-bold text-xl tracking-tight">Are We Okay</span>
        </div>
        <button
          onClick={() => document.getElementById("waitlist")?.scrollIntoView({ behavior: "smooth" })}
          className="bg-[var(--color-ok-orange)] hover:bg-[var(--color-ok-orange-shade)] text-white px-5 py-2 rounded-full font-medium transition-colors text-sm md:text-base cursor-pointer"
        >
          Join Waitlist
        </button>
      </header>

      {/* HERO SECTION */}
      <section className="relative h-screen w-full flex flex-col items-center justify-center overflow-hidden bg-[var(--color-ok-black)]">
        {/* Background Video */}
        <div className="absolute inset-0 z-0 opacity-60">
          <video
            autoPlay
            loop
            muted
            playsInline
            className="w-full h-full object-cover"
          >
            <source src="https://assets.mixkit.co/videos/preview/mixkit-tree-branches-in-the-breeze-1188-large.mp4" type="video/mp4" />
          </video>
        </div>

        <div className="relative z-20 flex flex-col items-center text-center px-4 w-full max-w-4xl mt-16">
          <motion.h1 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="text-5xl md:text-7xl font-bold text-white mb-6 tracking-tight drop-shadow-lg"
          >
            Are We Okay?
          </motion.h1>
          <motion.p 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="text-xl md:text-2xl text-white/90 mb-12 font-medium max-w-2xl drop-shadow-md"
          >
            A global wellbeing movement. Check in daily, share how you feel anonymously, and support each other with zero judgment.
          </motion.p>

          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="flex flex-col sm:flex-row gap-4 w-full sm:w-auto"
          >
            <button 
              onClick={() => handleMoodSelect('good')}
              className="cursor-pointer bg-white/10 hover:bg-white/20 backdrop-blur-md border border-white/30 text-white px-8 py-4 rounded-full text-lg font-medium transition-all hover:scale-105 active:scale-95 flex items-center justify-center gap-3 shadow-xl"
            >
              <span>😎</span> We're Good
            </button>
            <button 
              onClick={() => handleMoodSelect('bad')}
              className="cursor-pointer bg-white/10 hover:bg-white/20 backdrop-blur-md border border-white/30 text-white px-8 py-4 rounded-full text-lg font-medium transition-all hover:scale-105 active:scale-95 flex items-center justify-center gap-3 shadow-xl"
            >
              <span>😔</span> We're Not
            </button>
            <button 
              onClick={() => handleMoodSelect('unsure')}
              className="cursor-pointer bg-white/10 hover:bg-white/20 backdrop-blur-md border border-white/30 text-white px-8 py-4 rounded-full text-lg font-medium transition-all hover:scale-105 active:scale-95 flex items-center justify-center gap-3 shadow-xl"
            >
              <span>🤔</span> Not Sure
            </button>
          </motion.div>
        </div>

        <motion.div 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1, duration: 1 }}
          className="absolute bottom-10 z-20 flex flex-col items-center text-white/70"
        >
          <span className="text-sm font-medium tracking-widest uppercase mb-2">Scroll to discover</span>
          <ArrowDown className="w-5 h-5 animate-bounce" />
        </motion.div>
      </section>

      {/* MOOD MODAL */}
      <AnimatePresence>
        {mood && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
          >
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-[var(--color-ok-off-white)] rounded-3xl p-8 max-w-md w-full relative shadow-2xl"
            >
              <button 
                onClick={() => setMood(null)}
                className="cursor-pointer absolute top-5 right-5 text-gray-500 hover:text-black transition-colors"
              >
                <X className="w-6 h-6" />
              </button>
              
              <div className="text-center mb-6">
                <div className="text-4xl mb-4 drop-shadow-sm">
                  {mood === 'good' ? '😎' : mood === 'bad' ? '😔' : '🤔'}
                </div>
                <h3 className="text-2xl font-bold mb-2 text-gray-900">
                  {mood === 'good' ? "Wonderful 😊" : mood === 'bad' ? "We hear you 💙" : "That's okay too 🤗"}
                </h3>
                <p className="text-gray-600">
                  {mood === 'good' ? "So glad you're doing well. 🫂" : mood === 'bad' ? "Hard times don't last. 🫂" : "Not knowing is also an answer. 🫂"}
                </p>
                <div className="mt-4 inline-block bg-[var(--color-ok-orange)]/10 text-[var(--color-ok-orange-shade)] px-4 py-2 rounded-full text-sm font-semibold">
                  Today, 2,451 people felt the same.
                </div>
              </div>

              {!submitted ? (
                <form onSubmit={handleWaitlistSubmit} className="space-y-4">
                  <div className="space-y-2 text-left">
                    <label htmlFor="email" className="text-sm font-medium text-gray-700">Get early access when we launch</label>
                    <input 
                      type="email" 
                      id="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="Enter your email" 
                      className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] bg-white transition-all text-black"
                    />
                  </div>
                  <button 
                    type="submit"
                    className="cursor-pointer w-full bg-[var(--color-ok-black)] hover:bg-gray-800 text-white py-3 rounded-xl font-medium transition-colors shadow-lg"
                  >
                    Join Waitlist
                  </button>
                </form>
              ) : (
                <motion.div 
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="bg-green-50 text-green-700 p-4 rounded-xl text-center font-medium"
                >
                  Thank you! We'll let you know when we're ready.
                </motion.div>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* WHAT IS THIS? SECTION */}
      <section className="py-24 px-6 md:px-12 max-w-7xl mx-auto">
        <div className="grid md:grid-cols-2 gap-16 items-center">
          <div>
            <h2 className="text-4xl md:text-5xl font-bold mb-8 leading-tight text-[var(--color-ok-black)]">A place where honesty heals.</h2>
            <p className="text-lg text-gray-700 mb-8 leading-relaxed">
              We live in a world where everyone pretends to be okay. Are We Okay is a sanctuary for the truth. It's a daily check-in to pause, reflect, and connect with a global community that truly understands.
            </p>
            <div className="space-y-4">
              <div className="flex items-center gap-4 text-xl">
                <span className="font-bold text-[var(--color-ok-orange)] w-32">Anonymous</span> <span className="text-gray-400">&middot;</span> <span>Struggles</span>
              </div>
              <div className="flex items-center gap-4 text-xl">
                <span className="font-bold text-[var(--color-ok-orange)] w-32">Or</span> <span className="text-gray-400">&middot;</span> <span>Joys</span>
              </div>
              <div className="flex items-center gap-4 text-xl">
                <span className="font-bold text-[var(--color-ok-orange)] w-32">Real</span> <span className="text-gray-400">&middot;</span> <span>Support</span>
              </div>
              <div className="flex items-center gap-4 text-xl">
                <span className="font-bold text-[var(--color-ok-orange)] w-32">Real</span> <span className="text-gray-400">&middot;</span> <span>Empathy</span>
              </div>
              <div className="flex items-center gap-4 text-xl">
                <span className="font-bold text-[var(--color-ok-orange)] w-32">Zero</span> <span className="text-gray-400">&middot;</span> <span>Judgment</span>
              </div>
            </div>
            
            <div className="mt-12 p-6 bg-[var(--color-ok-off-white)] rounded-2xl inline-block border border-[rgba(26,26,26,0.05)] shadow-sm">
              <p className="text-lg font-medium flex items-center gap-2">
                <span className="inline-block animate-heartbeat text-[var(--color-ok-orange)]">❤️</span> 
                Together, we're better.
              </p>
            </div>
          </div>
          <div className="relative">
            <div className="aspect-[4/5] bg-[var(--color-ok-orange)]/10 rounded-3xl overflow-hidden border border-[var(--color-ok-orange)]/20 flex items-center justify-center p-8 shadow-inner">
               {/* Abstract placeholder for app mockup */}
               <div className="w-full max-w-sm bg-white rounded-[2.5rem] shadow-2xl h-[600px] border-[8px] border-gray-100 flex flex-col p-6 relative">
                 <div className="w-16 h-1 bg-gray-200 rounded-full mx-auto mb-8"></div>
                 <h4 className="text-2xl font-bold text-center mb-6">Are we okay today?</h4>
                 <div className="space-y-4">
                   <div className="h-16 bg-gray-50 rounded-2xl flex items-center px-4 gap-4 border border-gray-100 shadow-sm">
                     <span className="text-2xl">😎</span> <span className="font-medium text-gray-700">We're Good</span>
                   </div>
                   <div className="h-16 bg-gray-50 rounded-2xl flex items-center px-4 gap-4 border border-gray-100 shadow-sm">
                     <span className="text-2xl">😔</span> <span className="font-medium text-gray-700">We're Not</span>
                   </div>
                   <div className="h-16 bg-gray-50 rounded-2xl flex items-center px-4 gap-4 border border-gray-100 shadow-sm">
                     <span className="text-2xl">🤔</span> <span className="font-medium text-gray-700">Not Sure</span>
                   </div>
                 </div>
                 
                 <div className="mt-auto space-y-4">
                   <div className="h-24 bg-[var(--color-ok-teal)]/10 rounded-2xl p-4">
                     <div className="w-8 h-8 rounded-full bg-[var(--color-ok-teal)]/20 mb-2"></div>
                     <div className="h-2 w-3/4 bg-[var(--color-ok-teal)]/20 rounded"></div>
                     <div className="h-2 w-1/2 bg-[var(--color-ok-teal)]/20 rounded mt-2"></div>
                   </div>
                 </div>
               </div>
            </div>
          </div>
        </div>
      </section>

      {/* HOW IT WORKS */}
      <section className="bg-[var(--color-ok-orange)] text-white py-24 px-6 md:px-12">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-4xl md:text-5xl font-bold mb-4 drop-shadow-sm">How it works</h2>
            <p className="text-xl text-white/90">Six steps to a healthier mind and community.</p>
          </div>
          
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {[
              { icon: '🗓️', title: 'Daily Check-in', desc: 'Share how you feel each day.' },
              { icon: '🙈', title: 'Post Anonymously', desc: 'Speak freely, stay hidden.' },
              { icon: '🫂', title: 'Receive Support', desc: 'Get real empathy from the community.' },
              { icon: '💌', title: 'Give Support', desc: 'Listen and be there for others.' },
              { icon: '👭', title: 'Friend Check-ins', desc: 'Check on the people you care about.' },
              { icon: '🌱', title: 'Together We Heal', desc: 'Healing grows when shared.' },
            ].map((step, i) => (
              <div key={i} className="bg-white/10 border border-white/20 p-8 rounded-3xl hover:bg-white/20 transition-all hover:-translate-y-1 shadow-lg backdrop-blur-sm">
                <div className="text-4xl mb-6 drop-shadow-md">{step.icon}</div>
                <h3 className="text-2xl font-bold mb-3 flex items-center gap-3">
                  <span className="text-white/60 text-xl font-medium">{i + 1}.</span> {step.title}
                </h3>
                <p className="text-white/80 text-lg leading-relaxed">{step.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* COMMUNITY RULES */}
      <section className="py-24 px-6 md:px-12 bg-[var(--color-ok-off-white)] text-[var(--color-ok-black)]">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-4xl md:text-5xl font-bold mb-16 text-center">Our Community Rules</h2>
          
          <div className="grid sm:grid-cols-2 gap-6 mb-12">
            <div className="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
              <Shield className="w-8 h-8 text-[var(--color-ok-orange)] mb-4" />
              <h3 className="text-xl font-bold mb-2">Zero Judgment</h3>
              <p className="text-gray-600 leading-relaxed">No one's story is too much or too little.</p>
            </div>
            <div className="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
              <EyeOff className="w-8 h-8 text-[var(--color-ok-orange)] mb-4" />
              <h3 className="text-xl font-bold mb-2">Protect Anonymity</h3>
              <p className="text-gray-600 leading-relaxed">Never try to identify another user.</p>
            </div>
            <div className="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
              <Smile className="w-8 h-8 text-[var(--color-ok-orange)] mb-4" />
              <h3 className="text-xl font-bold mb-2">Be Kind</h3>
              <p className="text-gray-600 leading-relaxed">Soft words, open hearts. Kindness is strength.</p>
            </div>
            <div className="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
              <Users className="w-8 h-8 text-[var(--color-ok-orange)] mb-4" />
              <h3 className="text-xl font-bold mb-2">Content Warning</h3>
              <p className="text-gray-600 leading-relaxed">Flag heavy topics so others can prepare.</p>
            </div>
          </div>
          
          <div className="bg-[var(--color-ok-black)] text-white p-8 md:p-12 rounded-3xl flex flex-col md:flex-row items-center justify-between gap-8 shadow-xl">
            <div>
              <h3 className="text-2xl font-bold mb-2 text-[var(--color-ok-orange)]">Seek Professional Help</h3>
              <p className="text-white/80 text-lg">We're friends, not therapists. For crises, please reach out to a professional.</p>
            </div>
            <button className="cursor-pointer whitespace-nowrap px-8 py-4 bg-white text-[var(--color-ok-black)] font-bold rounded-full hover:bg-gray-100 transition-colors shadow-lg">
              View Resources
            </button>
          </div>
        </div>
      </section>

      {/* FOOTER & WAITLIST */}
      <footer id="waitlist" className="bg-[var(--color-ok-black)] text-white py-24 px-6 md:px-12 border-t border-white/10 relative overflow-hidden">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-full max-w-4xl opacity-10 pointer-events-none">
           <Heart className="w-full h-full text-[var(--color-ok-orange)] scale-150" />
        </div>
        
        <div className="max-w-4xl mx-auto text-center relative z-10">
          <h2 className="text-4xl md:text-5xl font-bold mb-6 drop-shadow-sm">Ready to join the movement?</h2>
          <p className="text-xl text-white/80 mb-12 max-w-2xl mx-auto leading-relaxed">
            We are launching soon. Leave your email to get early access and be part of our beta community.
          </p>
          
          {!submitted ? (
            <form onSubmit={handleWaitlistSubmit} className="flex flex-col sm:flex-row gap-4 max-w-lg mx-auto mb-20">
              <input 
                type="email" 
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Enter your email" 
                className="flex-1 px-6 py-4 rounded-full bg-white/10 border border-white/20 text-white placeholder:text-white/50 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] shadow-inner transition-all"
              />
              <button 
                type="submit"
                className="cursor-pointer bg-[var(--color-ok-orange)] hover:bg-[var(--color-ok-orange-shade)] text-white px-8 py-4 rounded-full font-bold transition-all shadow-lg hover:shadow-xl"
              >
                Join Waitlist
              </button>
            </form>
          ) : (
            <div className="mb-20 inline-block bg-white/10 border border-white/20 text-white px-8 py-4 rounded-full font-medium shadow-sm">
              Thank you! You're on the list. 💙
            </div>
          )}

          <div className="border-t border-white/10 pt-12 flex flex-col md:flex-row items-center justify-between gap-6 text-white/50 text-sm">
            <div className="flex items-center gap-2">
              <Heart className="w-5 h-5 text-[var(--color-ok-orange)]" />
              <span className="font-bold text-white text-xl tracking-tight">Are We Okay</span>
            </div>
            <p>&copy; {new Date().getFullYear()} Are We Okay. Made with ❤️ around the world.</p>
            <div className="flex gap-6">
              <a href="/kvkk" className="hover:text-white transition-colors">KVKK</a>
              <a href="/privacy" className="hover:text-white transition-colors">Privacy</a>
              <a href="/terms" className="hover:text-white transition-colors">Terms</a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
