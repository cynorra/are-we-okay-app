import type { Metadata } from "next";
import { DM_Sans } from "next/font/google";
import "./globals.css";

const dmSans = DM_Sans({
  variable: "--font-dm-sans",
  subsets: ["latin"],
  weight: ["400", "500", "700"],
});

export const metadata: Metadata = {
  title: "Are We Okay | A global wellbeing movement",
  description: "A daily anonymous emotional check-in community where people share how they feel, receive peer support, and support others — with zero judgment.",
  keywords: ["wellbeing", "mental health", "anonymous community", "emotional check-in", "are we okay", "peer support"],
  authors: [{ name: "Are We Okay Team" }],
  alternates: {
    canonical: "https://areweokay.com",
  },
  openGraph: {
    title: "Are We Okay | Wellbeing Community",
    description: "A daily anonymous emotional check-in community. Join the global wellbeing movement.",
    url: "https://areweokay.com",
    siteName: "Are We Okay",
    images: [
      {
        url: "https://areweokay.com/og-image.jpg",
        width: 1200,
        height: 630,
        alt: "Are We Okay Global Wellbeing Movement",
      },
    ],
    locale: "en_US",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "Are We Okay | A global wellbeing movement",
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
  "@graph": [
    {
      "@type": "SoftwareApplication",
      "@id": "https://areweokay.com/#software",
      "name": "Are We Okay",
      "operatingSystem": "Android, iOS, Web",
      "applicationCategory": "HealthApplication",
      "description": "A daily anonymous emotional check-in community where people share how they feel, receive peer support, and support others with zero judgment.",
      "offers": {
        "@type": "Offer",
        "price": "0",
        "priceCurrency": "USD"
      },
      "screenshot": "https://areweokay.com/og-image.jpg"
    },
    {
      "@type": "Organization",
      "@id": "https://areweokay.com/#organization",
      "name": "Are We Okay",
      "url": "https://areweokay.com",
      "logo": "https://areweokay.com/favicon.ico"
    },
    {
      "@type": "WebSite",
      "@id": "https://areweokay.com/#website",
      "url": "https://areweokay.com",
      "name": "Are We Okay",
      "publisher": {
        "@id": "https://areweokay.com/#organization"
      }
    }
  ]
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
          dangerouslySetInnerHTML={{
            __html: `
              (function() {
                try {
                  var theme = localStorage.getItem('ok_theme') || 'warm-soft';
                  document.documentElement.setAttribute('data-theme', theme);
                } catch (e) {}
              })();
            `
          }}
        />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
        />
        {children}
      </body>
    </html>
  );
}
