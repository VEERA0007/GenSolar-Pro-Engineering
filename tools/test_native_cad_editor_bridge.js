#!/usr/bin/env node
"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const html = fs.readFileSync("app/src/main/assets/index.html", "utf8");
const transformStart = html.indexOf("function entityPivot(e)");
const transformEnd = html.indexOf("function finishPoly()", transformStart);
const helperStart = html.indexOf("function translateCadEntity(");
const bridgeStart = html.indexOf("window.GenSolarEpcBridge = Object.freeze({", helperStart);
const bridgeEnd = html.indexOf("\n});\nwindow.GenSolarStudio", bridgeStart);
assert.ok(transformStart >= 0 && transformEnd > transformStart, "CAD transform helpers must exist");
assert.ok(helperStart >= 0 && bridgeStart > helperStart, "Native CAD transaction helpers must exist");
assert.ok(bridgeEnd > bridgeStart, "Controlled EPC bridge must exist");

const transforms = html.slice(transformStart, transformEnd);
const helpers = html.slice(helperStart, bridgeStart);
const bridge = html.slice(bridgeStart, bridgeEnd + 4);

function createHarness(saveResult = true) {
  let serial = 0;
  const model = {
    entities: [
      {id:"roof-1",type:"rect",layer:"ROOF",x:0,y:0,w:10,h:6},
      {id:"line-1",type:"line",layer:"GENERAL",a:{x:1,y:1},b:{x:4,y:1}},
      {id:"poly-1",type:"poly",layer:"OBSTACLES",closed:true,points:[{x:2,y:2},{x:3,y:2},{x:3,y:3}]},
      {id:"circle-1",type:"circle",layer:"OBSTACLES",x:7,y:4,r:0.5}
    ],
    selected:"roof-1",
    hiddenLayers:[],
    layout:[],
    underlay:null,
    building:{height:6,thickness:0.2},
    outputs:{},
    undo:[]
  };
  const context = {
    model,
    state:{},
    window:{},
    uid:()=>"copy-"+(++serial),
    save:()=>saveResult,
    saveUndo:()=>model.undo.push(JSON.stringify({entities:model.entities,layout:model.layout,hiddenLayers:model.hiddenLayers})),
    draw:()=>{},
    render3d:()=>{},
    renderLayerVisibility:()=>{},
    hit:(p)=>model.entities.find(e=>e.type==="rect"&&p.x>=e.x&&p.x<=e.x+e.w&&p.y>=e.y&&p.y<=e.y+e.h) || null
  };
  vm.createContext(context);
  vm.runInContext(transforms + "\n" + helpers + "\n" + bridge, context);
  return {bridge:context.window.GenSolarEpcBridge,model};
}

{
  const h=createHarness();
  const initial=h.bridge.getCadSnapshot();
  assert.equal(initial.entities.length,4);
  assert.equal(initial.selected,"roof-1");
  assert.deepEqual(Array.from(initial.hiddenLayers),[]);
  initial.entities[0].x=999;
  assert.equal(h.model.entities[0].x,0,"Snapshots must not expose mutable project entities");

  assert.equal(h.bridge.selectCadEntityAt(2,2),"roof-1");
  assert.equal(h.model.selected,"roof-1");
  assert.equal(h.bridge.selectCadEntityAt(100,100),false);
  assert.equal(h.model.selected,null,"A miss must clear selection");

  assert.equal(h.bridge.moveCadEntity("roof-1",2,-1),true);
  assert.deepEqual({x:h.model.entities[0].x,y:h.model.entities[0].y},{x:2,y:-1});
  assert.equal(h.model.undo.length,1);
  assert.equal(h.bridge.moveCadEntity("roof-1",Infinity,0),false);
  assert.equal(h.bridge.moveCadEntity("missing",1,1),false);

  const beforeDuplicate=h.model.entities.length;
  const copyId=h.bridge.duplicateCadEntity("line-1");
  assert.ok(copyId&&copyId!=="line-1");
  assert.equal(h.model.entities.length,beforeDuplicate+1);
  assert.equal(h.model.selected,copyId);
  const copied=h.model.entities.find(e=>e.id===copyId);
  assert.equal(copied.a.x,2,"Duplicate must preserve the CAD engine's one-metre offset");
  assert.equal(copied.layer,"GENERAL");

  assert.equal(h.bridge.rotateCadEntityBy90("roof-1"),true);
  const roof=h.model.entities.find(e=>e.id==="roof-1");
  assert.equal(roof.w,6);
  assert.equal(roof.h,10);
  assert.equal(h.bridge.rotateCadEntityBy90("circle-1"),false,"Rotationally symmetric circles are not modified");

  assert.equal(h.bridge.setCadLayerVisible("OBSTACLES",false),true);
  assert.ok(h.model.hiddenLayers.includes("OBSTACLES"));
  assert.equal(h.model.selected,null,"Hiding the selected layer must clear selection");
  assert.equal(h.bridge.setCadLayerVisible("OBSTACLES",true),true);
  assert.ok(!h.model.hiddenLayers.includes("OBSTACLES"));

  assert.equal(h.bridge.deleteCadEntity("poly-1"),true);
  assert.ok(!h.model.entities.some(e=>e.id==="poly-1"));
  assert.equal(h.bridge.deleteCadEntity("unknown"),false);
}

{
  const h=createHarness(false);
  const before=JSON.stringify(h.model.entities);
  assert.equal(h.bridge.moveCadEntity("roof-1",5,5),false,"Failed persistence must reject the edit");
  assert.equal(JSON.stringify(h.model.entities),before,"Failed persistence must restore geometry");
  assert.equal(h.model.undo.length,0,"Failed persistence must discard the undo entry");
  assert.equal(h.bridge.setCadLayerVisible("ROOF",false),false);
  assert.deepEqual(Array.from(h.model.hiddenLayers),[],"Failed layer persistence must restore visibility");
  assert.equal(h.bridge.duplicateCadEntity("line-1"),false);
  assert.equal(h.model.entities.length,4,"Failed duplicate persistence must restore entity count");
  assert.equal(h.bridge.deleteCadEntity("poly-1"),false);
  assert.ok(h.model.entities.some(e=>e.id==="poly-1"),"Failed delete persistence must restore the entity");
}

console.log("Native CAD bridge tests passed: snapshot isolation, hit selection, measured move, duplicate, rotate, delete, layer visibility and persistence rollback.");
