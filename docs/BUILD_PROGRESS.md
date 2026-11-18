# GenSolar Pro Engineering — Build Progress

## Latest implementation (2026-11-19 — measurement history management)

- Added individual measurement deletion and a confirmed clear-all action to the native Measurement & 3D Survey history screen. The UI refreshes from app-private storage only after the synchronous persistence operation succeeds; failed writes leave the existing records in place.
- Added CSV-export reminder before destructive clear-all, and retained the existing 200-record bound and spreadsheet-formula-safe CSV export.
- Added a pure Kotlin regression test confirming selective deletion preserves all other records and their order.
- Commits: [store deletion/clear API](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/f79d8248208e576461b888c2a74fe4910b6b4783), [Compose history controls](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/b2857c31bd7b39e366c171e2e58f2e90bcc97842), [history deletion test](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/cef1662fc761fb76e7866719f1db734f87e44dd3), and [unused callback cleanup](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/e92c9c4a25d2ea7b3a2283a6994156e37601ab50).
- CI status for the final commit has not yet been reported by the connected status endpoint. Do not count this slice as build-verified until the Android Actions run completes successfully and its artifact is inspected.

## Latest implementation (2026-11-17 — Measurement & 3D Survey foundation)

- Added a native Jetpack Compose Measurement & 3D Survey workspace to the dashboard on `main`, with Distance, Object, Room, Solar roof and GPS tools.
- Added pure Kotlin measurement helpers for 3D point distance, GPS haversine distance, sloped rectangular roof surface area and preliminary module-count estimation, with unit tests.
- Added ARCore availability reporting, the optional ARCore manifest declaration and camera capability declarations. ARCore 1.57.0 initially failed manifest merge because its AAR required minSdk 24 while this application retains minSdk 23. The dependency was corrected to ARCore 1.51.0, preserving minSdk 23; AR runtime operation still requires Android API 24+ and a supported device.
- Live camera point-picking, Depth API sampling, room/roof scanning, 3D mesh generation, measurement history integration and report export are explicitly pending. The current screen is a usable calculator/GPS foundation, not a completed AR scanner.
- GitHub Actions Build #543 passed the Kotlin/HTML checks, configured engineering regression tests, debug APK packaging, APK signature/package verification and artifact upload. Artifact ZIP: 9,270,287 bytes; SHA-256: `e2b3c4f46dc12b83798a979fa0f1116e00220a1c82f45f5996d6a3dea371e68a`. [Build #543](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/44034496433).
- The existing 14-workspace overall completion estimate is not changed by this foundation-only addition; the live AR scanner remains a distinct incomplete feature slice.


Last assessed: 2026-11-15 (native Compose CAD editing and transactional project bridge implemented; full validation of the new CAD slice pending)

## Overall completion estimate: 49.1%

This is a feature-scope estimate, not an APK build percentage, release-readiness claim or engineering certification. The 14 required workspaces in `REFERENCE_REQUIREMENTS.md` are equally weighted (1/14 each). Each score reflects implemented and testable scope within that workspace; partially implemented items receive partial credit. The native supplier quotation workspace and RFQ → quotation → PO traceability are verified by GitHub Actions Build #468: all 80 workflow steps, Kotlin/HTML validation, configured engineering regressions, APK build, APK verification and artifact upload succeeded. The debug APK is 9,265,736 bytes (about 8.83 MiB); its workflow artifact ZIP is 8,857,010 bytes (about 8.45 MiB). Build #342's APK was 9,104,692 bytes, so the new APK is 161,044 bytes larger. This is a successful debug build, not a release or physical-device certification.

