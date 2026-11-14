const assert=require("node:assert/strict");const fs=require("node:fs");const vm=require("node:vm");
const html=fs.readFileSync("app/src/main/assets/index.html","utf8");
function extract(name){const start=html.indexOf("function "+name+"(");assert.notEqual(start,-1,"Missing "+name);const end=html.indexOf("\nfunction ",start+1);return html.slice(start,end<0?html.length:end)}
const elements={poStatusRef:{value:"PO-01",innerHTML:""},poNextStatus:{value:"Approved"},poLifecycleMessage:{textContent:""},poStatusHistory:{innerHTML:""},poList:{innerHTML:""}};
let saveResult=true;
const context={state:{pos:[["PO-01","Vendor A",10000,"2026-12-01","Draft"]],poHistory:[]},$:id=>elements[id],save:()=>saveResult,escapeHtml:v=>String(v==null?"":v).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c])),renderSimple:(id,rows)=>{elements[id].innerHTML=rows.map(row=>row.join(" · ")).join("\n")}};
vm.createContext(context);for(const name of ["renderPoLifecycle","updatePoStatus"])vm.runInContext(extract(name),context);
assert.equal(vm.runInContext("updatePoStatus()",context),true);assert.equal(context.state.pos[0][4],"Approved");assert.equal(context.state.poHistory.length,1);assert.match(elements.poLifecycleMessage.textContent,/updated/i);
assert.equal(vm.runInContext("updatePoStatus()",context),false);assert.match(elements.poLifecycleMessage.textContent,/already has this status/);
elements.poStatusRef.value="UNKNOWN";elements.poNextStatus.value="Ordered";assert.equal(vm.runInContext("updatePoStatus()",context),false);assert.match(elements.poLifecycleMessage.textContent,/existing purchase order/);
elements.poStatusRef.value="PO-01";elements.poNextStatus.value="<script>";assert.equal(vm.runInContext("updatePoStatus()",context),false);assert.match(elements.poLifecycleMessage.textContent,/valid purchase status/);
elements.poNextStatus.value="Received";assert.equal(vm.runInContext("updatePoStatus()",context),false);assert.match(elements.poLifecycleMessage.textContent,/automatically from validated goods receipts/);assert.equal(context.state.pos[0][4],"Approved");
elements.poNextStatus.value="Draft";assert.equal(vm.runInContext("updatePoStatus()",context),false);assert.match(elements.poLifecycleMessage.textContent,/not allowed from Approved/);assert.equal(context.state.pos[0][4],"Approved");
saveResult=false;elements.poNextStatus.value="Ordered";assert.equal(vm.runInContext("updatePoStatus()",context),false);assert.equal(context.state.pos[0][4],"Approved","failed persistence must restore the previous PO status");assert.equal(context.state.poHistory.length,1,"failed persistence must roll back the audit history");assert.match(elements.poLifecycleMessage.textContent,/previous status has been restored/);
saveResult=true;elements.poNextStatus.value="Ordered";assert.equal(vm.runInContext("updatePoStatus()",context),true);assert.equal(context.state.pos[0][4],"Ordered");assert.equal(context.state.poHistory.length,2);
console.log("PO lifecycle tests passed: valid transitions, receipt-owned statuses, terminal/reverse transition guards, audit history, invalid references and persistence rollback.");
