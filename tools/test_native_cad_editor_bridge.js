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

const cadScriptStart = html.indexOf("const model={version:1");
const saveStart = html.indexOf("function save()", cadScriptStart);
const saveEnd = html.indexOf("\nfunction load()", saveStart);
assert.ok(saveStart > cadScriptStart && saveEnd > saveStart, "CAD local save function must exist");
const saveSource = html.slice(saveStart, saveEnd);
{
  const values = new Map([
    ["gensolar_integrated_studio_v1","previous-model"],
    ["gensolar_module_catalogue_v1","previous-catalogue"]
  ]);
  let failCatalogue = false;
  const status = {textContent:""};
  const context = {
    STORE:"gensolar_integrated_studio_v1",
    CAT:"gensolar_module_catalogue_v1",
    model:{entities:[{id:"persisted"}],catalogue:[]},
    $:()=>status,
    localStorage:{
      getItem:key=>values.has(key)?values.get(key):null,
      setItem:(key,value)=>{
        if(failCatalogue&&key==="gensolar_module_catalogue_v1")throw new Error("quota");
        values.set(key,String(value));
      },
      removeItem:key=>values.delete(key)
    }
  };
  vm.createContext(context);
  vm.runInContext(saveSource,context);
  assert.equal(vm.runInContext("save()",context),true,"Successful localStorage writes must be confirmed");
  assert.ok(JSON.parse(values.get("gensolar_integrated_studio_v1")).entities[0].id==="persisted");
  values.set("gensolar_integrated_studio_v1","previous-model");
  values.set("gensolar_module_catalogue_v1","previous-catalogue");
  failCatalogue=true;
  assert.equal(vm.runInContext("save()",context),false,"A partial storage failure must be reported");
  assert.equal(values.get("gensolar_integrated_studio_v1"),"previous-model","Partial writes must restore the previous CAD model");
  assert.equal(values.get("gensolar_module_catalogue_v1"),"previous-catalogue","Partial writes must preserve the previous catalogue");
}
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
    layout:[{x:1,y:1,w:1.2,h:2}],
    underlay:null,
    building:{height:6,thickness:0.2},
    outputs:{},
    undo:[]
  };
  const state={roofZones:[{id:"zone-1",name:"Main roof",vertices:[{x:0,y:0},{x:10,y:0},{x:10,y:6}]}]};
  const context = {
    model,
    state,
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
  return {bridge:context.window.GenSolarEpcBridge,model,state};
}

{
  const h=createHarness();
  const initial=h.bridge.getCadSnapshot();
  assert.equal(initial.entities.length,4);
  assert.equal(initial.layout.length,1);
  assert.equal(initial.roofZones.length,1);
  assert.equal(initial.selected,"roof-1");
  assert.deepEqual(Array.from(initial.hiddenLayers),[]);
  initial.entities[0].x=999;
  initial.layout[0].x=999;
  initial.roofZones[0].vertices[0].x=999;
  assert.equal(h.model.entities[0].x,0,"Snapshots must not expose mutable project entities");
  assert.equal(h.model.layout[0].x,1,"Snapshots must not expose mutable PV layout");
  assert.equal(h.state.roofZones[0].vertices[0].x,0,"Snapshots must not expose mutable roof-zone geometry");

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

  h.model.selected="poly-1";
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