| Required workspace | Estimated completion | Current evidence / principal gap |
|---|---:|---|
| Project control | 42% | Local project save/load, JSON/ZIP backup foundations and a controlled native project bridge; robust revision history, audit trail, conflict-safe restore and cross-workspace snapshots remain |
| Geospatial site | 43% | Native Compose GPS/resource screen, Android permission flow and bridge-backed project synchronization; multi-building survey geometry, elevation/source metadata and offline map layers remain |
| Roof survey | 38% | Native Compose survey form, local draft persistence and shared project transfer; full roof-zone geometry, all roof types and evidence workflow remain |
| Module/equipment catalogue | 60% | Sourced module records and electrical parameters; complete inverter, battery, protection and vendor catalogue coverage remains |
| Automatic/manual PV layout | 68% | Native Compose inputs connected to the existing polygon optimizer, setbacks, obstacles, orientation and module schedules; multi-zone optimization, pitch/tilt and comprehensive constraints remain |
| Solar resource and yield | 35% | Resource inputs, loss/yield screens and monthly forecast foundations; validated sourced weather/resource integration and full yield model remain |
| Shade engine | 25% | Native Compose interface connected to timezone-aware solar position, day-profile and directional obstruction screening with regression tests; 3D ray tracing, horizon profiles, diffuse shading and module/string mismatch modelling remain |
| CAD and drawings | 58% | Native Compose building inputs connected to the shared CAD model; editable CAD selection/move, snapping, unit-aware DXF, survey-image underlay and conceptual 3D rendering remain partly WebView-backed; PDF/DWG conversion, advanced editing and fabrication drawings remain |
| 3D engineering | 30% | Conceptual 3D roof/building and PV visualization with independent layer visibility; multi-roof parametric model, structural components and synchronized drawing sections remain |
| Structural and wind | 58% | Native Compose STAAD geometry/load inputs connected through the controlled bridge to the existing portal-frame model and STAAD export; complete code verification and engineer-reviewed release remain |
| Electrical | 68% | Native Compose string-validation inputs connected to the existing temperature, MPPT, cable, protection and battery screening; full topology, coordination, earthing/lightning, transformer/HT and complete SLD remain |
| Procurement and construction | 50% | Native BOM/BOQ, RFQ creation, invited-supplier quotation traceability, RFQ-validated PO creation, PO status lifecycle, goods receipt reconciliation and audit-history export are integrated; BOM-linked multi-line purchasing, delivery tracking, construction inspections and field QA remain |
| Documents | 55% | DPR, schedules, PDF/print and backup foundations; revision-controlled drawing register, signatures, acceptance and cross-workspace snapshot assurance remain |
| Quality and verification | 58% | CI HTML/Kotlin checks, engineering Node regressions, controlled-bridge validation and new BOM quantity/rate-preservation tests; latest push run, instrumented Android tests and broader end-to-end/offline/performance verification remain |

**Calculation:** arithmetic mean of the 14 workspace scores = 48.57%, reported as **48.6%**.

## Latest implementation (2026-11-13)

- Added a controlled, immutable `window.GenSolarEpcBridge` contract inside the existing EPC JavaScript runtime. Native Compose screens now use explicit methods for navigation, project summaries, CAD snapshots, offline save, stale-result invalidation, STAAD generation and BOM read/write. This fixes the previous scope mismatch where Kotlin attempted to call private variables and functions inside the JavaScript IIFE directly.
- Routed native PV layout, electrical, STAAD, GPS/resource and CAD screens through that bridge. GPS synchronization now reports success only after the shared project save returns successfully.
- Added a native Jetpack Compose BOM/BOQ workspace. It loads the saved schedule, builds material quantities from the active PV layout and engineering state, preserves rates when the schedule is rebuilt, saves edited rates to the same offline project, and exports a CSV through Android's `ACTION_CREATE_DOCUMENT` flow.
- Hardened BOM generation: invalid module/row counts, module rating, inverter capacity, cable route length or MPPT count stop generation instead of writing invalid quantities. Existing BOM rates are retained for matching item/specification/unit rows.
- Added `tools/test_bom_generation.js` to verify all 12 material groups, route allowances, string-fuse and MPPT quantities, invalid-input guards, rate preservation and save behavior. Added native-bridge/BOM assertions to the Python modernization validator and a dedicated GitHub Actions test step.
- The existing engineering solvers and CAD/DXF/STAAD exports remain the calculation source. The Compose layer is now connected through a controlled bridge; this is still a staged migration, not a claim that the solver core has been rewritten in Kotlin. Advanced fabrication/vendor comparison and procurement, construction and DPR workspaces remain WebView-backed.

## Latest implementation (2026-11-14)

