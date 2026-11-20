const assert = require("node:assert/strict");
const fs = require("node:fs");
const source = fs.readFileSync("app/src/main/assets/index.html", "utf8");
const fn = source.match(/function buildMultiRoofLayout\(zones,mod,options\)\{[^\n]+/)?.[0];
assert.ok(fn, "multi-roof layout function must exist");
const rectCorners = (x,y,w,h) => [{x,y},{x:x+w,y},{x:x+w,y:y+h},{x,y:y+h}];
const inside = (p,poly) => {let hit=false;for(let i=0,j=poly.length-1;i<poly.length;j=i++){const a=poly[i],b=poly[j];if(((a.y>p.y)!==(b.y>p.y))&&(p.x<(b.x-a.x)*(p.y-a.y)/(b.y-a.y)+a.x))hit=!hit;}return hit;};
const build = new Function("rectCorners","inside",fn+";return buildMultiRoofLayout;")(rectCorners,inside);
const zones=[
 {id:"ROOF-A",name:"Main RCC",elevation:5,vertices:[{x:0,y:0},{x:10,y:0},{x:10,y:10},{x:0,y:10}]},
 {id:"ROOF-B",name:"Shed",elevation:8,vertices:[{x:20,y:0},{x:30,y:0},{x:30,y:10},{x:20,y:10}]}
];
const mod={length:2000,width:1000};
const base={gap:0,rowgap:0,walk:0,setback:0.5,target:30,orientation:"portrait"};
const result=build(zones,mod,base);
assert.equal(result.length,30);
const a=result.filter(x=>x.zoneId==="ROOF-A"),b=result.filter(x=>x.zoneId==="ROOF-B");
assert.ok(a.length>0&&b.length>0,"both roof zones should receive modules");
assert.ok(Math.abs(a.length-b.length)<=1,"allocation should be balanced between roofs");
assert.ok(a.every(x=>x.zoneElevation===5)&&b.every(x=>x.zoneElevation===8),"module schedule should preserve roof elevations");
assert.ok(result.every(x=>(x.x>=0.5&&x.x+x.w<=9.5)||(x.x>=20.5&&x.x+x.w<=29.5)));
const limited=build(zones,mod,{...base,target:1000});
assert.ok(limited.length<1000,"insufficient roof capacity must not fabricate placements");
const circleBlocked=build([zones[0]],mod,{...base,target:100,obstacleCircles:[{x:2.5,y:2.5,r:0.7}],obstacleClearance:0.3});
assert.ok(circleBlocked.length<build([zones[0]],mod,{...base,target:100}).length,"circular obstacle and clearance must reduce placements");
for(const m of circleBlocked){const qx=Math.max(m.x,Math.min(2.5,m.x+m.w)),qy=Math.max(m.y,Math.min(2.5,m.y+m.h));assert.ok(Math.hypot(2.5-qx,2.5-qy)>=1-1e-8,"placed module must clear circular obstacle plus clearance");}
const rectBlocked=build([zones[0]],mod,{...base,target:100,obstacleRectangles:[{x:4,y:4,w:2,h:2}],obstacleClearance:0.25});
assert.ok(rectBlocked.length<build([zones[0]],mod,{...base,target:100}).length,"rectangular obstacle must reduce placements");
for(const m of rectBlocked)assert.ok(!(m.x<6.25-1e-8&&m.x+m.w>3.75+1e-8&&m.y<6.25-1e-8&&m.y+m.h>3.75+1e-8),"placed module must clear inflated rectangular obstacle");
console.log("Multi-roof PV layout passed: balanced allocation, roof setback, capacity limits, circular/rectangular obstacles and clearance.");
