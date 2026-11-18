# Architecture and engineering guardrails

## Current application architecture

- Native Android host and navigation: Kotlin 2.0.21 with Jetpack Compose and Material 3.
- EPC engineering workspace: offline-first HTML/CSS/JavaScript asset hosted in an Android WebView while the native Compose shell owns app navigation and native entry screens. The WebView remains the host for existing tested calculation engines and advanced legacy workspaces during the staged migration.
- Native Compose workspaces: Home, site survey, PV design, electrical, CAD/building setup, shade analysis, GPS/solar resource, STAAD structural inputs, BOM/BOQ and the initial Measurement & 3D Survey toolkit.
- Measurement toolkit v0.3.0: pure Kotlin calculations for 3D coordinate distance, GPS haversine distance, room/object geometry and preliminary sloped-roof/PV module estimates. ARCore 1.51.0 is optional and retains the app's minSdk 23; AR runtime functionality itself requires a supported device and Android API 24+. Live camera rendering, AR hit testing, depth sampling and 3D scan persistence are not yet implemented. Native BOM rate editing and CSV export use Android's document-creation flow.
- Controlled JavaScript bridge: `window.GenSolarEpcBridge` is created inside the EPC runtime and exposes explicit navigation, project/CAD snapshots, save, stale-result invalidation, STAAD generation and BOM read/write methods. Kotlin must not reference private IIFE variables/functions such as `state`, `model`, `save` or `staadGenerate` directly. Add new bridge methods deliberately and cover them in the Python modernization validator.
- Engineering calculation core: pure Java/Kotlin-compatible calculation classes and tested JavaScript engineering modules; keep calculation inputs/units explicit and validation independent from UI. Do not replace a tested solver with a UI approximation.
- Python: CI engineering-quality gates, source audits, regression fixtures and deterministic engineering/report automation. Python is not presented as an Android runtime language.
- Local project persistence: existing workspace browser storage and project-library snapshots remain the source of truth while native Compose screens read/write through the bridge; robust database/revision synchronization remains future work.
- Network: optional for map tiles and externally sourced resources; core workspace remains available offline.
- Build: GitHub Actions, JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01 and Material 3 1.3.1.

## Native UI / UX modernization

- Compose Material 3 app bar, bottom navigation, dashboard and native workspace forms provide the primary Android interaction surface.
- Native Compose screens are connected to the existing engineering data through `window.GenSolarEpcBridge`; the calculation engines and advanced fabrication/vendor/procurement/construction/DPR views remain in the WebView until their native replacements are complete.
- BOM generation validates source inputs, preserves previously entered rates for matching material lines, persists the schedule in the shared offline project and exports CSV using Android `ACTION_CREATE_DOCUMENT`.
- File selection and location permission requests use Android activity-result APIs. Native CSV export writes through `ContentResolver` after the user selects a destination.
- Local WebView access is retained for bundled offline assets; mixed content and universal/file-origin cross-access are disabled.
- Python CI checks guard Kotlin/Compose setup, launcher wiring, bridge contracts, WebView security settings, HTML ID uniqueness and navigation targets. Node regression tests cover BOM quantity derivation, invalid-input guards, rate preservation and save behavior.


## Calculation definitions

- Panel count = ceiling(target kW × 1000 / module W).
- Panel area = panel count × module length × module width (metres).
- Gap area is estimated from a rectangular grid and the user-entered clear gap.
- Fabrication footprint = panel area + estimated internal gap area.
- Preliminary gross roof area = fabrication footprint × user-selected planning factor.
- Square feet = square metres × 10.7639104167.

The current grid calculation assumes uniform panel orientation and equal horizontal/row gaps. Edge margins, walkways, roof obstacles, shading setbacks, irregular polygons and multiple roof elevations are not included yet.

## Engineering guardrails

- Never use a fixed 300 mm module gap as a universal rule.
- Weather history is not a substitute for code-prescribed design wind speed.
- Do not infer structural capacity from satellite imagery or building floor count.
- Wind coefficients, terrain, topography, roof zones, canopy effects, load combinations and connection checks must be implemented from controlled applicable standards.
- A PASS state requires complete inputs and validated calculations. Missing inputs must result in REVIEW, not PASS.
- Structural design and fabrication release require qualified engineer review.

## Future package boundaries

`site/`, `roof/`, `catalog/`, `solar/`, `shadow/`, `wind/`, `structures/`, `cad/`, `fabrication/`, `documents/`, and `sync/` should be introduced as independent modules as the features are implemented. Keep formulas in pure, unit-tested classes and keep UI code out of the engineering core.