- Hardened DXF import for legacy `POLYLINE` entities: 2D polylines remain editable, while DXF 3D polylines and polygon-mesh/polyface-mesh variants are skipped rather than silently flattened into 2D geometry. This prevents loss of Z/elevation information during import.
- Added a regression fixture containing a closed 2D legacy polyline and an unsupported 3D polyline. The existing assertions continue to verify imported entity counts, closure flags, layers, units and extents; the unsupported 3D entity must not appear in the imported geometry.
- Commits: [`42a122b`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/42a122b3f6be4f6c50ecc941a47a4af38e9f7e04) and [`15ac15b`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/15ac15b123d55b17c54d84982dd86df49b01b2df). GitHub Actions status for the latest commit had not reported a check result at the time of this update; this change is not yet counted as CI-verified.

## Latest implementation (2026-11-14 — procurement controls)

- Hardened purchase-order status changes with an explicit transition policy. Receipt-derived states (`Partially received` and `Received`) can only be reached through validated goods receipts; terminal `Received` and `Cancelled` orders cannot be reopened through the manual status control.
- Added save-failure rollback: if local project persistence fails, the previous PO status and audit-history length are restored and the user receives a clear failure message. The UI register and lifecycle history are refreshed after successful persistence or rollback.
- Expanded `tools/test_po_lifecycle.js` for allowed transitions, invalid/receipt-owned states, reverse-transition rejection, audit history and persistence rollback. The existing GitHub Actions workflow already runs this test file.
- Commits: [`220d79a`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/220d79a2ef3fe7c25147ca63613d3285cb290d3c) and [`7a8f418`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/7a8f4186390b7db6e780af0ec2fe229e20e9d1d3). The latest commit currently has no reported CI status; do not count this implementation as build-verified until the workflow completes.

## Latest implementation (2026-11-14 — native supplier quotation workspace)

- Added a Kotlin/Jetpack Compose supplier quotation register to the native Procurement workspace. Users can enter supplier, quoted amount, delivery days and commercial/warranty notes; quotations are read from and saved into the same active offline project through the controlled EPC bridge.
- Added bridge-side validation for blank supplier names, non-positive/non-finite quote values, negative or fractional delivery days, case-insensitive duplicate supplier names and failed project persistence (rollback). Native comparison sorts by quoted price and then delivery time, displays the premium over the lowest offer and clearly warns that lowest price alone is not an award decision.
- Extended `tools/test_native_procurement_bridge.js` for valid quote creation, invalid fields, duplicate suppliers and save rollback. GitHub Actions Build #451 passed the new bridge test, all existing engineering regressions, Kotlin/HTML validation, debug APK build, APK verification and artifact upload.
- Source commits: [`fa1bbf8`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/fa1bbf8892c7fa02b69eaedd22278f5336dbf021), [`c0976a3`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/c0976a34b599c94fb9f6f9b2840a31a8d34d048d), [`4d2f1ce`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/4d2f1cec7d7f7ec0a4e7dc92733fea6f5495b665), and workflow/test commit [`38f7564`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/38f75647c04fd65356feb6ebd7a71c9b6dad1ab5).
- Build #451: [successful Android APK workflow](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/43626269355). APK SHA-256: `b7590f23354e55311c02326bdbe1e4ee5cc560f341cace1a3bb231462c2c214a`.

## Latest implementation (2026-11-14 — supplier quote comparison)

- Reworked supplier quotation display into a ranked comparison: lowest quoted amount first, then shorter delivery time for equal-price offers. The table shows rank, quoted amount, difference from the lowest offer, delivery days and escaped supplier notes. The screen explicitly reminds users to review technical specifications, warranty and commercial terms before award; lowest price is not treated as an automatic award decision.
- Made quote creation transactional. Invalid entries are rejected before mutation, and a failed/throwing project save rolls the newly added quotation back instead of leaving unsaved data visible in the project.
- Expanded `tools/test_procurement_register.js` to cover ranking, tie-break ordering, price differences, HTML escaping and save-failure rollback. The existing Android Actions workflow already executes this test.
- Commits: [`ad48c93`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/ad48c93548e24d299454276df4423d63bde04874) and [`43b6c7e`](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/43b6c7e94e853bb69b3f54d78374683fa8c85264). CI verified the supplier-comparison source and test commit in Build #447 (success); the documentation-only follow-up commit is not a separate APK build.

