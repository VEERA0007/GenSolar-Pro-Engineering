const assert = require("node:assert/strict");
const fs = require("node:fs");
const source = fs.readFileSync("app/src/main/assets/index.html", "utf8");
const fn = source.match(/function buildMultiRoofLayout\(zones,mod,options\)\{[^
]+/)?.[0];
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


const circleParserSource=source.match(/function parseObstacleCircles\(raw\)\{[^
]+\}/)?.[0];
const rectangleParserSource=source.match(/function parseObstacleRectangles\(raw\)\{[^
]+\}/)?.[0];
assert.ok(circleParserSource&&rectangleParserSource,"saved obstacle parsers must exist");
const parseCircles=new Function(circleParserSource+";return parseObstacleCircles;")();
const parseRectangles=new Function(rectangleParserSource+";return parseObstacleRectangles;")();
assert.deepEqual(parseCircles(""),[]);
assert.deepEqual(parseCircles("1,2,0.5; 3 4 1"),[{x:1,y:2,r:0.5},{x:3,y:4,r:1}]);
assert.throws(()=>parseCircles("1,2,0"),/positive radius/);
assert.throws(()=>parseCircles("1,2,abc"),/positive radius/);
assert.deepEqual(parseRectangles(""),[]);
assert.deepEqual(parseRectangles("1,2,3,4; 5 6 7 8"),[{x:1,y:2,w:3,h:4},{x:5,y:6,w:7,h:8}]);
assert.throws(()=>parseRectangles("1,2,0,4"),/positive dimensions/);
assert.throws(()=>parseRectangles("1,2,3"),/positive dimensions/);
const zeroClearance=build([zones[0]],mod,{...base,target:100,obstacleRectangles:[{x:4,y:4,w:2,h:2}],obstacleClearance:0});
const negativeClearance=build([zones[0]],mod,{...base,target:100,obstacleRectangles:[{x:4,y:4,w:2,h:2}],obstacleClearance:-2});
assert.deepEqual(negativeClearance,zeroClearance,"negative clearance must normalize to zero");

const accessBuilderSource=source.match(/function buildAccessCorridors\(zones,edge,width,additional\)\{[^
]+\}/)?.[0];
const corridorParserSource=source.match(/function parseCorridorRectangles\(raw\)\{[^
]+\}/)?.[0];
assert.ok(accessBuilderSource&&corridorParserSource,"access corridor builder and parser must exist");
const buildAccessCorridors=new Function(accessBuilderSource+";return buildAccessCorridors;")();
const parseCorridors=new Function(corridorParserSource+";return parseCorridorRectangles;")();
assert.deepEqual(parseCorridors(""),[]);
assert.deepEqual(parseCorridors("2,4,1,8; 8,12,6,1"),[{x:2,y:4,w:1,h:8},{x:8,y:12,w:6,h:1}]);
assert.throws(()=>parseCorridors("1,2,0,4"),/positive dimensions/);
const leftReserved=buildAccessCorridors([zones[0]],"left",1.5,[]);
assert.deepEqual(leftReserved,[{x:0,y:0,w:1.5,h:10}]);
const leftFit=build([zones[0]],mod,{...base,target:100,reservedRectangles:leftReserved});
assert.ok(leftFit.length<build([zones[0]],mod,{...base,target:100}).length,"left access corridor must reserve roof area");
assert.ok(leftFit.every(m=>m.x>=1.5-1e-8),"no module may intrude into the left access corridor");
const rightReserved=buildAccessCorridors([zones[0]],"right",1.5,[]);
assert.deepEqual(rightReserved,[{x:8.5,y:0,w:1.5,h:10}]);
const topReserved=buildAccessCorridors([zones[0]],"top",1.5,[]);
assert.deepEqual(topReserved,[{x:0,y:0,w:10,h:1.5}]);
const bottomReserved=buildAccessCorridors([zones[0]],"bottom",1.5,[]);
assert.deepEqual(bottomReserved,[{x:0,y:8.5,w:10,h:1.5}]);
const extraReserved=buildAccessCorridors([zones[0]],"none",0,[{x:4,y:4,w:1,h:4}]);
const extraFit=build([zones[0]],mod,{...base,target:100,reservedRectangles:extraReserved});
for(const m of extraFit)assert.ok(!(m.x<5-1e-8&&m.x+m.w>4+1e-8&&m.y<8-1e-8&&m.y+m.h>4+1e-8),"module must not overlap a manually reserved corridor");
assert.throws(()=>buildAccessCorridors([zones[0]],"diagonal",1,[]),/Invalid access corridor edge/);
