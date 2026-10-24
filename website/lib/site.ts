/**
 * @file site.ts
 * @description Central configuration file for the website's metadata, links, and constant values.
 * @module lib/site
 */

/**
 * Global site configuration object.
 * Contains metadata, external links, and author information used throughout the application.
 */
export const siteConfig = {
  name: "Aura Player",
  version: "v1.0.0",
  description:
    "Next-generation media player for Android with Obsidian design, cinema ambient lighting, advanced hardware decoding, and pure offline privacy.",
  url: "https://aura-player.vercel.app",
  ogImage: "https://aura-player.vercel.app/og.jpg",
  icons: {
    icon: "/icon.svg",
    apple: "/apple-icon.png",
  },
  links: {
    github: "https://github.com/mindcreative134-creator/aura-player-",
    releases: "https://github.com/mindcreative134-creator/aura-player-/releases",
    latestRelease: "https://github.com/mindcreative134-creator/aura-player-/releases/latest",
    izzyOnAndroid: "https://apt.izzysoft.de/packages/com.cmstudio.auraplayer",
    contributors: "https://github.com/mindcreative134-creator/aura-player-/graphs/contributors",
  },
  author: {
    name: "mindcreative134-creator",
    url: "https://github.com/mindcreative134-creator",
  },
} as const;

export type SiteConfig = typeof siteConfig;
