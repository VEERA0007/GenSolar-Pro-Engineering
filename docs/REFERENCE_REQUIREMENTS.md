# GenSolar Pro Engineering — Full EPC scope and validation plan

## Product goal

A complete solar engineering workspace, not a collection of disconnected forms. One project model must drive survey, maps, roof geometry, PV layout, shading, CAD, 3D, electrical design, procurement, construction, BOM/BOQ and DPR. A change to a shared input must invalidate and recalculate every dependent result.

## Required workspaces

1. Project control: multiple projects, revisions, dates, customer/site details, audit trail, local backup and restore.
2. Geospatial site: GPS capture, latitude/longitude pins, map layers, elevation, site boundary, multiple buildings/roof zones, north reference, survey photos and source/date metadata.
3. Roof survey: RCC, metal sheet, sloped, elevated canopy and ground mount; polygons, lengths, widths, levels, parapets, setbacks, walkways, obstacles, drains, skylights, access and exclusion zones.
4. PV module and equipment catalogue: manufacturer/model/datasheet, dimensions, Wp, Voc, Vmp, Isc, Imp, temperature coefficients, bifacial parameters, inverter MPPT windows/current/voltage limits, batteries, cables and protection devices.
5. Automatic and manual PV layout: portrait/landscape, orientation, tilt, row pitch, sun clearance, module spacing, setbacks, obstacles, fire paths, roof zones, multiple structures, panel count, DC kWp, usable area and area balance.
6. Solar resource and yield: sourced GHI/DNI/DHI/GTI, weather record, plane-of-array assumptions, PR, losses, monthly/annual yield, specific yield and CUF.
7. Shade engine: date/time/location sun position, sun-path chart, obstacle geometry, horizon profile, 3D shadow casting, per-module/string shading, time-series shade map and loss assumptions. Clearly label the accuracy level of each model.
8. CAD and drawings: editable layers/entities, DXF import/export, PDF/image underlays, drawing scale and units, dimensioning, snapping, selection/editing, roof plan, module plan, cable routing, elevations, sections, SLD and fabrication sheets. DWG/SKP import requires a verified converter/library; do not claim unsupported formats are editable.
9. 3D engineering: navigable model of buildings, roof zones, modules, mounting rails, supports, obstructions and cable routes; 2D/3D synchronization; sections/elevations and drawing sheets.
10. Structural and wind: site wind basis, terrain/topography/height/roof zones, applicable IS 875 Part 3 workflow, load combinations, member sizing, connection/foundation checks and review status. No automatic PASS when required inputs or engineering verification are missing.
11. Electrical: string sizing, cold Voc, hot Vmp, MPPT current, DC/AC ratio, inverter allocation, cable ampacity and voltage drop, protection coordination, earthing, lightning, transformer/HT interface and SLD.
12. Procurement and construction: BOQ, BOM, rates, vendor RFQs/comparisons, purchase orders, delivery, site activities, inspections, testing, commissioning and handover.
13. Documents: engineering calculation report, drawing register, BOM/BOQ, DPR, SLD, PDF print/export, revision/date/sign-off and customer acceptance.
14. Quality: units, assumptions, data provenance, calculation version, warnings, test cases, input validation, offline behavior and performance.

## Shared data contract

- Project is the root record; all workspaces read/write the same versioned project.
- Each roof zone has stable ID, geometry, elevation, type, exclusions and coordinate reference.
- Each module/inverter/equipment item references a catalogue record and datasheet revision.
- Calculations are pure functions where practical. Results store inputs, units, assumptions, engine version and timestamp.
- Input edits mark dependent outputs stale and trigger recalculation; no stale result may appear as current.
- Duplicate fields bound to the same key must stay synchronized.
- Exported documents and drawings must use the same project revision and calculation snapshot.

## Accuracy and safety gates

- A map pin is not a roof survey. Satellite imagery must not be treated as structural evidence.
- Weather values must be attributed to a named source and retrieval/report date.
- Shadow estimates must distinguish a simplified sun/shadow screen from validated 3D ray-tracing and electrical mismatch analysis.
- Preliminary structural or electrical estimates must not be presented as approved fabrication/construction designs.
- A result can be PASS only when all mandatory inputs, standards checks and engineering review steps are satisfied. Otherwise show REVIEW / INCOMPLETE.
- Do not insert arbitrary media or binary files to inflate APK size. Size follows actual features, offline assets and native dependencies; optimize delivery and keep the APK verifiable.

## Verification gates

1. Java/Gradle compile and APK existence.
2. JavaScript syntax check for every embedded/external script.
3. HTML parse and required element-ID uniqueness checks.
4. Shared-data tests: edit one field and verify all duplicate fields and dependent results update.
5. Geometry tests: rectangular, irregular, multi-zone, obstacles, setbacks, walkways, portrait/landscape and no-fit cases.
6. Solar tests: known-date sun-position fixtures, sunrise/sunset and polar/invalid coordinate cases.
7. Electrical tests: cold Voc, MPPT bounds, current limits, invalid inputs and cable drop.
8. CAD tests: DXF LINE/LWPOLYLINE/CIRCLE, units, scale, invalid files and export round-trip.
9. Android tests: file picker, GPS permission, offline launch, local persistence, PDF printing and rotation/relaunch.
10. DPR/BOM tests: one revision snapshot, consistent quantities, rates, totals, units and source references.

## Current implementation status (2026-11-06)

- Implemented in the current starter: project workspace, roof input fields, preliminary rectangular PV layout, concept DXF export, limited DXF entity parsing, project JSON import/export, BOM/BOQ estimate, basic electrical/battery/cable screens, GPS coordinate inputs, map embed/pin, device GPS permission flow, solar-resource input fields, simplified single-obstacle sun/shadow estimate, DPR preview and Android print integration.
- Still required for the full product: true editable multi-layer CAD, DWG/SKP conversion, polygon/irregular roof editing, robust multi-roof 3D modelling, ray-traced shade and module mismatch/loss simulation, detailed structural engineering/fabrication release, validated weather/resource integration, full SLD/protection coordination, robust database/revision management, and comprehensive automated/instrumented tests.
- The current application is a foundation and must not be represented as a finished professional CAD/shading package until those gates pass.
