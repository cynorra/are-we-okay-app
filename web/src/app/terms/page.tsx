import { FileText } from "lucide-react";
import Link from "next/link";

export default function TermsPage() {
  return (
    <div className="min-h-screen bg-[var(--color-ok-beige)] text-[var(--color-ok-black)] py-24 px-6 md:px-12">
      <div className="max-w-4xl mx-auto bg-white/80 backdrop-blur-xl p-8 md:p-16 rounded-[3rem] shadow-xl border border-white/40">
        <div className="flex items-center gap-4 mb-12">
          <Link href="/" className="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center border border-gray-200 hover:bg-gray-100 transition-colors">
            &larr;
          </Link>
          <FileText className="w-8 h-8 text-[var(--color-ok-black)]" />
          <h1 className="text-3xl md:text-5xl font-bold tracking-tight">Terms of Service</h1>
        </div>

        <div className="prose prose-lg text-gray-700 space-y-6">
          <p className="font-semibold text-xl text-gray-900">Effective Date: {new Date().toLocaleDateString('en-US')}</p>
          
          <p>
            By joining Okayness, you agree to become part of a community built on empathy, respect, and zero judgment.
          </p>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">1. Community Guidelines</h2>
          <p>As a user of Okayness, you agree to:</p>
          <ul className="list-disc pl-6 space-y-2">
            <li><strong>Be Kind:</strong> Harassment, hate speech, or bullying of any kind will result in immediate termination of your account.</li>
            <li><strong>Protect Anonymity:</strong> Do not attempt to dox or reveal the identity of any anonymous user.</li>
            <li><strong>Use Content Warnings:</strong> When sharing heavy or sensitive topics, utilize the content warning features.</li>
          </ul>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">2. Not a Medical Service</h2>
          <p>
            Okayness is a peer-support platform, not a substitute for professional mental health care. We do not provide medical advice. 
            If you are in a crisis, please contact emergency services or a crisis hotline in your area.
          </p>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">3. Termination</h2>
          <p>
            We reserve the right to suspend or terminate any account that violates these terms or disrupts the safe environment of our community.
          </p>

          <div className="mt-12 p-6 bg-gray-100 rounded-2xl border border-gray-200">
            <p className="font-medium text-gray-800">
              Contact us at <a href="mailto:hello@areweokay.com" className="font-bold underline">hello@areweokay.com</a> for legal inquiries.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
