# GenSolar Pro Engineering — Build Progress

Last assessed: 2026-11-12 (solar-position and directional-shading implementation; Build #304 pending)

## Overall completion estimate: 46%

This is a feature-scope estimate, not an APK build percentage or a certification claim. The 14 required workspaces in `REFERENCE_REQUIREMENTS.md` are equally weighted (1/14 each). Each score reflects implemented and testable scope within that workspace; partially implemented items receive partial credit. Scores are estimates and must be revised as features are completed and verified.

| Required workspace | Estimated completion | Current evidence / principal gap |
|---|---:|---|
| Project control | 40% | Local project save/load and JSON/ZIP backup foundations; robust revision history, audit trail and conflict-safe restore remain |
| Geospatial site | 40% | GPS capture, coordinates, map pin and resource fields; multi-building survey geometry, elevation/source metadata and offline map layers remain |
| Roof survey | 35% | Basic roof dimensions, polygon geometry, obstacles and setbacks; full roof-zone survey workflow and all roof types remain |
| Module/equipment catalogue | 60% | Sourced module records and electrical parameters; complete inverter, battery, protection and vendor catalogue coverage remains |
| Automatic/manual PV layout | 65% | Preliminary polygon-based placement, setbacks, obstacles, orientation and module schedules; multi-zone optimization, pitch/tilt and comprehensive constraints remain |
| Solar resource and yield | 35% | Resource inputs, loss/yield screens and monthly forecast foundations; validated sourced weather/resource integration and full yield model remain |
| Shade engine | 22% | NOAA-style timezone-aware solar position, configurable day-profile intervals and directional obstruction / shadow-distance screening with regression tests; 3D ray tracing, horizon profiles, diffuse shading and module/string mismatch modelling remain |
| CAD and drawings | 49% | Editable CAD selection/move, polygon/line hit testing, duplicate/rotate, object snapping, orthogonal drawing, unit-aware DXF import, survey-image underlay with scale/origin/opacity controls, persistent visibility and conceptual 3D roof/wall/obstacle/PV rendering; PDF/DWG vector conversion, advanced editing and fabrication drawing suite remain |
| 3D engineering | 30% | Conceptual 3D roof/building and PV visualization with independent roof, wall, obstacle and PV layer visibility; multi-roof parametric model, structural components and synchronized drawing sections remain |
| Structural and wind | 55% | IS 875-oriented wind inputs/screens, portal frame/STAAD deck, steel take-off and preliminary RCC/foundation/detailing checks; complete code verification and engineer-reviewed release remain |
| Electrical | 65% | String/MPPT, temperature voltage, cable, protection and battery screening with tests; full topology, coordination, earthing/lightning, transformer/HT and complete SLD remain |
| Procurement and construction | 40% | BOM/BOQ, vendor/RFQ and fabrication/commissioning/handover foundations; integrated purchasing, delivery, site execution and inspection workflows remain |
| Documents | 55% | DPR, schedules, PDF/print and backup foundations; revision-controlled drawing register, signatures, acceptance and cross-workspace snapshot assurance remain |
| Quality and verification | 55% | CI syntax/HTML checks, many Node regression tests and APK build/artifact verification; instrumented Android tests, broader end-to-end tests and performance/offline verification remain |

**Calculation:** arithmetic mean of the 14 workspace scores = 46.14%, reported as **46%**.


## Latest implementation in progress (2026-11-12)

- Added `shade_engine.js`: timezone-aware solar altitude / azimuth calculation, solar-time conversion, obstruction bearing alignment, shadow-length screening and configurable 15 / 30 / 60-minute day profiles.
- Replaced the earlier shade-screen calculation with the shared engine. The date defaults to the device's local date; the profile reports daylight samples, peak altitude and potential obstruction-alignment samples.
- Added Node regression coverage for Hyderabad summer-solstice sun position, east/west azimuth progression, equivalent UTC / India-local timestamps, directional obstruction screening, profile intervals and invalid inputs.
- The shade engine remains a geometric screening tool. It is not a 3D ray tracer, PVsyst replacement or module-level mismatch / energy-loss model.
- **Build #304 is running** for commit `854b77e6229ba01db521abd24e2b4be379fbae5b`. Do not treat the new APK as verified until the workflow and artifact checks complete.

## Current verified build state

- **Build #297: success** — shared-router navigation for dashboard destinations, Android back-to-Home behavior, new dashboard shortcuts, native shell validation, configured engineering regression suite, debug APK verification and artifact upload passed.
- Build #297 commit: `e3ed7adcd0d68f1794d6f21338c556332adaf9cb`.
- APK artifact: `GenSolar-Pro-Engineering-debug-APK`, ID `13923075759`, size 8,521,207 bytes, SHA-256 `3b7e1e31edfe4127276e6a3b5fd6532fe8f9d8f3d0700acaee6681f2207ed8fc`, expires 2026-11-26.
- **Build #295: success** — native Compose dashboard and workspace launcher, structural solver regression test, configured engineering suite, debug APK verification and artifact upload passed.
- Build #295 commit: `ced912889a77941484e8e9309e91372cdc59959e`; artifact ID `13922697003`.
- Builds #293 and #294 failed during Kotlin compilation because the initial dashboard commit omitted the Compose `Spacer` import. The import was corrected and Build #295 passed.
- Build #292: success — inbuilt 2D portal-frame solver regression test, configured engineering suite, APK verification and artifact upload passed.
- Build #278: success — PV-layout DXF millimetre-unit and round-trip import test, debug APK verification and artifact upload passed.
- Build #267: success — orthogonal CAD drawing regression tests, debug APK verification and artifact upload passed.

The native Android UI includes a Compose Home dashboard, workspace cards for survey, PV design, CAD/3D, electrical, structural, GPS/resource, BOM/BOQ, procurement, construction and DPR, a More menu, bottom navigation and back-to-Home behavior. Navigation now calls the workspace's shared router, fixing destinations such as BOM and STAAD that do not have footer shortcuts. CI validates the route wiring.

The native Home dashboard and Android navigation are implemented, but the engineering workspace content still runs inside the local WebView. The UI migration is therefore partial, not a complete native rewrite. Build #297 verifies compilation and configured tests; it does not replace physical Android device testing or professional engineering review.

A green CI build verifies the checks configured in GitHub Actions and APK packaging. It does not mean the full EPC application is complete or validated on a physical Android device.

## Completion gates before Android field execution

1. Complete and verify editable CAD entity workflows, layer controls, geometry import/export and roof polygon handoff.
2. Complete multi-roof 3D and PV placement synchronization.
3. Replace simplified shade screen with a documented sun/shadow and loss workflow.
4. Close remaining solar-resource, electrical SLD/protection and structural engineering gaps.
5. Integrate procurement, construction, QA, commissioning and document revision control around one project revision.
6. Pass the expanded automated suite, then Android device tests for offline launch, persistence, file import/export, GPS, print, rotation and relaunch.
7. Publish a release APK only after all release gates pass. Structural/electrical outputs still require qualified professional review where applicable.
