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
const geometryStart = html.indexOf("function polygonArea(");
const geometryEnd = html.indexOf("function layoutCalc()", geometryStart);
assert.ok(geometryStart >= 0 && geometryEnd > geometryStart, "PV geometry validation helpers must exist");
const pvGeometry = html.slice(geometryStart, geometryEnd);
const obstacleStart = html.indexOf("function obstacleHit(");
const obstacleEnd = html.indexOf("\nfunction draw()", obstacleStart);
assert.ok(obstacleStart >= 0 && obstacleEnd > obstacleStart, "CAD obstacle collision helper must exist");
const obstacleHelper = html.slice(obstacleStart, obstacleEnd);

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
  const state={roofZones:[{id:"zone-1",name:"Main roof",vertices:[{x:0,y:0},{x:10,y:0},{x:10,y:6}]}],project:{}};
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
  vm.runInContext(pvGeometry + "\n" + obstacleHelper + "\n" + transforms + "\n" + helpers + "\n" + bridge, context);
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

  const beforePrimitives=h.model.entities.length;
  const rectId=h.bridge.createCadEntity({type:"rect",layer:"GENERAL",x:-2,y:3,w:4,h:2});
  assert.ok(typeof rectId==="string"&&rectId.length>0,"Native rectangle creation must return the saved entity ID");
  const rect=h.model.entities.find(e=>e.id===rectId);
  assert.deepEqual({type:rect.type,layer:rect.layer,x:rect.x,y:rect.y,w:rect.w,h:rect.h},
    {type:"rect",layer:"GENERAL",x:-2,y:3,w:4,h:2});
  const lineId=h.bridge.createCadEntity({type:"line",layer:"DIMENSIONS",a:{x:0,y:0},b:{x:3,y:4}});
  assert.ok(lineId&&h.model.entities.find(e=>e.id===lineId).type==="line");
  const circleId=h.bridge.createCadEntity({type:"circle",layer:"OBSTACLES",x:5,y:6,r:0.75});
  assert.ok(circleId&&h.model.entities.find(e=>e.id===circleId).r===0.75);
  const polyId=h.bridge.createCadEntity({type:"poly",layer:"ROOF_ZONES",closed:true,points:[{x:0,y:0},{x:4,y:0},{x:4,y:3}]});
  const poly=h.model.entities.find(e=>e.id===polyId);
  assert.ok(polyId&&poly.type==="poly"&&poly.closed&&poly.points.length===3,"Native CAD must create a closed editable polygon");
  assert.equal(h.bridge.createCadEntity({type:"poly",layer:"ROOF_ZONES",closed:true,points:[{x:0,y:0},{x:1,y:1}]}),false,"Closed polylines require at least three vertices");
  assert.equal(h.bridge.createCadEntity({type:"poly",layer:"ROOF_ZONES",points:[{x:0,y:0},{x:0,y:0}]}),false,"Polyline segments must have non-zero length");
  assert.equal(h.bridge.createCadEntity({type:"poly",layer:"ROOF_ZONES",points:Array.from({length:201},(_,i)=>({x:i,y:i+1}))}),false,"Polyline vertex count must be bounded");
  const dimensionId=h.bridge.createCadEntity({type:"dim",layer:"DIMENSIONS",a:{x:1,y:1},b:{x:4,y:5}});
  assert.equal(h.model.entities.find(e=>e.id===dimensionId).label,"5.00 m","Dimensions must derive their displayed length from endpoints");
  assert.equal(h.model.entities.length,beforePrimitives+5);
  assert.equal(h.model.selected,dimensionId,"New geometry must become the selected CAD entity");
  assert.equal(h.bridge.createCadEntity({type:"rect",layer:"GENERAL",x:0,y:0,w:0,h:1}),false);
  assert.equal(h.bridge.createCadEntity({type:"circle",layer:"GENERAL",x:0,y:0,r:0}),false);
  assert.equal(h.bridge.createCadEntity({type:"line",layer:"GENERAL",a:{x:0,y:0},b:{x:0,y:0}}),false);
  assert.equal(h.bridge.createCadEntity({type:"unknown",layer:"GENERAL"}),false);
  assert.equal(h.bridge.createCadEntity({type:"rect",layer:"bad\nlayer",x:0,y:0,w:1,h:1}),false,
    "Control characters must be rejected from layer names");
  assert.equal(h.bridge.createCadEntity({type:"rect",layer:"GENERAL",x:10000,y:0,w:1,h:1}),false,
    "Geometry must remain within the supported coordinate range");
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
  const beforeCreate=JSON.stringify(h.model.entities);
  assert.equal(h.bridge.createCadEntity({type:"rect",layer:"GENERAL",x:1,y:1,w:2,h:2}),false,
    "Failed persistence must reject new native geometry");
  assert.equal(JSON.stringify(h.model.entities),beforeCreate,"Failed geometry persistence must restore the previous CAD model");
  assert.equal(h.model.undo.length,0,"Failed geometry persistence must discard its undo entry");
}


