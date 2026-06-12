# Are We Okay?

A cross-platform anonymous mood-tracking app that lets people around the world share how they're feeling — no accounts, no names, just honest signals.

## What It Does

Users submit their current mood anonymously. The app aggregates these in real-time and displays a global pulse — a living snapshot of how the world is feeling right now. Simple, private, human.

## Tech Stack

### Android (`/android`)
- **Kotlin** — native Android development
- **Firebase Realtime Database** — live mood sync across all users
- **Firebase Authentication** — anonymous sign-in, no personal data collected

### Web (`/web`)
- **TypeScript + Next.js** — modern frontend with SSR support
- **Firebase** — shared backend with the Android app
- **CSS** — custom styling

## Architecture

The project follows a mono-repo structure with separate `android/` and `web/` directories sharing the same Firebase backend. Both platforms read and write to the same real-time data layer, meaning a mood submitted on Android is instantly reflected on the web dashboard and vice versa.

## Privacy

No user accounts. No personal data. Anonymous Firebase Auth tokens are used solely to prevent spam — nothing is stored or tracked.

## Status

Available on Google Play Store under the [Cynorra](https://github.com/cynorra) publisher brand.
