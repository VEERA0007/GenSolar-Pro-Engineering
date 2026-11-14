const assert = require("node:assert/strict");
const fs = require("node:fs");
const source = fs.readFileSync("app/src/main/assets/index.html","utf8");
function extract(a,b){const i=source.indexOf(a),j=source.indexOf(b,i);assert.ok(i>=0&&j>i,"Missing function "+a);return source.slice(i,j);}
const boundsFn=extract("function bounds(){","function roofEntity()");
const state={roofZones:[
 {id:"A",name:"RCC block",elevation:5,vertices:[{x:0,y:0},{x:10,y:0},{x:10,y:10},{x:0,y:10}]},
 {id:"B",name:"Metal shed",elevation:8,vertices:[{x:30,y:0},{x:40,y:0},{x:40,y:10},{x:30,y:10}]}
]};
const model={entities:[],layout:[{x:31,y:2,w:2,h:1,zoneElevation:8}]};
const bounds=new Function("state","model","n",boundsFn+";return bounds;")(state,model,()=>20)();
assert.equal(bounds.x,0);
assert.equal(bounds.y,0);
assert.equal(bounds.w,40);
assert.equal(bounds.h,10);
const renderFn=extract("function render3d(){","$('cadRefresh3d').onclick");
const box={innerHTML:""};
const get=id=>id==="cad3d"?box:null;
const render=new Function("state","model","$","roofPoly","n","esc",renderFn+";return render3d;")(
 state,{...model,building:{height:6},hiddenLayers:[]},get,
 ()=>[{x:0,y:0},{x:10,y:0},{x:10,y:10},{x:0,y:10}],()=>6,String
);
render();
assert.ok(box.innerHTML.includes("RCC block"));
assert.ok(box.innerHTML.includes("Metal shed"));
assert.ok(box.innerHTML.includes("5.00 m"));
assert.ok(box.innerHTML.includes("8.00 m"));
assert.ok(box.innerHTML.includes("1 modules"));
console.log("Multi-roof CAD bounds and conceptual 3D rendering tests passed.");
