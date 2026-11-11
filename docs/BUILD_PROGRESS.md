# GenSolar Pro Engineering — Build Progress

Last assessed: 2026-11-11

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
| Shade engine | 15% | Simplified obstacle/shadow screen; sun-path time series, 3D ray tracing and module/string mismatch modelling remain |
| CAD and drawings | 47% | Concept CAD editor with selection/move, polygon-interior and line-segment hit testing, duplicate/90° rotate tools, optional endpoint/vertex object snapping (new test pending), layer visibility, unit-aware DXF-to-CAD import, and conceptual 3D roof/wall/obstacle/PV layer rendering; underlays, advanced editing, SLD and fabrication drawing suite remain |
| 3D engineering | 30% | Conceptual 3D roof/building and PV visualization with independent roof, wall, obstacle and PV layer visibility; multi-roof parametric model, structural components and synchronized drawing sections remain |
| Structural and wind | 55% | IS 875-oriented wind inputs/screens, portal frame/STAAD deck, steel take-off and preliminary RCC/foundation/detailing checks; complete code verification and engineer-reviewed release remain |
| Electrical | 65% | String/MPPT, temperature voltage, cable, protection and battery screening with tests; full topology, coordination, earthing/lightning, transformer/HT and complete SLD remain |
| Procurement and construction | 40% | BOM/BOQ, vendor/RFQ and fabrication/commissioning/handover foundations; integrated purchasing, delivery, site execution and inspection workflows remain |
| Documents | 55% | DPR, schedules, PDF/print and backup foundations; revision-controlled drawing register, signatures, acceptance and cross-workspace snapshot assurance remain |
| Quality and verification | 55% | CI syntax/HTML checks, many Node regression tests and APK build/artifact verification; instrumented Android tests, broader end-to-end tests and performance/offline verification remain |

**Calculation:** arithmetic mean of the 14 workspace scores = 45.50%, reported as **46%**.

## Current verified build state

- Build #234: success — DXF preview regression test and APK artifact verified.
- Build #235: success — persistent CAD layer visibility controls compiled and APK packaged.
- Build #237: success — CAD layer visibility regression test, prior checks and APK packaging passed.
- Build #238: success — DXF import geometry connected to editable CAD and APK packaged.
- Builds #239–261: unsuccessful iterations while correcting embedded JavaScript and regression-test harnesses.
- Build #262: success — all configured regression checks passed, including DXF import handoff, 3D layer visibility, CAD entity hit testing, duplicate/rotate behavior, debug APK build, APK verification and artifact upload.
- Verified APK artifact from #262: `GenSolar-Pro-Engineering-debug-APK`, artifact ID `13834650229`.
- Build #263/#264: runs for optional CAD object snapping and its regression coverage.
- Build #265: latest workflow run for the corrected object-snap test harness; pending.

Object snapping is optional and snaps nearby drawing points to visible geometry vertices/endpoints. It ignores hidden layers. It is not yet counted as verified until its regression test and APK build pass.

A green CI build verifies the checks configured in GitHub Actions and APK packaging. It does not mean the full EPC application is complete or validated on a physical Android device.

## Completion gates before Android field execution

1. Complete and verify editable CAD entity workflows, layer controls, geometry import/export and roof polygon handoff.
2. Complete multi-roof 3D and PV placement synchronization.
3. Replace simplified shade screen with a documented sun/shadow and loss workflow.
4. Close remaining solar-resource, electrical SLD/protection and structural engineering gaps.
5. Integrate procurement, construction, QA, commissioning and document revision control around one project revision.
6. Pass the expanded automated suite, then Android device tests for offline launch, persistence, file import/export, GPS, print, rotation and relaunch.
7. Publish a release APK only after all release gates pass. Structural/electrical outputs still require qualified professional review where applicable.
