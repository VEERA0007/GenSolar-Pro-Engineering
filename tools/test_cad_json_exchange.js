const assert=require("node:assert/strict");
const fs=require("node:fs");
const source=fs.readFileSync("app/src/main/assets/index.html","utf8");
function extract(a,b){const i=source.indexOf(a),j=source.indexOf(b,i);assert.ok(i>=0&&j>i,"Missing "+a);return source.slice(i,j);}
const helpers=extract("function validateImportedRoofZones(","function validateImportedUnderlay(");
const validate=new Function(helpers+";return {validateImportedRoofZones,validateImportedCadLayout};")();
const zones=[{id:"ZONE-1",name:"Main RCC",type:"RCC",elevation:5.5,pitch:12,vertices:[{x:0,y:0},{x:20,y:0},{x:20,y:10},{x:0,y:10}]}];
const clean=validate.validateImportedRoofZones(zones);
assert.equal(clean.length,1);
assert.equal(clean[0].elevation,5.5);
assert.equal(clean[0].pitch,12);
assert.equal(clean[0].vertices.length,4);
assert.throws(()=>validate.validateImportedRoofZones([{...zones[0],id:""}]),/IDs/);
assert.throws(()=>validate.validateImportedRoofZones([{...zones[0],vertices:[{x:0,y:0},{x:1,y:1},{x:2,y:2}]}]),/zero plan area/);
assert.throws(()=>validate.validateImportedRoofZones([{...zones[0],pitch:90}]),/pitch/);
assert.throws(()=>validate.validateImportedRoofZones([{...zones[0],vertices:[{x:0,y:0},{x:1,y:0},{x:Infinity,y:1}]}]),/invalid coordinates/);
const layout=validate.validateImportedCadLayout([{x:1,y:2,w:2,h:1,zoneElevation:5,zoneId:"ZONE-1"}]);
assert.equal(layout[0].zoneElevation,5);
assert.equal(layout[0].zoneId,"ZONE-1");
assert.throws(()=>validate.validateImportedCadLayout([{x:0,y:0,w:0,h:1}]),/invalid geometry/);

assert.ok(source.includes("schema:'GenSolarCAD/2'"),"CAD export must use the versioned exchange schema");
assert.ok(source.includes("roofZones:projectState.roofZones||[]"),"CAD export must carry roof-zone survey geometry");
assert.ok(source.includes("layout:model.layout"),"CAD export must carry PV module placement");
assert.ok(source.includes("localStorage.setItem('gensolar_epc_workspace_v1',JSON.stringify(nextState))"),"CAD import must persist the imported shared project state");
assert.ok(source.includes("location.reload()"),"Importing roof-zone state must refresh the EPC workspace");
console.log("CAD JSON roof-zone and PV-layout import validation tests passed.");
