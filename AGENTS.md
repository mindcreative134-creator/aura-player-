# Agent Instructions & Workflow Rules

## 1. Always Sync to GitHub
After completing any feature, bug fix, or refactor task and verifying tests/builds:
1. Stage all changes (`git add -A`).
2. Commit with a clear, conventional commit message (`git commit -m "..."`).
3. Push to the GitHub remote `aura` on branch `main` (`git push aura main`).
4. Ensure working directory remains clean and local `main` is always up to date with `aura/main`.

## 2. Remote Configuration
- **Target Repository:** `https://github.com/mindcreative134-creator/aura-player-.git`
- **Remote Name:** `aura`
- **Default Branch:** `main`

## 3. Play Store Identity & Safeguards
- Strictly maintain `applicationId = "com.cmstudio.auraplayer"`.
- Never reintroduce `MANAGE_EXTERNAL_STORAGE` to the `playstore` flavor.
- Maintain open source attribution in `NOTICE`.
