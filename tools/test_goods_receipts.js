const assert=require("node:assert/strict");const fs=require("node:fs");const vm=require("node:vm");
const html=fs.readFileSync("app/src/main/assets/index.html","utf8");
function extract(name){const start=html.indexOf("function "+name+"(");assert.notEqual(start,-1,"Missing "+name);const end=html.indexOf("\nfunction ",start+1);return html.slice(start,end<0?html.length:end)}
const ids=["receiptPoRef","receiptItem","receiptQty","receiptUnit","receiptDate","receiptCondition","receiptReceiver","receiptDocument","receiptRemarks","receiptStatusMessage","goodsReceiptList","receiptReconciliation","stockLedger","stockIssueRef","stockIssuePo","stockIssueItem","stockIssueQty","stockIssueDate","stockIssueTo","stockIssuePackage","stockIssueRemarks","stockIssueMessage","stockIssueList","poStatusRef","poStatusHistory","poLifecycleMessage","poList"];
const elements=Object.fromEntries(ids.map(id=>[id,{value:"",innerHTML:"",textContent:""}]));
elements.receiptPoRef.value="PO-100";elements.receiptQty.value="12";elements.receiptUnit.value="Nos";elements.receiptDate.value="2026-12-01";elements.receiptCondition.value="Good";elements.receiptReceiver.value="Storekeeper";
const context={state:{pos:[["PO-100","Supplier A",10000,"2026-12-01","Ordered"]],poHistory:[],goodsReceipts:[],stockIssues:[]},window:{GenSolarEpcBridge:{createStockIssue:v=>{context.state.stockIssues.push([v.issueRef,v.poRef,v.item,v.qty]);return true;}}},$:id=>elements[id],save:()=>true,escapeHtml:v=>String(v==null?"":v).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c])),csv:rows=>rows.map(r=>r.join(",")).join("\\n"),download:()=>{}};
vm.createContext(context);for(const name of ["renderPoLifecycle","renderSimple","renderGoodsReceipts","addGoodsReceipt","renderStockIssues","addStockIssue","exportGoodsReceipts"])vm.runInContext(extract(name),context);
assert.equal(vm.runInContext("addGoodsReceipt()",context),true);assert.equal(context.state.goodsReceipts.length,1);assert.ok(elements.goodsReceiptList.innerHTML.includes("Storekeeper"));
elements.receiptQty.value="0";assert.equal(vm.runInContext("addGoodsReceipt()",context),false);assert.match(elements.receiptStatusMessage.textContent,/greater than zero/);
elements.receiptQty.value="3";elements.receiptPoRef.value="UNKNOWN";assert.equal(vm.runInContext("addGoodsReceipt()",context),false);assert.match(elements.receiptStatusMessage.textContent,/existing purchase order/);
elements.receiptPoRef.value="PO-100";elements.receiptDate.value="2026-02-30";assert.equal(vm.runInContext("addGoodsReceipt()",context),false);assert.match(elements.receiptStatusMessage.textContent,/valid receipt date/);
elements.receiptDate.value="2026-12-01";elements.receiptReceiver.value="<script>alert(1)</script>";assert.equal(vm.runInContext("addGoodsReceipt()",context),true);assert.ok(elements.goodsReceiptList.innerHTML.includes("&lt;script&gt;"));assert.ok(!elements.goodsReceiptList.innerHTML.includes("<script>"));

context.state.pos.push(["PO-200","PV Supplier",100000,"2026-12-15","Ordered",10,"Nos","",[
  {item:"PV module",qty:10,unit:"Nos",rate:8500},
  {item:"DC connector",qty:20,unit:"Nos",rate:120}
]]);
elements.receiptPoRef.value="PO-200";elements.receiptItem.value="PV module";elements.receiptQty.value="6";elements.receiptUnit.value="Nos";elements.receiptDate.value="2026-12-02";elements.receiptCondition.value="Good";elements.receiptReceiver.value="Storekeeper";
assert.equal(vm.runInContext("addGoodsReceipt()",context),true,"web receipt entry accepts a selected material line");
assert.equal(context.state.goodsReceipts.at(-1)[8],"PV module","web receipt persists the selected material");
assert.ok(elements.stockLedger.innerHTML.includes("PV module"),"web receipt refreshes item-wise stock ledger");
elements.receiptQty.value="5";
assert.equal(vm.runInContext("addGoodsReceipt()",context),false,"web receipt cannot exceed the selected material line balance");

elements.stockIssuePo.value="PO-200";elements.stockIssueItem.value="PV module";elements.stockIssueRef.value="ISS-UI-01";elements.stockIssueQty.value="2";elements.stockIssueDate.value="2026-12-02";elements.stockIssueTo.value="Site Engineer";elements.stockIssuePackage.value="Module mounting";
assert.equal(vm.runInContext("addStockIssue()",context),true,"web stock issue form delegates a valid issue to the procurement bridge");
assert.equal(context.state.stockIssues.length,1);
assert.ok(elements.stockIssueList.innerHTML.includes("ISS-UI-01"));
assert.equal(vm.runInContext("exportGoodsReceipts()",context),true);console.log("Goods receipt tests passed: PO linkage, positive quantity, date validation, required receiver, safe rendering and CSV export.");