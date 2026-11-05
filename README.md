# GenSolar Pro Engineering

A separate, offline-first Android application for rooftop solar engineering workflows.

- **Repository:** `VEERA0007/GenSolar-Pro-Engineering`
- **Android application ID:** `com.gensolar.engineering`
- **Initial version:** `0.1.0`
- **Minimum Android:** API 23 (Android 6.0)
- **Build:** JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3

## Current starter scope

This first milestone is intentionally small and independent of Firebase or network services:

- Native Android Java application.
- Offline rooftop panel-area estimator.
- Panel wattage validation from 330 W through 700 W.
- Panel surface area, module-gap area, fabrication footprint and preliminary gross roof area.
- Results and last-used inputs saved locally on the device.
- GitHub Actions workflow to build a debug APK.

The initial calculator is a geometric estimator only. It is not an IS 875 wind design, structural verification, approved fabrication drawing or construction release.

## Build the APK on GitHub

1. Open the repository's **Actions** tab.
2. Select **Build GenSolar Pro Engineering APK**.
3. Select **Run workflow** on branch `main`.
4. Open the completed run and download `GenSolar-Pro-Engineering-debug-APK` under Artifacts.
5. Extract the artifact ZIP to obtain `app-debug.apk`.

The workflow also runs after source-code pushes to `main` (documentation-only changes are ignored).

## Project structure

```text
.
├── .github/workflows/android-apk.yml
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gensolar/engineering/
│       │   ├── MainActivity.java
│       │   └── RoofAreaCalculator.java
│       └── res/values/styles.xml
├── docs/
│   ├── ARCHITECTURE.md
│   └── REFERENCE_REQUIREMENTS.md
├── build.gradle
├── gradle.properties
└── settings.gradle
```

## Planned next milestones

1. Project records and roof-zone management using a local database.
2. Panel catalogue and configurable portrait/landscape layouts.
3. Roof geometry import and 2D layout.
4. Location, solar-resource and shadow analysis.
5. IS 875 wind-load calculation module with traceable code inputs and engineer review.
6. RCC, metal-shed, elevated and multi-structure design workflows.
7. Fabrication details, BOQ/BOM, CAD and DPR/PDF exports.
8. Optional cloud sync after the offline workflow is stable.

## Reference-based feature selection

See `docs/REFERENCE_REQUIREMENTS.md` for the comparison of `gensolar-pro` and `Roka-One`, selected engineering-only requirements, excluded CRM/cloud scope, and phased implementation plan.

## Safety and engineering limitations

The current roof-area calculator is a preliminary geometric estimator. It does not account for roof-edge setbacks, walkways, obstacles, shading, irregular roof polygons or multiple roof elevations. It does not calculate wind loads, structural member sizes, anchors or foundations. Engineering outputs must be reviewed by a qualified engineer before procurement, fabrication or construction.

## Reuse and separation policy

The existing `VEERA0007/gensolar-pro` and `VEERA0007/Roka-One` repositories remain unchanged. Reuse is selective: inspect and port only verified calculation logic, UI concepts and assets. Do not copy Firebase configuration, signing credentials, package IDs, release updater code or customer data into this application. This app uses the separate package ID `com.gensolar.engineering`.
