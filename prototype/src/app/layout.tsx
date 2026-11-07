import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Wault — Design Contract",
  description:
    "Multi-account WhatsApp client for Android and web. Material 3 foundations with Apple-grade restraint. Interactive design contract for the Wault v2 build.",
  authors: [{ name: "Wault" }],
  icons: {
    icon: "logo.svg",
  },
  openGraph: {
    title: "Wault — Design Contract",
    description:
      "Multi-account WhatsApp client concept. Android + Web Edition. Phase 0 of the Wault v2 build plan.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-background text-foreground`}
      >
        {children}
        <Toaster />
      </body>
    </html>
  );
}
