# Synapse

A completely modernized, standalone Kernel Manager for Android. This project is a rebuilt and updated fork of the original [AndreiLux/Synapse](https://github.com) engine. It transforms the legacy utility into a fully self-contained, native Android application featuring a contemporary UI and streamlined installation.

**Major Shift from Legacy:** The original Synapse required strict dependency on the Universal Control Interface (UCI) protocol embedded inside a custom kernel's ramdisk. **This version completely eliminates UCI requirements.** All kernel nodes, configuration schemas, and hardware controls are now embedded directly within the APK, allowing it to function universally on any rooted kernel.

## Features

* **Embedded Hardware Controls:** No UCI ramdisk or external scripts needed; all configuration interfaces are hardcoded natively into the application backend.
* **Modern UI:** Redesigned from the ground up to match modern Android aesthetics and navigation flows.
* **Total Kernel Control:** Easily adjust CPU/GPU frequencies, governors, voltages, low-memory killer (LMK) limits, thermal steps, and custom I/O schedulers.
* **Universal Compatibility:** Works across a broader range of kernels and ROMs since it does not require specialized ramdisk backends.
* **Standard APK Deployment:** Installs instantly as a normal application package.

## Requirements

To manage your hardware with Synapse, your system must meet the following baseline requirements:

1. **Android 10+ (API 29):** The minimum supported version is Android 10 or higher.
2. **Root Access:** Superuser permissions (via Magisk, KernelSU, or APatch) are mandatory to read and write to low-level system hardware nodes (`/sys/` filesystem).

## Installation

### Prerequisites
Ensure that **Unknown Sources** installation is permitted within your Android system settings before attempting a manual setup.

### Step-by-Step Installation
1. Go to the [Synapse Releases](https://github.com/yarpiin/Synapse/releases) section of this repository.
2. Download the latest compiled `.apk` installation file.
3. Open your favorite Android file manager and tap the downloaded APK.
4. Complete the standard system prompt to install the application.
5. Open **Synapse** from your app drawer.
6. Grant permanent **Superuser/Root** access when prompted by your root management app.

## Building from Source

This application uses the Gradle build system configured via Kotlin DSL (`.gradle.kts`).

1. Clone this repository locally:
   ```bash
   git clone https://github.com
   cd Synapse
   ```
2. Import the root directory into **Android Studio**.
3. Allow Gradle to sync and fetch all necessary project dependencies automatically.
4. Compile a release build through the built-in IDE controls or via terminal:
   ```bash
   ./gradlew assembleRelease
   ```

## Credits & Acknowledgments

This project builds upon the hard work and innovations of the Android custom kernel community:
* **Andrei_Lux** – For creating the original, groundbreaking Synapse APK and its configuration philosophy.
* **apb_axel** – For the old-style JSON UKM (Universal Kernel Manager) concepts that shaped customizable kernel scripting.

## License

This software is governed by its accompanying reference license. By using, reproducing, or modifying this software, you accept its terms. For complete rules regarding trademark usage, patent litigation limits, and the implied warranty exclusions, please read the full `LICENSE.txt` file included in this repository.

