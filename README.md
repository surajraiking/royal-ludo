# 👑 Royal Ludo Empire (रॉयल लूडो एम्पायर)

An ultra-modern, AAA-style Ludo board game for Android featuring neon aesthetics, AI opponents, custom themes, dice skins, friend room codes, and coin/gem reward systems.

---

## 📱 How to Download & Install APK (APK कैसे डाउनलोड और इनस्टॉल करें)

### Method 1: Direct Download from Google AI Studio
1. In the top right corner of the AI Studio interface, open the **Project Settings / Options** menu (⚙️ / ⋮ icon).
2. Select **"Download APK"** or **"Export Project as ZIP"**.
3. If downloading APK directly: Open the downloaded `.apk` file on your Android device and tap **Install**.
   *(If prompted, allow "Install Unknown Apps" from your browser/files app settings).*

---

### Method 2: Automatic APK & AAB Builder via GitHub Actions (मोबाइल से ही 1-क्लिक में APK/AAB)
This project comes pre-configured with `.github/workflows/build.yml`!
1. Export or Push this project to your GitHub repository.
2. Every time you push or trigger manually under **Actions** tab on GitHub:
   - GitHub automatically compiles **`Royal-Ludo-Empire-Debug-APK` (.apk)**
   - GitHub automatically compiles **`Royal-Ludo-Empire-AAB-Bundle` (.aab)** for Google Play Store upload
3. Go to the **Actions** tab on your GitHub repository -> Click on the latest workflow run -> Scroll to **Artifacts** -> Tap to download the ready-to-install `.apk` directly onto your phone!

---

### Method 3: Build APK on Mobile (Termux) or Linux / Mac / PC
Run the following command in terminal:
```bash
# Build Debug APK (Ready to install on any Android phone)
gradle assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

To build Google Play Store App Bundle (AAB):
```bash
gradle bundleRelease
# Output AAB path:
# app/build/outputs/bundle/release/app-release.aab
```

---

## 🎮 Game Features
- **Online & Local Multiplayer**: Play with 2, 3, or 4 players on one device or share private room codes with friends.
- **Smart Computer AI**: Easy, Medium, and Hard strategic AI with safe-zone prioritization and opponent attack logic.
- **Multiple Playground Themes**:
  - Classic Royal Gold
  - Cyber Neon Matrix
  - Emerald Forest & Desert Oasis
- **Custom 3D Dice Skins**: Default Royal, Epic Inferno, Neon Cyan, Emerald Dragon.
- **Spin Wheel & Daily Rewards**: Win free coins, gems, and exclusive dice cosmetics.
- **Leaderboards & Player Profile**: Track your wins, win rates, and ranking status.
