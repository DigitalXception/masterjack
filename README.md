# 21 Trainer — Blackjack Basic Strategy App

A clean, dark-themed Android app for drilling blackjack basic strategy
(4–8 decks, dealer hits soft 17, double after split allowed, surrender
2–10) and tracking the specific hands you keep getting wrong.

- Deals a random two-card hand + dealer up-card
- You pick Hit / Stand / Double / Split / Surrender
- Instant feedback against the exact strategy table (source: WizardOfOdds.com)
- Every miss is logged locally and shown on the **Review** tab, ranked
  by how often you get that exact scenario wrong (e.g. "Hard 16 vs
  dealer 10 — you tend to Hit, correct play is Surrender")
- Accuracy and streak tracked across sessions, all stored on-device
  (no account, no network calls)

## Get the APK — no Android Studio needed

This repo builds itself. Once it's on GitHub:

1. Push this project to a new GitHub repository.
2. Go to the **Actions** tab and confirm the "Build APK" workflow runs
   (it triggers automatically on every push to `main`, or run it
   manually with "Run workflow").
3. Once it finishes, go to the repo's **Releases** page — there will
   be a release called **"Latest build"** with `app-debug.apk`
   attached. Download that file on your phone and install it (you'll
   need to allow "install unknown apps" for your browser/file manager
   the first time).

The APK is also attached to every workflow run under **Actions →
(latest run) → Artifacts**, if you'd rather grab it from there.

### Pushing this project to GitHub

```bash
cd BlackjackTrainer
git init
git add .
git commit -m "Initial commit: blackjack strategy trainer"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

## Building locally (optional)

If you'd rather build it yourself or open it in Android Studio:

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`. Or
just open the project folder in Android Studio (Giraffe or newer) and
hit Run — it uses standard Gradle/Kotlin/Compose, nothing exotic.

Requires JDK 17. Minimum supported Android version is 8.0 (API 26).

## Editing the strategy table

The whole table lives in one place:
`app/src/main/java/com/strategy/blackjacktrainer/logic/Strategy.kt`.
If your rule set differs (fewer decks, dealer stands on soft 17, no
DAS, etc.), just update the `hard`, `soft`, and `pair` maps there.

## Notes

- This is a debug-signed build, which is normal for personal/sideload
  use. If you ever want a Play Store–ready release build, you'll need
  to set up your own signing key — the debug workflow won't cover
  that on purpose.
- All data (stats + mistake log) is stored locally in the app's
  SharedPreferences. Uninstalling the app clears it.
