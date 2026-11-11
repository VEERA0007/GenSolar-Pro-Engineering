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
| CAD and drawings | 49% | Editable CAD selection/move, polygon/line hit testing, duplicate/rotate, object snapping, orthogonal drawing, unit-aware DXF import, survey-image underlay with scale/origin/opacity controls, persistent visibility and conceptual 3D roof/wall/obstacle/PV rendering; PDF/DWG vector conversion, advanced editing and fabrication drawing suite remain |
| 3D engineering | 30% | Conceptual 3D roof/building and PV visualization with independent roof, wall, obstacle and PV layer visibility; multi-roof parametric model, structural components and synchronized drawing sections remain |
| Structural and wind | 55% | IS 875-oriented wind inputs/screens, portal frame/STAAD deck, steel take-off and preliminary RCC/foundation/detailing checks; complete code verification and engineer-reviewed release remain |
| Electrical | 65% | String/MPPT, temperature voltage, cable, protection and battery screening with tests; full topology, coordination, earthing/lightning, transformer/HT and complete SLD remain |
| Procurement and construction | 40% | BOM/BOQ, vendor/RFQ and fabrication/commissioning/handover foundations; integrated purchasing, delivery, site execution and inspection workflows remain |
| Documents | 55% | DPR, schedules, PDF/print and backup foundations; revision-controlled drawing register, signatures, acceptance and cross-workspace snapshot assurance remain |
| Quality and verification | 55% | CI syntax/HTML checks, many Node regression tests and APK build/artifact verification; instrumented Android tests, broader end-to-end tests and performance/offline verification remain |

**Calculation:** arithmetic mean of the 14 workspace scores = 45.64%, reported as **46%**.

## Current verified build state

- Build #276: success — all configured regression checks, including CAD survey underlay scale/import validation, debug APK verification and artifact upload passed. APK artifact ID `13860348660` (expires 2026-11-25).
- Build #271: success — survey-image underlay scaling, CAD layer visibility, full configured regression suite, debug APK verification and artifact upload passed. APK artifact ID `13859008893` (expires 2026-11-25).

- Build #234: success — DXF preview regression test and APK artifact verified.
- Build #235: success — persistent CAD layer visibility controls compiled and APK packaged.
- Build #237: success — CAD layer visibility regression test, prior checks and APK packaging passed.
- Build #238: success — DXF import geometry connected to editable CAD and APK packaged.
- Builds #239–261: unsuccessful iterations while correcting embedded JavaScript and regression-test harnesses.
- Build #262: success — full configured regression suite, debug APK build, APK verification and artifact upload passed.
- Build #265: success — full configured regression suite including CAD object snapping, debug APK build, APK verification and artifact upload passed.
- Verified APK artifact from #265: `GenSolar-Pro-Engineering-debug-APK`, artifact ID `13834732793`.
- Build #266: orthogonal line/polyline drawing mode and regression tests passed.
- Build #267: orthogonal CAD drawing regression tests passed; debug APK verification and artifact upload passed. Latest verified run: #267, commit `e8fbf11226c914f451b5bb181f63c72369644f9b`.

Object snapping is verified: nearby drawing points snap to visible geometry vertices/endpoints, with hidden-layer filtering and an off switch. Survey image underlays are aligned in drawing metres and rendered beneath editable CAD geometry; they remain visual references and do not create surveyed coordinates automatically. CAD JSON import now validates underlay image data types, payload size, dimensions, origin, calibrated width and opacity before applying it. Orthogonal drawing is implemented for line, dimension and polyline segments; regression tests and APK build passed. CAD now supports an image survey underlay with user-controlled scale width, origin, opacity, visibility and local project persistence.

PV layout DXF export now declares `$INSUNITS=4` (millimetres), matching the exporter's metre-to-millimetre coordinate conversion. A new CI regression test round-trips exported roof zones, PV modules, obstacles and access corridors through the DXF parser and checks unit metadata and geometry scale. Builds #277 and #278 were triggered while validating this change; #278 includes the new round-trip test.

A green CI build verifies the checks configured in GitHub Actions and APK packaging. It does not mean the full EPC application is complete or validated on a physical Android device.

## Completion gates before Android field execution

1. Complete and verify editable CAD entity workflows, layer controls, geometry import/export and roof polygon handoff.
2. Complete multi-roof 3D and PV placement synchronization.
3. Replace simplified shade screen with a documented sun/shadow and loss workflow.
4. Close remaining solar-resource, electrical SLD/protection and structural engineering gaps.
5. Integrate procurement, construction, QA, commissioning and document revision control around one project revision.
6. Pass the expanded automated suite, then Android device tests for offline launch, persistence, file import/export, GPS, print, rotation and relaunch.
7. Publish a release APK only after all release gates pass. Structural/electrical outputs still require qualified professional review where applicable.