## Latest implementation (2026-11-14 — native RFQ and PO lifecycle)

- Added a native Compose RFQ register in Procurement. It creates project-linked requests with item, quantity, unit, due date and invited suppliers; duplicate supplier invitations and invalid calendar dates are rejected.
- Added deterministic UTC calendar-date validation for RFQ due dates. The earlier over-escaped date check caused the RFQ regression test to fail; the validator was replaced with explicit year/month/day parsing and calendar round-trip checks.
- Added native PO status controls for Draft, RFQ sent, Approved, Ordered, On hold and Cancelled. The bridge enforces allowed transitions; Partially received and Received remain receipt-derived and cannot be selected manually.
- Goods receipts now append PO status transitions to the audit history, and failed persistence restores both the previous PO status and history length. The native screen displays the latest 25 audit entries and exports the full status history to CSV.
- Added bridge and UI assertions for the native status workflow. The test now re-reads the procurement snapshot after mutations because the bridge intentionally returns cloned snapshots; retaining an earlier array reference produced a false-negative assertion.
- Main-branch source commits: [RFQ bridge](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/fe62b4866a69306984e6ff64fbf347b153bd28ce), [native RFQ screen](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/6ff2207771341545c2788eca30b61ebb8e286207), [PO lifecycle UI/bridge](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/0f8cf7f62f66e4754740d492f77d3249028bb58e), [deterministic RFQ date validation](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/8e9da4c1ca42009ef04266e37e464f188c572cfa), and [fresh-snapshot lifecycle regression](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/6086f8a58e46ec090dc29431832a6dc454184c78).
- Build #461 currently reports all 73 preceding validation/test steps successful. APK compilation is still in progress; APK verification and artifact upload have not completed. The procurement changes are not counted as APK-verified yet.

## Current build verification state

- **Current main HEAD:** [87a6c419e60602e68d3a4a13c0f1d524efde06b8](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/e24d419ab967cd99fcf226cd69f41aeecce76d21).

- **Latest successful APK build: Build #455**, [workflow run](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/43627106003), source commit [6ff2207771341545c2788eca30b61ebb8e286207](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/6ff2207771341545c2788eca30b61ebb8e286207). Kotlin/HTML validation, configured tests, APK verification and artifact upload succeeded. Artifact ID 14126986551; ZIP size 8,867,670 bytes; extracted app-debug.apk size **9,282,352 bytes (8.85 MiB)**; APK SHA-256 ae54092fbfad28229c67c74e82af3da020c44fa7ceae29345f045695cfec407e. [Download Build #455 artifact](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/43627106003/artifacts/14126986551).
- Build #455 predates the native PO status lifecycle. It contains the native RFQ workspace but does not verify the latest PO lifecycle code. Use Build #461's artifact only after its verification and upload steps succeed.
- Earlier successful Build #451 and Build #342 remain historical checkpoints; Build #297 is older still and does not contain the current native bridge/BOM work.

A green CI build verifies only the checks configured in GitHub Actions and APK packaging. It does not replace physical Android device testing, field validation or qualified engineering review.

## Next implementation

1. Link the native RFQ/quotation/PO workflow to BOM-derived multi-line purchase orders, vendor award records, delivery milestones and stock receipt reconciliation.
2. Migrate construction/QA natively: activity tracking, inspection evidence, commissioning measurements, corrective actions and customer handover.
3. Migrate DPR/document control natively with revision-controlled project snapshots, drawing registers and export/print verification.
4. Migrate CAD editing to native Compose: entity selection, move/duplicate/rotate/delete, layer visibility and a native 2D preview backed by the existing CAD model; then complete multi-roof 3D, STAAD and electrical integration gaps without replacing tested solvers.
5. Verify the latest main-branch Actions run, inspect the complete logs, verify APK packaging and upload, then test offline launch, persistence, CSV/DXF import/export, GPS, print, rotation and relaunch on Android hardware.
6. Publish a release APK only after all planned features are implemented, integrated, tested and approved. Never report 100% before these gates pass.

