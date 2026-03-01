import Header from "@/components/Header";
import Providers from "@/components/Providers";
import "./globals.css";

export const metadata = {
  title: "Journal App",
  description: "A journal application for mental health tracking",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <div className="bg-[url('/bg.jpg')] bg-cover opacity-40 fixed -z-10 inset-0" />

        <Providers>
          <Header />
          <main className="min-h-screen">{children}</main>

          <footer className="bg-orange-50/80 py-8 border-t border-orange-100">
            <div className="mx-auto px-4 text-center text-gray-500 text-sm">
              <p>Made with 🧡 by Devesh</p>
            </div>
          </footer>

        </Providers>

      </body>
    </html>
  );
}
