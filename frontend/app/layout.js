import Header from "@/components/Header";
import Providers from "@/components/Providers";
import "./globals.css";

import { Lora, Playfair_Display, Inter } from "next/font/google";

const lora = Lora({ subsets: ["latin"], variable: "--font-lora" });
const playfair = Playfair_Display({ subsets: ["latin"], variable: "--font-playfair" });
const inter = Inter({ subsets: ["latin"], variable: "--font-inter" });

export const metadata = {
  title: "Journal App",
  description: "A journal application for mental health tracking",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en" className={`${lora.variable} ${playfair.variable} ${inter.variable}`} suppressHydrationWarning>
      <body className="font-lora">
        <div className="bg-[url('/bg.jpg')] bg-cover opacity-40 dark:opacity-10 fixed -z-10 inset-0" />

        <Providers>
          <Header />
          <main className="min-h-screen">{children}</main>

          <footer className="bg-orange-50/80 dark:bg-gray-900/80 py-8 border-t border-orange-100 dark:border-gray-800">
            <div className="mx-auto px-4 text-center text-gray-500 dark:text-gray-400 text-sm">
              <p>Made with 🧡 by Devesh</p>
            </div>
          </footer>

        </Providers>

      </body>
    </html>
  );
}