{
  const h=createHarness();
  h.state.roofZones=[{
    id:"manual-zone",name:"Manual roof",elevation:4,pitch:5,
    vertices:[{x:0,y:0},{x:10,y:0},{x:10,y:6},{x:0,y:6}]
  }];
  const before=h.model.layout.length;
  const placed=h.bridge.addManualPvModule({
    zoneId:"manual-zone",x:4,y:1,w:1.134,h:2.278,orientation:"portrait"
  });
  assert.equal(placed.ok,true,"A valid manual module inside the selected roof zone must be saved");
  assert.equal(h.model.layout.length,before+1);
  const module=h.model.layout.find(item=>item.id===placed.id);
  assert.equal(module.placement,"manual");
  assert.equal(module.zoneId,"manual-zone");
  assert.equal(module.zoneName,"Manual roof");
  assert.equal(module.zoneElevation,4);
  assert.equal(h.bridge.selectPvModuleAt(4.5,2),placed.id,"Tapping a manual module must return its stable ID");
  assert.equal(h.bridge.deleteManualPvModule(placed.id),true,"A selected manual module must be removable");
  assert.equal(h.model.layout.length,before);
  assert.equal(h.bridge.deleteManualPvModule("missing"),false);
  assert.equal(h.bridge.deleteManualPvModule(""),false);

  const overlap=h.bridge.addManualPvModule({zoneId:"manual-zone",x:1.1,y:1.1,w:1,h:1.5});
  assert.equal(overlap.ok,false,"Manual placement must reject overlap with an existing module");
  assert.match(overlap.error,/overlaps an existing PV module/);

  h.state.project.obstacleCircles="4.5,1.5,0.35";
  const circleBlock=h.bridge.addManualPvModule({zoneId:"manual-zone",x:4,y:1,w:1,h:2});
  assert.equal(circleBlock.ok,false,"Manual placement must respect circular obstacle clearance");
  assert.match(circleBlock.error,/obstacle or its clearance/);
  h.state.project.obstacleCircles="";
  h.state.project.obstacleRectangles="4,1,1,2";
  const rectangleBlock=h.bridge.addManualPvModule({zoneId:"manual-zone",x:4,y:1,w:1,h:2});
  assert.equal(rectangleBlock.ok,false,"Manual placement must respect rectangular obstacles");
  h.state.project.obstacleRectangles="";
  h.state.project.accessCorridorEdge="left";
  h.state.project.accessCorridorWidth=1;
  const edgeBlock=h.bridge.addManualPvModule({zoneId:"manual-zone",x:0.4,y:1,w:0.5,h:1});
  assert.equal(edgeBlock.ok,false,"Manual placement must preserve the selected edge access corridor");
  assert.match(edgeBlock.error,/access corridor/);
  h.state.project.accessCorridorEdge="none";
  h.state.project.accessCorridorWidth=0;
  h.state.project.corridorRectangles="4,1,1,2";
  const customBlock=h.bridge.addManualPvModule({zoneId:"manual-zone",x:4,y:1,w:1,h:2});
  assert.equal(customBlock.ok,false,"Manual placement must preserve custom access corridors");
  h.state.project.corridorRectangles="";
  const boundary=h.bridge.addManualPvModule({zoneId:"manual-zone",x:9.5,y:5.5,w:1,h:1});
  assert.equal(boundary.ok,false,"Manual placement must remain inside the surveyed polygon");
  assert.match(boundary.error,/roof boundary/);
  h.state.project.setback=0.5;
  const setback=h.bridge.addManualPvModule({zoneId:"manual-zone",x:0.1,y:2,w:1,h:1});
  assert.equal(setback.ok,false,"Manual placement must respect configured roof-edge setback");

  const wrongZone=h.bridge.addManualPvModule({zoneId:"missing-zone",x:4,y:1,w:1,h:1});
  assert.equal(wrongZone.ok,false,"Manual placement must require an actual saved roof-zone ID");
  const invalidDimension=h.bridge.addManualPvModule({zoneId:"manual-zone",x:4,y:1,w:0,h:1});
  assert.equal(invalidDimension.ok,false,"Manual placement must reject zero-sized modules");
  const nonFinite=h.bridge.addManualPvModule({zoneId:"manual-zone",x:Infinity,y:1,w:1,h:1});
  assert.equal(nonFinite.ok,false,"Manual placement must reject non-finite coordinates");
  assert.equal(h.bridge.selectPvModuleAt(Infinity,1),false,"Module selection must reject non-finite coordinates");
}
{
  const h=createHarness(false);
  h.state.roofZones=[{
    id:"manual-zone",name:"Manual roof",elevation:0,
    vertices:[{x:0,y:0},{x:10,y:0},{x:10,y:6},{x:0,y:6}]
  }];
  const before=JSON.stringify(h.model.layout);
  const result=h.bridge.addManualPvModule({zoneId:"manual-zone",x:4,y:1,w:1,h:2});
  assert.equal(result.ok,false,"Manual placement must report a failed local persistence transaction");
  assert.equal(JSON.stringify(h.model.layout),before,"Failed persistence must restore the previous module array");
  assert.equal(h.model.undo.length,0,"Failed persistence must discard its undo entry");
}

console.log("Native CAD/PV bridge tests passed: CAD geometry editing plus native manual PV placement, polygon/setback, obstacles, access corridors, overlap, selection, delete and persistence rollback.");