## Completion gates before Android field execution

1. Complete editable CAD entity workflows, layer controls, geometry import/export and roof polygon handoff.
2. Complete multi-roof 3D and PV placement synchronization.
3. Complete documented sun/shadow and loss workflows, including the remaining shade-model limitations.
4. Close solar-resource, electrical SLD/protection and structural engineering gaps.
5. Integrate procurement, construction, QA, commissioning and document revision control around one project revision.
6. Pass the expanded automated suite, then Android device tests for offline launch, persistence, file import/export, GPS, print, rotation and relaunch.
7. Publish a release APK only after all release gates pass. Structural/electrical outputs still require qualified professional review where applicable.


## Latest implementation (2026-11-15 — RFQ-to-PO traceability)

- Supplier quotations can optionally reference an existing RFQ. When linked, the bridge rejects unknown RFQs and suppliers who were not invited; supplier matching is case-insensitive.
- Purchase orders can optionally reference the source RFQ. A linked PO is accepted only when the RFQ exists, the supplier was invited, and that same supplier has a quotation linked to that same RFQ.
- The native Compose quotation form captures the RFQ reference and shows the link on quotation cards. The native PO form carries the source RFQ reference forward from the selected quotation and displays it in the PO lifecycle register.
- Regression coverage includes invited/uninvited suppliers, unknown RFQs, case-insensitive supplier matching, missing linked quote, mismatched RFQ references and successful quote-to-PO traceability.
- Source commits: [RFQ-linked quotations](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/499adcef3c2523bcd1fab20f0adf501178310815), [native quote RFQ field](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/3878d803fd639a70ab22d2c1a61fb5e431258f7c), [quotation validation tests](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/8793945bd49fd16908c2ab91fc890f747d9e1991), [PO source-RFQ validation](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/36a3b0b5f919594fce831a3b7b14107b45b1879a), [native PO traceability](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/d2d07b752ed52b82555dd9f0c81ecfa128e6e654), and [end-to-end bridge tests](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/ea51869a305502aa63bdd8efe634d1a35b01a412).
- Build #468: [successful Android APK workflow](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/43629176625), source commit `ea51869a305502aa63bdd8efe634d1a35b01a412`; all 80 steps passed. The verified APK is 9,283,171 bytes; SHA-256 `13c3063833f84ae904fa95d83cc7917b4838386c6acacc5e4fe1ec8fa13a1189`.
- Build #467 also passed on the immediately preceding PO UI commit. Build #468 includes the final procurement bridge regression suite and is the latest verified APK.

## Current build verification state

