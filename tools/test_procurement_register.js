const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const html = fs.readFileSync("app/src/main/assets/index.html", "utf8");
function extract(name) {
  const start = html.indexOf("function " + name + "(");
  assert.notEqual(start, -1, "Missing function " + name);
  const end = html.indexOf("\nfunction ", start + 1);
  return html.slice(start, end < 0 ? html.length : end);
}
const elements = {
  vendorTable: { innerHTML: "" },
  vendorStatusMessage: { textContent: "" },
  poStatusMessage: { textContent: "" },
  poList: { innerHTML: "" },
  poStatusRef: { value: "", innerHTML: "" },
  poStatusHistory: { innerHTML: "" },
  receiptPoRef: { value: "", innerHTML: "" },
  goodsReceiptList: { innerHTML: "" },
  receiptReconciliation: { innerHTML: "" }
};
const context = {
  state: {
    project: {},
    vendors: [],
    pos: [],
    poHistory: [],
    goodsReceipts: []
  },
  $: id => elements[id],
  save: () => true,
  escapeHtml: value => String(value == null ? "" : value).replace(/[&<>"']/g, ch => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[ch])
};
vm.createContext(context);
for (const name of ["renderVendorTable", "addVendor", "renderSimple", "renderPoLifecycle", "renderGoodsReceipts", "addPo"]) {
  vm.runInContext(extract(name), context);
}

// Vendor quote validation and safe output.
context.state.project = {
  vendorName: "<img src=x onerror=alert(1)>",
  vendorAmount: "125000",
  vendorDays: "7",
  vendorNotes: "<script>alert(1)</script>"
};
assert.equal(vm.runInContext("addVendor()", context), true);
assert.equal(context.state.vendors.length, 1);
assert.ok(elements.vendorTable.innerHTML.includes("&lt;img"));
assert.ok(elements.vendorTable.innerHTML.includes("&lt;script&gt;"));
assert.ok(!elements.vendorTable.innerHTML.includes("<script>"));

context.state.project.vendorAmount = "0";
assert.equal(vm.runInContext("addVendor()", context), false);
assert.match(elements.vendorStatusMessage.textContent, /greater than zero/);
context.state.project.vendorAmount = "125000";
context.state.project.vendorDays = "1.5";
assert.equal(vm.runInContext("addVendor()", context), false);
assert.match(elements.vendorStatusMessage.textContent, /whole number/);

// Purchase order validation, duplicate guard and rendering safety.
context.state.project = {
  poRef: "PO-001",
  poVendor: "Supplier A",
  poValue: "250000",
  poQty: "100",
  poUnit: "Nos",
  poDate: "2026-12-15",
  poStatus: "Ordered"
};
assert.equal(vm.runInContext("addPo()", context), true);
assert.equal(context.state.pos.length, 1);
assert.match(elements.poStatusMessage.textContent, /saved/i);
assert.equal(vm.runInContext("addPo()", context), false);
assert.match(elements.poStatusMessage.textContent, /already exists/);

context.state.project.poRef = "PO-002";
context.state.project.poValue = "0";
assert.equal(vm.runInContext("addPo()", context), false);
assert.match(elements.poStatusMessage.textContent, /greater than zero/);
context.state.project.poValue = "1000";
context.state.project.poDate = "2026-02-30";
assert.equal(vm.runInContext("addPo()", context), false);
assert.match(elements.poStatusMessage.textContent, /valid expected delivery date/);
context.state.project.poDate = "2026-12-15";
context.state.project.poVendor = "<svg onload=alert(1)>";
assert.equal(vm.runInContext("addPo()", context), true);
assert.ok(elements.poList.innerHTML.includes("&lt;svg"));
assert.ok(!elements.poList.innerHTML.includes("<svg"));

console.log("Procurement register tests passed: vendor validation, PO validation, duplicate references, date validation and HTML escaping.");
