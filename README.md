# Music Nasir Khan

A modern, high-performance Android multimedia application engineered by **Nasir Khan** to deliver a seamless, visually immersive experience for local music listening, dynamic short-form videos (Shorts), and full-length video playback.

---

## ✨ Features

### 🎵 Music Player
- **Modern Playback Engine**: Seamless local audio playback with high-fidelity sound output.
- **Comprehensive Controls**: Smooth Play, Pause, Skip Next, Previous, and precision seek bar controls.
- **Queue Management**: Full support for Shuffle mode and Repeat modes (Off, Repeat All, Repeat One).
- **Metadata Display**: Real-time display of track title, artist, album, duration, and elapsed playback time.
- **Favorites Integration**: Quick one-tap heart toggle to curate favorite tracks into a dedicated view.
- **Animated Visualizer**: Dynamic animated waveform visualizer reacting to playback state.

### 💿 Vinyl Turntable Mode
- **Retro Interactive Disc**: A stylized, rotating vinyl record turntable visualization for currently playing tracks.
- **Dynamic Spin Mechanics**: Disc spins during active playback and smoothly glides to a stop when paused.
- **Immersive Aesthetic**: Elevates the listening experience with visual elegance and analog charm.

### 🎬 Shorts
- **Vertical Feed Experience**: Optimized short-form video playback designed for quick browsing and immersive portrait consumption.
- **Smooth Snapping & Scrolling**: Effortless navigation between short videos.
- **Interaction Tools**: In-feed like/favorite toggles and view counter tracking.
- **90-Second Format**: Strict 90-second (1 min 30 sec) duration ceiling dedicated to concise content.
- **Aspect Ratio Preservation**: Automatic detection preserving true portrait dimensions without distortion or unnatural stretching.

### 📺 Long Videos
- **Dedicated Video Section**: Streamlined gallery for long-form video files stored on the device.
- **Immersive Fullscreen**: Seamless transitions to dedicated fullscreen viewing.
- **Orientation & Rotation**: Fluid support for both portrait and landscape orientations, adapting naturally to device rotation.
- **Zero Distortion**: Preserves original aspect ratios using intelligent fit and letterboxing/pillarboxing where necessary.
- **Intuitive Overlay Controls**: Clean, accessible media controls for scrubbing, pausing, and resizing.

### ❤️ Favorites
- Curate favorite songs, Shorts, and long videos with immediate access in specialized filtered collections.

### 📊 Statistics
- **Activity & Engagement**: Tracks playback sessions, total listening time, play counts, and video view counts.
- **Top Tracks & Recent History**: Comprehensive insights into most-played tracks and completed listening sessions.

### 🎨 Themes
- **Visual Customization Lab**: Multiple hand-crafted dark-themed color palettes (Cyberpunk Neon, Emerald Glow, Deep Amber, Sunset Magenta, and more).
- **Real-Time Switcher**: Instant theme application across the entire application interface.

---

## 🧭 Navigation

The application features an intuitive, modern bottom navigation system with 6 accessible tabs:

| Tab | Icon | Description |
| :--- | :---: | :--- |
| **Player** | 💽 | Now Playing screen featuring album art or the interactive spinning Vinyl Turntable mode |
| **Music** | 🎵 | Local audio library, playlist manager, and favorites list |
| **Shorts** | ⚡ | Fast vertical video feed displaying videos up to 90 seconds |
| **Videos** | 🎬 | Long-form video gallery with fullscreen and rotation playback |
| **Stats** | 📊 | Listening statistics, play counts, view analytics, and top records |
| **Themes** | 🎨 | Theme selector to customize colors and visual accents across the app |

Users can fluidly switch between tabs with zero disruption to background music playback.

---

## 🎥 Video Experience

The video player is built with strict adherence to visual geometry and display standards:

- **No Video Stretching**: Videos strictly retain their native width-to-height proportions.
- **No Unwanted Distortion**: Content is rendered cleanly without artificial scaling or compression.
- **Aspect Ratio Adaptation**:
  - **Portrait Videos**: Maintained in true vertical orientation with clear framing.
  - **Landscape Videos**: Rendered proportionally with adaptive letterboxing.
  - **Square Videos**: Perfectly centered without edge clipping.
- **Fullscreen & Rotation**: Seamlessly transitions between inline preview and full immersive mode when the device is rotated or the fullscreen button is tapped.

---

## 🎧 Audio Experience

Engineered for local music enthusiasts, Music Nasir Khan offers:
- Fast media library indexing via Android MediaStore.
- Low-latency audio playback controls.
- Automatic track progression and seamless queue cycling.
- Persistent state management retaining your current playlist position.

---

## 📱 User Interface

