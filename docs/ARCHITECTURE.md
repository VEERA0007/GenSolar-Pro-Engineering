# Architecture and engineering guardrails

## Current application architecture

- Native Android host and navigation: Kotlin 2.0.21 with Jetpack Compose and Material 3.
- EPC engineering workspace: offline-first HTML/CSS/JavaScript asset hosted in an Android WebView; the native Compose shell owns the app bar and primary navigation while the existing engineering views remain intact.
- Engineering calculation core: pure Java/Kotlin-compatible calculation classes and tested JavaScript engineering modules; keep calculation inputs/units explicit and validation independent from UI.
- Python: CI engineering-quality gates, source audits, regression fixtures and future deterministic engineering/report automation. Python is not presented as an Android runtime language.
- Local project persistence: existing workspace browser storage and native WebView storage; robust database/revision synchronization remains future work.
- Network: optional for map tiles and externally sourced resources; core workspace remains available offline.
- Build: GitHub Actions, JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01 and Material 3 1.3.1.

## Native UI / UX modernization

- Compose Material 3 app bar and bottom navigation provide native entry points for Home, Survey, PV Design and CAD / 3D.
- A native More menu exposes GPS / Map, Electrical, STAAD Structural, BOM / BOQ, Procurement, Construction and DPR.
- The existing EPC screens are retained inside the WebView to avoid breaking engineering workflows during the UI migration.
- The native WebView bridge is limited to report printing. File selection and location permission requests use Android activity-result APIs.
- Local WebView access is retained for bundled offline assets; mixed content and universal/file-origin cross-access are disabled.
- Python CI checks guard Kotlin/Compose setup, launcher wiring, WebView security settings, HTML ID uniqueness and navigation targets.


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
