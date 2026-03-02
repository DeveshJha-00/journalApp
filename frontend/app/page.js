"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {Skeleton } from "@/components/ui/skeleton";

import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import {
  BookOpen,
  Brain,
  BarChart3,
  Shield,
  Sparkles,
  Mail,
  ArrowRight,
  Calendar,
} from "lucide-react";
import TestimonialCarousel from "@/components/TestimonialCarasouel";

const features = [
  {
    icon: BookOpen,
    title: "Rich Journal Editor",
    description:
      "Write freely with a beautiful rich-text editor. Organize entries into collections and never lose a thought.",
  },
  {
    icon: Brain,
    title: "AI Sentiment Analysis",
    description:
      "Every entry is analyzed for mood, emotions, and key themes — giving you deeper self-awareness automatically.",
  },
  {
    icon: BarChart3,
    title: "Mood Analytics",
    description:
      "Track your emotional trends over time with visual charts, mood timelines, and personalized insights.",
  },
  {
    icon: Mail,
    title: "Biweekly Reports",
    description:
      "Receive AI-generated mental health reports every two weeks, summarizing your emotional patterns and growth.",
  },
  {
    icon: Sparkles,
    title: "Smart Collections",
    description:
      "Organize entries into smart collections. Filter by emotions, keywords, dates, or moods.",
  },
  {
    icon: Shield,
    title: "Secure & Private",
    description:
      "Your journal is your safe space. Secure authentication keeps your entries private and protected.",
  },
];

const faqs = [
  {
    q: "How does the AI sentiment analysis work?",
    a: "When you publish a journal entry, our AI analyzes the text to identify your mood (on a scale), key emotions like joy or anxiety, and recurring themes. This happens automatically — no extra steps needed.",
  },
  {
    q: "What are biweekly reports?",
    a: "Every two weeks, we generate a personalized mental health insights report based on your recent journal entries. It highlights emotional trends, patterns, and offers reflective prompts.",
  },
  {
    q: "Is my data private?",
    a: "Absolutely. Your journal entries are stored securely and are only accessible to you. We never share your personal data with third parties.",
  },
  {
    q: "Can I organize my entries?",
    a: "Yes! You can create collections (like 'Work', 'Personal', 'Goals') and assign entries to them. You can also filter entries by date, emotions, or keywords.",
  },
  {
    q: "Do I need to pay for this?",
    a: "The core journaling experience is completely free. We believe everyone deserves access to mental health tools.",
  },
];

export default function Home() {
  return (
    <div className="relative container mx-auto px-4 pt-16 pb-16">

      {/* ===== HERO ===== */}
        <div className="max-w-5xl mx-auto text-center text-orange-800 space-y-8">
          <h1 className="text-4xl md:text-6xl font-bold tracking-tight mb-6">
            <span className="bg-gradient-to-r from-orange-500 to-orange-700 bg-clip-text text-transparent">
            Your Space To Reflect
            </span>
            <br/>
            Your mind deserves a safe space
          </h1>
          <p className="text-lg md:text-xl text-orange-800 mb-8 max-w-2xl mx-auto">
            Journal freely, track your mood, and receive personalized
            mental health insights - all in one beautiful app.
          </p>

          <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-t from-orange-50 via-transparent to-transparent pointer-events-none z-10" />
          <div className="bg-white rounded-2xl p-4 max-full mx-auto">
            <div className="border-b border-orange-100 pb-4 mb-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Calendar className="h-5 w-5 text-orange-600"/>
                <span className="text-orange-900 font-medium">
                  Today&rsquo;s Entry
                </span>
              </div>
              <div className="flex gap-2">
                <div className="h-3 w-3 rounded-full bg-orange-200" />
                <div className="h-3 w-3 rounded-full bg-orange-300" />
                <div className="h-3 w-3 rounded-full bg-orange-400" />
              </div>
            </div>
            <div className="space-y-4 p-4">
              <Skeleton className="h-4 bg-orange-100 rounded w-3/4" />
              <Skeleton className="h-4 bg-orange-100 rounded w-full" />
              <Skeleton className="h-4 bg-orange-100 rounded w-2/3" />
            </div>
          </div>
        </div>

          <div className="flex gap-4 justify-center">
            <Link href="/auth">
              <Button variant="journal" className="px-8 py-6 rounded-full flex items-center gap-2">
                Start Journaling <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </Link>
            <Link href="#features">
              <Button
                variant="outline"
                className="px-8 py-6 rounded-full border-orange-600 text-orange-600 hover:bg-orange-100"
              >
                Learn More
              </Button>
            </Link>
          </div>
        </div>

      {/* ===== FEATURES ===== */}
      <section id="features" className="mt-24 grid md:grid-cols-2 lg:grid-cols-3 gap-8 px-4">
            {features.map((f, index) => (
              <Card
                key={index}
                className="shadow-lg"
              >
                <CardContent className="p-6">
                  <div className="h-12 w-12 bg-orange-100 rounded-full flex items-center justify-center mb-4">
                    <f.icon className="h-6 w-6 text-orange-600" />
                  </div>
                  <h3 className="font-semibold text-xl text-orange-900 mb-2">{f.title}</h3>
                  <p className="text-orange-600 text-sm leading-relaxed">
                    {f.description}
                  </p>
                </CardContent>
              </Card>
            ))}
        </section>

              
      <TestimonialCarousel />


      {/* ===== FAQ ===== */}
        <div className="mt-12">
          <h2 className="text-3xl font-bold text-center text-orange-900 mb-12">
            Frequently Asked Questions
          </h2>
          <Accordion type="single" collapsible className="w-full mx-auto">
            {faqs.map((faq, i) => (
              <AccordionItem
                key={i}
                value={`faq-${i}`}
              >
                <AccordionTrigger className="text-orange-900 text-lg">
                  {faq.q}
                </AccordionTrigger>
                <AccordionContent className="text-orange-600">
                  {faq.a}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </div>


      {/* ===== CTA ===== */}
        <div className="mt-15 bg-gradient-to-r from-orange-10 to-amber-100">
          <h2 className="text-3xl text-orange-900 font-bold mb-6">
            Ready to start your Journaling Journey?
          </h2>
          <p className="text-orange-700 mb-8 text-lg">
            Join thousands who are building better self-awareness, one entry at a time.
          </p>
          <Link href="/auth">
            <Button
              size="lg" variant="journal"
              className="text-base px-10"
            >
              Get Started For Free <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </Link>
        </div>


    </div>
  );
}