- **Modern & Sleek**: Tailored with clean Material Design 3 guidelines.
- **Dark-Themed Elegance**: High-contrast, eye-friendly dark surfaces with vibrant accent highlights.
- **Responsive & Mobile-First**: Crafted specifically for Android smartphones with fluid animations and generous touch targets (min 48dp).
- **Visual Identity**: Branded with the distinct **Music Nasir Khan** aesthetic.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Language** | Kotlin (2.2.10) |
| **UI Framework** | Jetpack Compose (BOM 2024.09.00), Material Design 3 |
| **Architecture** | MVVM (Model-View-ViewModel), Clean Architecture, Repository Pattern |
| **Concurrency & Async** | Kotlin Coroutines, StateFlow, SharedFlow |
| **Local Persistence** | Room Database (2.7.0) with KSP |
| **Media & Storage** | Android MediaStore API, Scoped Storage (API 29–36+), DocumentFile, MediaScanner |
| **Lifecycle & Navigation** | AndroidX Lifecycle (2.8.7), Activity Compose (1.10.1) |
| **Build System** | Gradle (Kotlin DSL), Android Gradle Plugin 9.1.1 |

---

## 📂 Project Structure

```
app/src/main/
├── AndroidManifest.xml                  # App manifest & permissions
├── java/com/example/
│   ├── MainActivity.kt                  # Main entry point & bottom navigation host
│   ├── audio/
│   │   └── MusicAudioEngine.kt          # Audio playback engine and state
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt           # Room Database definition & migrations
│   │   │   ├── LocalMediaScanner.kt     # Scans device MediaStore for audio & video
│   │   │   └── MediaFileManager.kt      # Scoped Storage file operations (Delete, Rename, Move, Share)
│   │   ├── model/
│   │   │   └── MusicModels.kt           # Data models: Song, VideoItem, Playlists, Themes, Stats
│   │   └── repository/
│   │       └── MusicRepository.kt       # Unified repository coordinating database & library cache
│   └── ui/
│       ├── components/                  # Reusable UI components & 3-dot management menus
│       ├── screens/                     # Player, Playlists, Shorts, Videos, Stats, Themes screens
│       ├── theme/                       # Color palettes, Typography, Theme definition
│       └── viewmodel/
│           └── MusicViewModel.kt        # App-wide UI state orchestration & action handling
└── res/                                 # Strings, adaptive drawables, launcher icons
```

---

## 🚀 Build and Run

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17 or higher
- Android SDK with Platform 36 installed

### 1. Clone the repository
```bash
git clone <repository-url>
cd music-nasir-khan
```

### 2. Open in Android Studio
- Launch Android Studio.
- Select **Open** and choose the cloned project root folder.

### 3. Sync Gradle
- Allow Gradle to download dependencies and sync configuration files.

### 4. Build the Project
```bash
gradle assembleDebug
```

### 5. Run on Device or Emulator
- Connect your Android device via USB (with Developer Options and USB Debugging enabled) or start an Android Virtual Device (AVD).
- Click the green **Run** button in Android Studio, or execute:
```bash
gradle installDebug
```

---

## 🔐 Permissions

The app requests only standard media read permissions to locate local audio and video files:

| Permission | Minimum SDK | Purpose |
| :--- | :---: | :--- |
| `android.permission.READ_MEDIA_AUDIO` | API 33+ | Read local music and audio files |
| `android.permission.READ_MEDIA_VIDEO` | API 33+ | Read local video files (Shorts and Videos) |
| `android.permission.READ_EXTERNAL_STORAGE` | Up to API 32 | Read media files on Android 12 and older |
| `android.permission.INTERNET` | All | Network capability if needed for remote streaming |

*Note: The app adheres to Android Scoped Storage guidelines. File deletions leverage standard system consent prompts (`MediaStore.createDeleteRequest`) rather than requesting invasive all-files access.*

---

## 📸 Screenshots

*(Screenshots will be added in a future update)*

- `screenshots/player.png`
- `screenshots/music.png`
- `screenshots/shorts.png`
- `screenshots/videos.png`

---

## 🔄 Current Status

**Music Nasir Khan** is actively maintained and continuously improved. Current development milestones focus on:
- Enhancing low-latency music and audio playback.
- Optimizing vertical scrolling performance in the Shorts feed.
- Advancing video aspect-ratio auto-detection across non-standard resolutions.
- Expanding playlist creation and management capabilities.
- Refining Material 3 transitions and user ergonomics.

---

## 🧑‍💻 Developer

- **Developer**: Nasir Khan
- **Project**: Music Nasir Khan

---

## 📄 License

License information will be added in a future update.

---

## ⭐ Final Section

Music Nasir Khan is a personal Android multimedia project focused on combining music playback, short videos, long-form videos and a modern visual experience in one application.

If you like the project, consider giving the repository a ⭐.
