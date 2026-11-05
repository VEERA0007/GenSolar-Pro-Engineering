# Reference comparison and selected requirements

Reviewed repositories (read-only):
- `VEERA0007/gensolar-pro` (`main`)
- `VEERA0007/Roka-One` (`main`)

Both repositories contain the GenSolar Pro Android application under `gensolar-android-project/gensolar-android`. Roka-One is the more recent and expanded source baseline. The new `GenSolar-Pro-Engineering` app is not a fork or full copy of either repository. It takes only the engineering requirements relevant to a separate offline-first engineering tool.

## Comparison

| Area | gensolar-pro | Roka-One | Decision for Engineering app |
|---|---|---|---|
| Solar estimator | Residential/commercial/industrial estimate inputs, tariff/yield and savings assumptions | Retains estimator and expands shared platform plan | Keep only engineering-side sizing inputs and assumptions; do not rebuild sales estimator first |
| Basic site engineering | Site name, state, coordinates, capacity, module details, tilt/azimuth, resource fields, checklist and project stages | Retains site workspace and adds EPC engineering path | Include site/project records, roof zones, survey inputs and engineering checklist |
| PV/electrical sizing | Panel count, preliminary generation and DC/AC inputs | Roadmap identifies PV, inverter, battery, cable and protection calculations | Include as separate, testable calculation modules with assumptions and source notes |
| EPC workflow | Limited project stages | EPC tabs for site, layout, drawing, electrical, BOM and documents; engineering/procurement/construction stages | Adopt an engineering project workflow; exclude CRM/lead sales pipeline |
| Auto layout | Basic area/capacity estimates | Test script checks Auto Design, panel count, SVG concept layout and preliminary BOM/cable schedule | Include 2D roof layout and BOM in phases; mark output conceptual until engineering checks exist |
| CAD/site drawing | Not evidenced in the inspected feature summary | Test script covers DXF LINE parsing, PDF reference underlay and DWG conversion guidance | Include DXF first; PDF as reference underlay; DWG requires external conversion to DXF |
| 3D | Not evidenced in inspected feature summary | Test script checks conceptual 3D building/PV model | Later phase only; label conceptual, not structural/BIM-grade |
| DPR/PDF | Quote/PDF output | Test script checks separate DPR tab, editable BOM/workflow and multi-page print-to-PDF output | Include engineering DPR and PDF export after design/BOM data model stabilizes |
| CRM/auth/roles | OTP, customer/rep/manager, Firestore lead flows | More extensive CRM, role model and cross-platform notes | Exclude from initial Engineering app; no login or cloud dependency |
| Cross-platform | Android focus | Web/PWA, iOS Simulator and Windows wrapper/CI roadmap | Android first; consider other platforms only after core engineering is stable |
| Notifications/cloud | Firebase and lead sync | In-app notifications; cross-device push remains future work | Exclude from offline-first v0.x; optional sync later |

## Selected requirements

### Include in this new app
1. Offline-first Android app with package `com.gensolar.engineering`.
2. Project and site records stored on device; later migrate to Room database with schema migrations.
3. Roof survey: dimensions, roof zones, obstacles, setbacks, photos/notes and orientation.
4. PV module catalogue and configurable module dimensions/wattage.
5. Preliminary module count, roof area, row/column arrangement, tilt/azimuth and DC/AC ratio.
6. Inverter, battery, cable, protection and basic electrical design calculators, each isolated and unit-tested.
7. 2D roof layout with DXF import/export where feasible; PDF drawings may be attached as reference underlays.
8. Preliminary BOM/BOQ, project costing inputs and editable engineering schedules.
9. Engineering DPR with project summary, design inputs, assumptions, calculations, layout, BOM and review/approval sections.
10. PDF generation and local file sharing/printing.
11. Validation states: INCOMPLETE / REVIEW / READY FOR ENGINEER REVIEW. Never imply structural or statutory approval automatically.

### Exclude from initial scope
- Customer login, Firebase OTP, user roles, CRM leads, sales assignments and follow-up reminders.
- Cloud database, cross-device sync, push notifications and automatic online tariff/resource claims.
- Sales dashboards, marketing website, iOS and Windows packaging.
- Production structural certification, stamped drawings or automatic approval.

## Implementation sequence

- **0.1 foundation:** native Android shell, offline roof-area calculator, local settings and GitHub Actions debug APK.
- **0.2 project data:** Room database, project list, site/roof zones, survey checklist and export/import backup.
- **0.3 PV layout:** module catalogue, portrait/landscape placement, clearances, walkways, obstacles and editable 2D layout.
- **0.4 electrical:** inverter selection, string sizing, DC/AC checks, cable/protection schedules and battery sizing where applicable.
- **0.5 CAD and 3D:** DXF parser/import, PDF underlay, SVG layout export, conceptual 3D preview. DWG conversion is not native in the first CAD milestone.
- **0.6 documents:** BOM/BOQ, cost sheet, multi-page engineering DPR, PDF export and print QA.
- **0.7 engineering QA:** unit tests, reference cases, boundary checks, assumptions/source dates and qualified engineer review.
- **Later:** optional cloud sync, authentication and cross-platform packaging only if separately approved.

## Evidence and limitations

The Roka-One repository's `tests/gensolar-webview-smoke.mjs` contains checks for the DPR workflow, EPC tabs, Auto Design, SVG concept layout, preliminary BOM/cable schedule, DXF line parsing, PDF underlay and DWG conversion messaging. These are test definitions in the repository; they are not a claim that those checks were executed during this review or that the resulting tools are production-grade CAD. The Roka-One roadmap itself describes remaining work and calls for engineering review.

The inspected `gensolar-pro` feature summary documents the basic engineering/site-design workspace and explicitly treats solar resource values as user-entered and outputs as preliminary. That conservative boundary is retained.

## Source separation and security

- Keep both reference repositories unchanged.
- Do not copy Firebase configuration, Google service files, signing keystores, API keys, user data or release updater code.
- Do not reuse the `com.gensolar.pro` package ID.
- Port only individual calculation logic/assets after reviewing and testing them.
- Keep formulas in pure Java modules with unit tests and clear assumptions.
