import { ShieldCheck } from "lucide-react";
import Link from "next/link";

export default function PrivacyPage() {
  return (
    <div className="min-h-screen bg-[var(--color-ok-beige)] text-[var(--color-ok-black)] py-24 px-6 md:px-12">
      <div className="max-w-4xl mx-auto bg-white/80 backdrop-blur-xl p-8 md:p-16 rounded-[3rem] shadow-xl border border-white/40">
        <div className="flex items-center gap-4 mb-12">
          <Link href="/" className="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center border border-gray-200 hover:bg-gray-100 transition-colors">
            &larr;
          </Link>
          <ShieldCheck className="w-8 h-8 text-[var(--color-ok-teal)]" />
          <h1 className="text-3xl md:text-5xl font-bold tracking-tight">Privacy Policy</h1>
        </div>

        <div className="prose prose-lg text-gray-700 space-y-6">
          <p className="font-semibold text-xl text-gray-900">Effective Date: {new Date().toLocaleDateString('en-US')}</p>
          
          <p>
            Welcome to Okayness. We believe that privacy is a fundamental human right, especially when it comes to mental wellbeing. 
            Our core philosophy is "anonymous-by-default". This policy explains what little data we collect and why.
          </p>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">1. Information We Collect</h2>
          <p>We purposefully collect as little information as possible:</p>
          <ul className="list-disc pl-6 space-y-2">
            <li><strong>Email Addresses:</strong> Used solely for authentication and account recovery. Your email is never displayed publicly or tied to your public posts.</li>
            <li><strong>Check-in Data:</strong> Your daily moods, anonymous notes, and interactions (hugs, nudges).</li>
          </ul>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">2. How We Protect Your Anonymity</h2>
          <p>
            When you post to the global feed, your identity is completely stripped away. We do not track your IP address to your posts, 
            and we do not sell your behavioral data to advertisers. 
          </p>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">3. Third-Party Services</h2>
          <p>
            We use Supabase as our secure backend provider. Your encrypted data is stored safely on their servers in accordance with global security standards.
          </p>

          <div className="mt-12 p-6 bg-[var(--color-ok-orange-light)] rounded-2xl border border-[var(--color-ok-orange)]/20">
            <p className="font-medium text-[var(--color-ok-orange-shade)]">
              Questions? Reach out to our privacy team at <a href="mailto:hello@areweokay.com" className="font-bold underline">hello@areweokay.com</a>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
