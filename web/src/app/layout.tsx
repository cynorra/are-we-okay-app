import type { Metadata } from "next";
import { DM_Sans } from "next/font/google";
import "./globals.css";

const dmSans = DM_Sans({
  variable: "--font-dm-sans",
  subsets: ["latin"],
  weight: ["400", "500", "700"],
});

export const metadata: Metadata = {
  title: "Okayness | A global wellbeing movement",
  description: "A daily anonymous emotional check-in community where people share how they feel, receive peer support, and support others — with zero judgment.",
  keywords: ["wellbeing", "mental health", "anonymous community", "emotional check-in", "okayness", "peer support"],
  authors: [{ name: "Okayness Team" }],
  openGraph: {
    title: "Okayness | Are We Okay?",
    description: "A daily anonymous emotional check-in community. Join the global wellbeing movement.",
    url: "https://areweokay.com",
    siteName: "Okayness",
    images: [
      {
        url: "https://areweokay.com/og-image.jpg",
        width: 1200,
        height: 630,
        alt: "Okayness Global Wellbeing Movement",
      },
    ],
    locale: "en_US",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "Okayness | A global wellbeing movement",
    description: "Share how you feel anonymously, and support others with zero judgment.",
    images: ["https://areweokay.com/twitter-image.jpg"],
  },
  robots: {
    index: true,
    follow: true,
  },
};

const structuredData = {
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "Okayness",
  "operatingSystem": "Web",
  "applicationCategory": "HealthApplication",
  "description": "A daily anonymous emotional check-in community where people share how they feel, receive peer support, and support others with zero judgment.",
  "offers": {
    "@type": "Offer",
    "price": "0",
    "priceCurrency": "USD"
  }
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${dmSans.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
        />
        {children}
      </body>
    </html>
  );
}
