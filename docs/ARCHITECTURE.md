# Architecture and engineering guardrails

## Initial architecture

- Android UI: native Java Activity.
- Calculation core: pure Java `RoofAreaCalculator` with no Android dependencies.
- Local persistence: Android SharedPreferences for this first prototype only.
- Network: none in the initial milestone.
- Build: GitHub Actions, JDK 17, Gradle 8.9, AGP 8.7.3.

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
