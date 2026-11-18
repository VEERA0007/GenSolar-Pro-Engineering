const assert = require("node:assert/strict");
const solver = require("../app/src/main/assets/structural_solver.js");
const nodes=[
{id:1,x:0,y:0,z:0},{id:2,x:0,y:4,z:0},{id:3,x:5,y:5,z:0},
{id:4,x:10,y:4,z:0},{id:5,x:10,y:0,z:0}
];
const members=[
{id:1,a:1,b:2,type:"column",frame:0},{id:2,a:2,b:3,type:"rafter",frame:0},
{id:3,a:3,b:4,type:"rafter",frame:0},{id:4,a:4,b:5,type:"column",frame:0}
];
const model={
nodes,members,frames:[{baseL:1,eaveL:2,ridge:3,eaveR:4,baseR:5}],
support:"PINNED",
loads:[{id:1,name:"DEAD",selfWeight:true,memberLoads:[{member:2,axis:"GY",value:-1},{member:3,axis:"GY",value:-1}]},{id:2,name:"LIVE",memberLoads:[{member:2,axis:"GY",value:-1},{member:3,axis:"GY",value:-1}]}],
combinations:[{id:5,name:"ULS",factors:[[1,1.5],[2,1.5]]}]
};
const result=solver.solve(model,{E:205000000,columnArea:.01,columnI:.00008,rafterArea:.01,rafterI:.00008},5);
assert.equal(result.solvedMembers,4);
assert.ok(Number.isFinite(result.maxTranslationM)&&result.maxTranslationM>0);
assert.ok(Number.isFinite(result.maxMomentKNm)&&result.maxMomentKNm>0);
assert.equal(result.reactions.length,4);
assert.ok(result.reactions.every(r=>Number.isFinite(r.value)));
assert.throws(()=>solver.solve(model,{E:0,columnArea:.01,columnI:.00008,rafterArea:.01,rafterI:.00008},5),/positive verified/);
console.log("Inbuilt portal-frame solver passed: 2D stiffness assembly, supports, gravity combination, reactions and member end forces.");
