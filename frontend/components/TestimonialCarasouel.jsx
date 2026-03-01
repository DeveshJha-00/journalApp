"use client";

import React from "react";
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "./ui/carousel";
import Autoplay from "embla-carousel-autoplay";
import { Card, CardContent } from "./ui/card";


const testimonials = [
  {
    author: "Priya S.",
    role: "Daily Journaler",
    text: "This app has completely changed how I reflect on my day. The mood tracking is eye-opening!",
  },
  {
    author: "Arjun M.",
    role: "Mental Wellness Enthusiast",
    text: "The biweekly reports feel like having a therapist who actually reads my journal. Incredible.",
  },
  {
    author: "Sarah K.",
    role: "Product Manager",
    text: "I love the collections feature — I organize entries by project and it's helped my work-life balance so much.",
  },
  {
    author: "Rahul D.",
    role: "Self-Improvement Advocate",
    text: "The sentiment analysis caught emotional patterns I never noticed myself. Truly powerful.",
  },
  {
    author: "Aryan Kumar",
    role: "Student",
    text: "This app is so good that I don't want to stop using it. It's like having a best friend who listens to everything you say.",
  }
];


const TestimonialCarousel = () => {
  return (
    <div className="mt-24 px-5">
      <h2 className="text-3xl font-bold text-center text-orange-900 mb-12">
        What Our Writers Say
      </h2>
      <Carousel
        opts={{
            loop: true, 
        }}
        plugins={[
            Autoplay({
            delay: 2000,
            stopOnInteraction: false, 
            }),
        ]}
        className="w-full mx-auto"
        >
        <CarouselContent>
          {testimonials.map((testimonial, index) => (
            <CarouselItem key={index} className="md:basis-1/2 lg:basis-1/3">
              <Card className="bg-white/80 backdrop-blur-sm">
                <CardContent className="p-6">
                  <blockquote className="space-y-4">
                    <p className="text-orange-700 italic">
                      &quot;{testimonial.text}&quot;
                    </p>
                    <footer>
                      <div className="font-semibold text-orange-900">
                        {testimonial.author}
                      </div>
                      <div className="text-sm text-orange-600">
                        {testimonial.role}
                      </div>
                    </footer>
                  </blockquote>
                </CardContent>
              </Card>
            </CarouselItem>
          ))}
        </CarouselContent>
        <CarouselPrevious />
        <CarouselNext />
      </Carousel>
    </div>
  );
};

export default TestimonialCarousel;