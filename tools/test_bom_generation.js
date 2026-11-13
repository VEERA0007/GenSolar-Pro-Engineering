const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const html = fs.readFileSync("app/src/main/assets/index.html", "utf8");
const start = html.indexOf("function buildBom(){");
const end = html.indexOf("\nfunction renderBom()", start);
assert.ok(start >= 0 && end > start, "buildBom implementation must exist and remain independently testable");

const source = html.slice(start, end);
const alerts = [];
const context = {
  state: {
    layout: { count: 101, rows: 11, kwp: 55.55 },
    project: { wp: 550, acKw: 50, cableLength: 40, mpptCount: 4 },
    electrical: { strings: 5 },
    protection: { fuse: 20, dcSwitch: 32, acBreaker: 100, earthRequired: 16 },
    bom: [
      ["PV modules", "Wp 550", "Nos", 101, 1234.5],
      ["Grid inverter", "AC 50 kW", "Nos", 1, 45678]
    ]
  },
  val: key => Number(context.state.project[key] ?? 0),
  renderBom: () => { context.renderedRows = context.state.bom.length; },
  save: () => { context.saved = true; return true; },
  alert: message => alerts.push(String(message))
};
vm.createContext(context);
vm.runInContext(source, context, { filename: "gensolar-build-bom.js" });
assert.equal(typeof context.buildBom, "function", "buildBom must be callable in the isolated regression harness");

assert.equal(context.buildBom(), true, "valid engineering inputs should save the BOM");
assert.equal(context.state.bom.length, 12, "BOM should contain the 12 configured material groups");
assert.deepEqual(Array.from(context.state.bom[0]), ["PV modules", "Wp 550", "Nos", 101, 1234.5], "existing module rate must survive a rebuild");
assert.deepEqual(Array.from(context.state.bom[1]), ["Grid inverter", "AC 50 kW", "Nos", 1, 45678], "existing inverter rate must survive a rebuild");
assert.equal(context.state.bom[2][3], 22, "rail quantity must be two lines per module row");
assert.equal(context.state.bom[3][3], 102, "support allowance must round up to an even pair count");
assert.equal(context.state.bom[4][3], 88, "DC cable allowance must include the two-conductor route and 10% allowance");
assert.equal(context.state.bom[5][3], 44, "AC cable allowance must include the 10% route allowance");
assert.equal(context.state.bom[6][3], 10, "string fuse quantity must be two per calculated string");
assert.equal(context.state.bom[7][3], 4, "DC isolator allowance must follow the positive integer MPPT count");
assert.equal(context.state.bom[10][3], 44, "earthing conductor allowance must use the route allowance");
assert.equal(context.state.bom[11][3], 44, "cable tray allowance must use the route allowance");
assert.equal(context.renderedRows, 12, "rendered BOM should match the saved material schedule");
assert.equal(context.saved, true, "generated BOM must be persisted");
assert.equal(alerts.length, 0, "valid inputs must not raise validation alerts");

const validBom = context.state.bom;
context.state.layout = null;
assert.equal(context.buildBom(), false, "BOM generation must stop when PV layout is missing");
assert.equal(context.state.bom, validBom, "missing PV layout must not overwrite an existing BOM");
assert.match(alerts.pop(), /Generate PV layout first/);

context.state.layout = { count: 101, rows: 11 };
context.state.project.cableLength = 0;
assert.equal(context.buildBom(), false, "zero cable route length must be rejected");
assert.equal(context.state.bom, validBom, "invalid cable length must not overwrite saved rates or quantities");
assert.match(alerts.pop(), /positive cable route length/);

context.state.project.cableLength = 40;
context.state.project.mpptCount = 2.5;
assert.equal(context.buildBom(), false, "fractional MPPT count must be rejected");
assert.equal(context.state.bom, validBom, "invalid MPPT count must not overwrite the saved BOM");
assert.match(alerts.pop(), /positive integer MPPT count/);

console.log("BOM generation tests passed: engineering inputs, 12 material groups, route allowances, MPPT validation, rate preservation and offline save.");