- **Current main HEAD:** [12af457af3fdcee778796cd8085323d360d320fa](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/12af457af3fdcee778796cd8085323d360d320fa).
- **Latest successful workflow:** [Build #487](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/43637134669), source commit `31a8f1c1cb90e9d4e9f5d46d266da37815223649` (multi-line PO/BOM import). Later procurement changes are under CI validation in Build #510; no APK from those newer changes is marked verified yet.

- CI validates the configured checks and packaging. It does not replace physical Android device testing, field validation or qualified engineering review.


## Latest implementation (2026-11-15 — purchase-order delivery monitoring)

- The native PO reconciliation register now compares each expected delivery date with the device's local date and shows a clear delivery state: overdue by N days, due today, due within seven days, on schedule, delivered, cancelled, on hold or a date-validation warning.
- The alert uses the PO's lifecycle state so completed, cancelled and held orders are not incorrectly labelled overdue.
- The reconciliation CSV now includes a Delivery alert column using the same date/status rules, so exported procurement reports carry the same exception information as the on-screen register.
- The Android modernization validator now checks the receipt-progress summary, overdue/due-today/upcoming alerts and delivery-alert CSV output. An initial validator assertion incorrectly expected the CSV column label as a standalone string; this was corrected to match the actual header text. Build #474 exposed that assertion issue; the corrected validator is included in Build #475.
- Commits: [native delivery alerts](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/66053ae93797a5b7ff8269eac9e109c8d1bb59ad), [delivery-alert CI assertion](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/122d751ca6fdabd28d8f4bc1c80e2e84994f6f79), [CSV delivery alert export](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/b6d649d5ff434cebc7680834e6f6cc2e675ed301), [CSV assertion correction](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/12af457af3fdcee778796cd8085323d360d320fa).
- Build #471: [successful Android APK workflow](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/43635303872), all 80 workflow steps passed. Verified APK size 9,299,548 bytes; SHA-256 `2dda0ac0218580687001d899b624e58479cb5d9da078296cedeb2d81bcdf014d`.
- Build #475 is validating the final CSV export and corrected validator; do not treat its artifact as current until all steps and APK verification succeed.


## Latest implementation (2026-11-15 — item-level receipts, stock ledger and site issue control)

- Native and web procurement now record goods receipts against a specific material line on a multi-line PO. The bridge rejects unknown material names, unit mismatches and receipts above that material's ordered balance; receipt rows retain the material identity.
- The receipt register and CSV export include the material field. The web interface now provides a material selector, and the native Compose screen shows ordered, received and remaining quantities for each PO line.
- The project stock ledger classifies received material by condition: Good, Short received, Damaged and Quarantined. Only Good-condition receipts count as available stock.
- New material issue workflow records a unique issue reference, PO, material, quantity, date, recipient and work package. It prevents issuing more than the remaining Good-condition stock and calculates issued quantity and on-hand balance per material.
- Native and web interfaces expose the issue workflow and material issue register. Failed saves roll back the issue record.
- Regression coverage now checks material-specific receipt limits, stock-ledger balances, issue limits, duplicate issue references, invalid dates and persistence rollback. Build #510 passed the procurement tests and modernization validator. Build #515 includes the corrected native stock-issue handler scope; its full Android workflow is running. The web stock-issue UI test was corrected to exercise the submit action.
- Commits: [item-wise receipt bridge](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/c655192c6e7d706a14b84cb1ebde9d8c2ec8b2eb), [native material receipt controls](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/24e768b6a02f5eada1071683e621531ffcbfd309), [item-wise stock ledger](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/7ad87867c83fdf7503c5e752cc96d172f5129137), [web receipt and stock ledger](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/392fbb7616ad4f9555afae84b20e3051e5b13d70), [stock issue bridge and ledger balances](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/070d3e34710728837410c2db0e39ba907a0c3f5d), [native stock issue workflow](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/f2ac1c2e9a56677ed7869796494ddf56bc7725f4), [stock issue regression tests](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/70345c621bdc54ad736f5983547a03effb292a31), [latest validator correction](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/e24d419ab967cd99fcf226cd69f41aeecce76d21).
- Latest completed baseline: [Build #515](https://github.com/VEERA0007/GenSolar-Pro-Engineering/actions/runs/43638648050), source commit 87a6c419e60602e68d3a4a13c0f1d524efde06b8. All 76 job steps completed successfully, including HTML/Kotlin validation, configured engineering and procurement regressions, debug APK build, APK verification and artifact upload. Artifact ID 14131342499; ZIP size 8,909,120 bytes; extracted APK size 9,319,638 bytes; APK SHA-256 85588d2f7ada3d30795176fcd888e2ae2048525329e42bd9c436328a34b23631. This baseline predates the native CAD editing commits below.

## Latest implementation (2026-11-15 — native CAD 2D editing)

- Replaced the parameter-only native CAD page with a Jetpack Compose editor that reads the active CAD snapshot through GenSolarEpcBridge and renders project geometry on a native Canvas preview.
- Added hit-based entity selection, measured X/Y move offsets, duplicate, 90-degree rotation, delete and per-layer visibility controls. The existing CAD engine remains responsible for geometry hit testing, transforms, DXF workflows and 3D rendering.
- Added transactional bridge operations. Geometry, selected entity, hidden-layer state and undo-stack length are restored if offline CAD persistence fails. CAD localStorage writes now return an explicit result and restore the previous model/catalogue if a multi-key write partially fails.
- Added tools/test_native_cad_editor_bridge.js for snapshot isolation, selection, move, duplicate, rotate, delete, layer visibility, save outcome and persistence rollback. The Python modernization validator and Android APK workflow now run this regression.
- Commits: [CAD project bridge](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/4606f281581bcce2e92894875edaf88efdb2696d), [native Compose editor](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/acb3920377cc764cfc46a42834edf15e5594ce75), [bridge tests](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/238a52786802ab987df6e097940bb1e23d374cfe), [modernization validator](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/5481b3df2b194e938f75245677aeccfcbd90b16a), and [workflow integration](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/611c7de4d50c188df9508a402b7ec485bbfdc05f).
- Additional persistence hardening and regression corrections: [atomic CAD save](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/6c09346b844029cba17429d9f57c8e77148ae8ef), [save rollback tests](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/f880bad46b91cf02a3aeea3b8dd75bfc253139d9), [layer-selection test correction](https://github.com/VEERA0007/GenSolar-Pro-Engineering/commit/087573801085825526774ebcab4f4859b4c18a2c).
- This is a staged native migration: advanced drafting tools, DXF import/export and conceptual 3D rendering remain in the detailed workspace. Full 3D editing, DWG conversion, fabrication drawings and structural release are not claimed.
- The new CAD commits require a fresh full workflow and APK verification; Build #515 is a verified baseline only, not verification of this implementation.

## Latest implementation (2026-11-18 — offline measurement history)

- Added a native saved-measurement register for the Distance, Object, Room, Solar roof and GPS calculators. Users explicitly save a calculated result; the history view shows the latest 50 records and exports all retained records as CSV through the Android document picker.
- Added bounded app-private SharedPreferences persistence (maximum 200 records) with synchronous write confirmation. If stored history is malformed or a write fails, a new record is not reported as saved and existing data is not intentionally replaced.
- Added CSV quoting for commas, quotes and line breaks, plus spreadsheet-formula prefix protection for user-controlled text.
- Added Kotlin unit tests for record validation, CSV escaping, formula protection and export ordering, and extended the Python modernization validator to require the native persistence, history and export wiring.
- The feature is committed to `main`; the GitHub Actions workflow is the verification gate. ARCore camera point picking, Depth API capture, mesh generation and project-linked measurement history remain incomplete.


## Latest implementation (2026-11-18 — native battery bank design)

- Added a native Jetpack Compose battery workspace reachable from the Electrical section and More menu. It loads the active project's saved battery inputs/result, stores native input drafts offline, and writes calculation results back through the controlled EPC bridge.
- Extracted the battery sizing logic into a browser/Node-compatible pure JavaScript engine with strict finite/range checks. It calculates usable energy, required nominal capacity, integer series-module count, parallel strings, total module count, installed kWh/Ah and continuous charge/discharge current capacity.
- The solver checks the entered module continuous discharge-current limit and, when a charger current is supplied, the module charge-current limit. A current-limit shortfall produces REVIEW REQUIRED rather than silently accepting the bank.
- Added Node regression tests for a 3 kW / 8 hour example, 51.2 V bus with 12.8 V modules, 4S × 7P topology, Ah/kWh totals, current overloads, charger limits, non-integer voltage ratios and invalid inputs. Added the test to GitHub Actions and the Python modernization validator.
- The native screen is an engineering sizing and current-capacity screen, not a chemistry/BMS certification or inverter approval. Battery datasheet, BMS, fuse, cable, thermal and manufacturer compatibility checks remain required before release.
- GitHub Actions verification is pending for this implementation.


## Latest implementation (2026-11-18 — Native multi-obstruction shade analysis)

- Native Compose shade workspace now supports a primary obstruction plus up to eight additional objects, validates each row, and calculates combined obstruction alignment/blocking at the selected time.
- Added aggregated day-profile samples and per-obstruction result details using the existing timezone-aware solar-position engine; calculations remain geometric screening, not module-level shade-loss simulation.
- Shade calculation inputs and result/profile snapshots are saved through the shared project bridge and included in project summaries. SharedPreferences retains native input drafts.
- Added regression coverage for multiple obstruction aggregation, blocking intervals, day-profile sample integrity and invalid obstruction counts. CI runs the shade-engine tests explicitly.
- CI verification of this commit is pending.
