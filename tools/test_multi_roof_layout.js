const assert = require("node:assert/strict");
const fs = require("node:fs");
const source = fs.readFileSync("app/src/main/assets/index.html", "utf8");
const fn = source.match(/function buildMultiRoofLayout\(zones,mod,options\)\{[^\n]+/)?.[0];
assert.ok(fn, "multi-roof layout function must exist");
const rectCorners = (x,y,w,h) => [{x,y},{x:x+w,y},{x:x+w,y:y+h},{x,y:y+h}];
const inside = (p,poly) => {let hit=false;for(let i=0,j=poly.length-1;i<poly.length;j=i++){const a=poly[i],b=poly[j];if(((a.y>p.y)!==(b.y>p.y))&&(p.x<(b.x-a.x)*(p.y-a.y)/(b.y-a.y)+a.x))hit=!hit;}return hit;};
const obstacleHit = () => false;
const build = new Function("rectCorners","inside","obstacleHit",fn+";return buildMultiRoofLayout;")(rectCorners,inside,obstacleHit);
const zones=[
 {id:"ROOF-A",name:"Main RCC",vertices:[{x:0,y:0},{x:10,y:0},{x:10,y:10},{x:0,y:10}]},
 {id:"ROOF-B",name:"Shed",vertices:[{x:20,y:0},{x:30,y:0},{x:30,y:10},{x:20,y:10}]}
];
const mod={length:2000,width:1000};
const result=build(zones,mod,{gap:0,rowgap:0,walk:0,setback:0.5,target:30,orientation:"portrait"});
assert.equal(result.length,30);
assert.ok(result.some(x=>x.zoneId==="ROOF-A"));
assert.ok(result.some(x=>x.zoneId==="ROOF-B"));
assert.ok(result.every(x=>x.zoneName==="Main RCC"||x.zoneName==="Shed"));
assert.ok(result.every(x=>(x.x>=0.5&&x.x+x.w<=9.5)||(x.x>=20.5&&x.x+x.w<=29.5)));
const limited=build(zones,mod,{gap:0,rowgap:0,walk:0,setback:0.5,target:1000,orientation:"portrait"});
assert.ok(limited.length<1000);
console.log("Multi-roof PV layout allocation tests passed.");
