const assert = require("node:assert/strict");
const fs = require("node:fs");
const source = fs.readFileSync("app/src/main/assets/index.html", "utf8");
function extract(startMarker,endMarker){
 const start=source.indexOf(startMarker),end=source.indexOf(endMarker,start);
 assert.ok(start>=0&&end>start,"Missing source function: "+startMarker);
 return source.slice(start,end);
}
const polygonArea=extract("function polygonArea(","function cross(");
const zoneGeometry=extract("function zoneGeometry(","function renderRoofZones(");
const calculate=new Function(polygonArea+zoneGeometry+";return zoneGeometry;")();
const rectangle=[{x:0,y:0},{x:30,y:0},{x:30,y:20},{x:0,y:20}];
const flat=calculate({vertices:rectangle,pitch:0});
assert.equal(flat.area,600);
assert.equal(flat.surfaceArea,600);
assert.equal(flat.pitch,0);
const pitched=calculate({vertices:rectangle,pitch:30});
assert.equal(pitched.area,600);
assert.ok(Math.abs(pitched.surfaceArea-692.820323)<0.001);
assert.ok(pitched.surfaceArea>pitched.area);
const legacy=calculate({vertices:rectangle});
assert.equal(legacy.pitch,0);
assert.equal(legacy.surfaceArea,600);
const highPitch=calculate({vertices:rectangle,pitch:80});
assert.ok(highPitch.surfaceArea>pitched.surfaceArea);
console.log("Roof zone plan and pitch-corrected surface area tests passed.");

const parser=extract("function polygonArea(","function zoneGeometry(");
const parseVertices=new Function(parser+";return parseZoneVertices;")();
assert.equal(parseVertices("0,0; 10,0; 10,10; 0,10").length,4);
assert.throws(()=>parseVertices("0,0; 10001,0; 10001,10; 0,10"),/±10000 m/);
const tooMany=Array.from({length:201},(_,i)=>[Math.cos(i*2*Math.PI/201)*100,Math.sin(i*2*Math.PI/201)*100].join(",")).join(";");
assert.throws(()=>parseVertices(tooMany),/cannot exceed 200 vertices/);
