# GenSolar Pro Engineering

An offline-first Android solar EPC engineering workspace for site survey, PV layout, CAD, structural and electrical screening, procurement, construction tracking and technical handover.

- **Repository:** `VEERA0007/GenSolar-Pro-Engineering`
- **Android application ID:** `com.gensolar.engineering`
- **Current app version:** `0.3.0`
- **Minimum Android:** API 23 (Android 6.0)
- **Build stack:** JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Jetpack Compose Material 3
- **Build workflow:** [GitHub Actions](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/workflows/android-apk.yml)

## Current application structure

- **Native Android shell:** Compose app bar, Home dashboard, bottom navigation, More menu and Android back-to-Home behavior.
- **Engineering workspaces:** bundled HTML/CSS/JavaScript EPC workspaces hosted locally in WebView, plus native Compose workspaces. The native dashboard routes to each workspace through the shared application router.
- **Measurement & 3D survey (v0.3.0 foundation):** native point-coordinate distance, object volume/surface area, room floor/perimeter/wall area, rectangular rooftop slope-area and preliminary PV module estimate, GPS point-to-point distance, bounded on-device measurement history and CSV export. ARCore support detection and optional camera permission are declared; live AR point-picking, depth overlay, room scanning and 3D capture remain pending.
- **Project and site:** project save/load foundations, survey details, roof-zone geometry, GPS coordinates, site media, local project JSON and ZIP backup/restore.
- **PV design:** native Compose capacity/module inputs and live project CAD snapshot preview, Kotlin module-count/DC-capacity/footprint metrics, plus the existing polygon/multi-roof optimizer, obstacle/setback/access-corridor checks, module schedule and DXF/CSV export.
- **Solar resource:** user-entered GHI/DNI/DHI/GTI and yield inputs, performance-loss budget and monthly generation estimate. Resource-source validation and full weather integration remain incomplete.
- **CAD / 3D:** editable 2D geometry, DXF import/export, image survey underlay calibration, snapping, orthogonal drawing, layer visibility and conceptual 3D visualization. This is not a full AutoCAD/DWG replacement.
- **Structural:** IS 875-oriented wind input screens, portal-frame model and STAAD.Pro `.STD` export, steel take-off, preliminary foundation/RCC screens and an in-app 2D elastic portal-frame solver.
- **Electrical and battery:** native Compose battery-bank sizing connected to the tested EPC project engine; module series/parallel topology, nominal/usable kWh, bank Ah, continuous discharge/charge current limits, plus PV string/MPPT, cable, protection and SLD screening.
- **Project delivery:** BOM/BOQ, vendor/RFQ, fabrication, construction, commissioning, DPR and handover registers.

## Build and download the debug APK

1. Open the repository's **Actions** tab.
2. Select **Build GenSolar Pro Engineering APK**.
3. Open the latest successful run on branch `main`.
4. Download `GenSolar-Pro-Engineering-debug-APK` from the Artifacts section.
5. Extract the ZIP to obtain `app-debug.apk`.

Latest verified build: [Build #586](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/44588437802), source commit 9c858bf0754601da6d769c93ec7ac3bd738b88f9. The extracted app-debug.apk is 10,324,400 bytes (9.85 MiB), SHA-256 6f1214335df068698e0e51d67659b86029a90554d88d0ee871a801b6a7346d3e. The artifact ZIP is 9,351,180 bytes; [download the verified APK artifact](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/44588437802/artifacts/14563400799). All 84 workflow steps passed, including APK signature/package verification. The debug artifact is a test build, not a Play Store release.

## Project structure

```text
.
├── .github/workflows/android-apk.yml
├── app/src/main/
│   ├── assets/
│   │   ├── index.html
│   │   └── structural_solver.js
│   ├── java/com/gensolar/engineering/MainActivity.kt
│   └── AndroidManifest.xml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── BUILD_PROGRESS.md
│   └── REFERENCE_REQUIREMENTS.md
└── tools/
    ├── test_structural_solver.js
    └── validate_android_modernization.py
```

## Engineering scope and safety

A green CI build confirms the checks configured in GitHub Actions and successful debug APK packaging. It does **not** certify the full EPC product, verify every screen on a physical device, or approve engineering calculations for construction.

The in-app structural solver is limited to in-plane 2D portal frames and requires verified member properties. It does not replace a complete 3D structural analysis package. Simplified wind, member, foundation, electrical and energy-yield screens are preliminary tools. Final design, code compliance, protection coordination, connections and fabrication/construction release require verified project inputs and review/sign-off by qualified professionals.

The app is offline-first. GPS permissions and online map/resource services are optional; the core workspace should remain usable without a network connection. Physical Android verification for offline launch, local persistence, file import/export, GPS permission, printing, rotation and relaunch remains a release gate.

## Repository separation

This repository is the dedicated engineering app. The existing `VEERA0007/gensolar-pro` and `VEERA0007/Roka-One` repositories are not the build target. Do not copy their signing credentials, Firebase configuration, customer records or release updater configuration into this project.
