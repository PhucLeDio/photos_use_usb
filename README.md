# First line
Well, how should I put it? This project is simply my attempt to find a solution that can display photos on my USB drive in the Google Photos format 😅. 
It has almost minimalist functionality, but enough to show photos from your USB drive in the same format as Google Photos, instead of having to read each file name in your "file explorer" 🤔.
So, if you're looking to free up space but still want to use Photos, this cheap version is for you. Enjoy! 😘

# 📸 USB Photos App

**USB Photos** is a native Android application built with Kotlin that allows users to browse, view, manage, and upload photos directly to external storage (USB OTG) without copying data to the phone's internal memory.

The project focuses on high-performance local file processing, specifically solving the Out of Memory (OOM) issues often encountered when displaying thousands of images from peripheral hardware.

## ✨ Key Features

* 🔌 **Direct USB Access:** Utilizes the Storage Access Framework (SAF) for secure permission handling and direct file system access to USB drives.
* 📅 **Smart Grouping:** Automatically scans and groups images by modification date (Group by Date) with an intuitive grid interface.
* ⚡ **High-Performance Rendering:**
    * Leverages **Coroutines** to offload I/O tasks (file scanning, copying, deleting) to background threads.
    * Implements **Image Downsampling** to create thumbnails based on actual display size, preventing RAM exhaustion.
* 🔍 **Multi-touch Interaction:** Supports Pinch-to-Zoom gestures (zoom, pan, swipe) for a seamless full-screen viewing experience.
* ⬆️ **Batch Upload:** Allows selecting multiple images from the device gallery and writing them as byte streams directly to the USB drive.
* 🗑️ **Easy Management:** Supports single-file deletion in detail view or **Long-press** on the main screen to enter selection mode for bulk deletion.

## 🛠 Tech Stack & Libraries

* **Language:** Kotlin
* **UI Engine:** Traditional XML Layouts, ConstraintLayout.
* **UI Architecture:** `RecyclerView` with `GridLayoutManager` using **SpanSizeLookup** for multi-view type headers.
* **Third-party Libraries:**
    * [Coil](https://coil-kt.github.io/coil/): Image loading optimization, memory caching, and automatic downsampling.
    * [PhotoView](https://github.com/Baseflow/PhotoView): ImageView extension for gesture-based zooming and panning.
    * `kotlinx-coroutines-android`: For asynchronous thread management.
    * `androidx.documentfile`: For CRUD operations on SAF document trees.

## 🚀 Getting Started

### Prerequisites:
* Android Studio (Latest version)
* A physical Android device (Android 8.0 Oreo or higher).
* A USB drive and a compatible OTG adapter (USB Type-C or Micro-USB).

### Installation:
1. Clone the repository or download the source ZIP.
2. Open Android Studio, select **Open**, and navigate to the project folder.
3. Wait for the Gradle sync to complete (`Sync Project with Gradle Files`).
4. Connect your USB OTG to the phone and connect the phone to your computer for debugging.
5. Click the **Run (Shift + F10)** button to install the app on your device.

## 📱 How to Use

1. **Initialization:** Open the app and tap **"Select USB"**. The system file picker will appear. Select the root directory of your USB drive and tap **Allow**.
2. **Browsing:** The app will automatically load and categorize photos by date. Scroll to browse the collection.
3. **Detail View:** Tap any photo to view it full-screen. Use two fingers to zoom in/out. Tap the trash icon in the top-right corner to delete the current photo.
4. **Upload to USB:** Tap the **"Upload"** button on the main screen and select one or more photos from your device. The app will copy them to the USB and refresh the list.
5. **Batch Deletion:** **Long-press** any photo to enter "Selection Mode." Continue tapping other photos to select them, then tap the red **"Delete Selected"** button at the top.

---
*This project was created in an effort to find an alternative to my Google Photos account, which was crying because of overflowing.*
