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
  activityStatusMessage: { textContent: "" },
  activityList: { innerHTML: "" }
};
let saves = 0;
const context = {
  state: { project: {}, activities: [] },
  $: id => elements[id],
  save: () => { saves += 1; return true; },
  escapeHtml: value => String(value == null ? "" : value).replace(/[&<>"']/g, ch => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[ch])
};
vm.createContext(context);
for (const name of ["renderSimple", "addActivity"]) vm.runInContext(extract(name), context);

context.state.project = {
  activityName: "Structure installation",
  activityOwner: "Site Engineer",
  activityStart: "2026-11-20",
  activityDays: "5",
  activityStatus: "In progress"
};
assert.equal(vm.runInContext("addActivity()", context), true);
assert.equal(context.state.activities.length, 1);
assert.deepEqual(Array.from(context.state.activities[0]), [
  "Structure installation", "Site Engineer", "2026-11-20", 5, "In progress"
]);
assert.match(elements.activityStatusMessage.textContent, /saved/i);
assert.equal(saves, 1);

context.state.project.activityOwner = "";
assert.equal(vm.runInContext("addActivity()", context), false);
assert.match(elements.activityStatusMessage.textContent, /owner/i);
context.state.project.activityOwner = "Site Engineer";
context.state.project.activityStart = "2026-02-30";
assert.equal(vm.runInContext("addActivity()", context), false);
assert.match(elements.activityStatusMessage.textContent, /valid activity start date/i);
context.state.project.activityStart = "2026-11-20";
context.state.project.activityDays = "0";
assert.equal(vm.runInContext("addActivity()", context), false);
assert.match(elements.activityStatusMessage.textContent, /at least one day/i);
context.state.project.activityDays = "1.5";
assert.equal(vm.runInContext("addActivity()", context), false);
context.state.project.activityDays = "2";
context.state.project.activityStatus = "Unknown";
assert.equal(vm.runInContext("addActivity()", context), false);
assert.match(elements.activityStatusMessage.textContent, /valid activity status/i);
context.state.project.activityStatus = "Completed";
context.state.project.activityName = "<script>alert(1)</script>";
assert.equal(vm.runInContext("addActivity()", context), true);
assert.ok(elements.activityList.innerHTML.includes("&lt;script&gt;"));
assert.ok(!elements.activityList.innerHTML.includes("<script>"));
assert.equal(saves, 2, "Invalid entries must not be persisted");

// Saved construction records must be restored into the activity register at startup.
const initStart = html.indexOf("function init(){");
assert.notEqual(initStart, -1, "Missing application initializer");
const initEnd = html.indexOf("\\nfunction ", initStart + 1);
const initSource = html.slice(initStart, initEnd < 0 ? html.length : initEnd);
assert.ok(initSource.includes("renderSimple('activityList',state.activities||[])"),
  "Startup must render saved activity records after loading the project");
assert.ok(initSource.includes("renderSimple('poList',state.pos||[])"),
  "Startup must continue restoring saved purchase records");



console.log("Construction activity tests passed: required fields, calendar date, whole-day duration, allowed status, safe rendering and save-on-valid-only.");
