const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('app/src/main/assets/index.html','utf8');
const start=html.indexOf('window.GenSolarEpcBridge = Object.freeze({');
const end=html.indexOf('window.GenSolarStudio=',start);
assert.ok(start>=0&&end>start);
let saved=true;
const context={window:{},state:{project:{},pos:[],vendors:[],poHistory:[],goodsReceipts:[]},model:{},go:()=>{},save:()=>saved,renderPoLifecycle:()=>{},renderGoodsReceipts:()=>{},renderSimple:()=>{},activityPlannedFinish:()=>'',renderActivitySchedule:()=>{},staadGenerate:()=>true,buildBom:()=>true,renderBom:()=>{}};
vm.createContext(context);vm.runInContext(html.slice(start,end),context);
const b=context.window.GenSolarEpcBridge;

const rfq={ref:'GS-RFQ-001',item:'550 W mono TOPCon PV module',qty:120,unit:'Nos',dueDate:'2026-12-01',suppliers:['Vendor A','Vendor B']};
assert.equal(b.createRfq(rfq),true);
assert.equal(b.getProcurement().rfqs.length,1);
assert.deepEqual(Array.from(b.getProcurement().rfqs[0]).slice(0,5),['GS-RFQ-001','550 W mono TOPCon PV module',120,'Nos','2026-12-01']);
assert.deepEqual(Array.from(b.getProcurement().rfqs[0][5]),['Vendor A','Vendor B']);
for(const patch of [{ref:''},{item:''},{qty:0},{qty:-1},{unit:''},{dueDate:'2026-02-30'},{suppliers:[]},{suppliers:['Vendor A','vendor a']}]) assert.equal(b.createRfq({...rfq,...patch}),false);
assert.equal(b.createRfq({...rfq,ref:'gs-rfq-001'}),false,'RFQ references are case-insensitive unique');
saved=false;
assert.equal(b.createRfq({...rfq,ref:'GS-RFQ-002'}),false);
assert.equal(b.getProcurement().rfqs.length,1,'failed RFQ save rolls back the request');
saved=true;

const quote = {name:'Module Supplier',amount:125000,deliveryDays:5,notes:'Tier-1 modules'};
assert.equal(b.createVendorQuote(quote),true);
assert.equal(b.getProcurement().vendors.length,1);
assert.deepEqual(Array.from(b.getProcurement().vendors[0]),['Module Supplier',125000,5,'Tier-1 modules']);
for (const patch of [{name:''},{amount:0},{amount:-10},{deliveryDays:-1},{deliveryDays:1.5}]) {
  assert.equal(b.createVendorQuote({...quote,...patch}),false);
}
assert.equal(b.createVendorQuote({...quote,name:'module supplier'}),false,'supplier names are case-insensitive unique');
saved=false;
assert.equal(b.createVendorQuote({...quote,name:'Unsaved Supplier'}),false);
assert.equal(b.getProcurement().vendors.length,1,'failed save rolls back supplier quote');
saved=true;

const po={ref:'GS-PO-001',vendor:'Solar Supplier',amount:250000,qty:100,unit:'Nos',date:'2026-12-01',status:'Ordered'};
assert.equal(b.createPurchaseOrder(po),true);
assert.equal(b.getProcurement().purchaseOrders.length,1);
for(const patch of [{ref:''},{amount:0},{qty:-1},{date:'2026-02-30'},{unit:''},{status:'Cancelled'}]) assert.equal(b.createPurchaseOrder({...po,...patch}),false);
const receipt={poRef:po.ref,qty:40,unit:'Nos',date:'2026-11-20',condition:'Good',receiver:'Store Manager',documentRef:'DC-001',remarks:''};
assert.equal(b.createGoodsReceipt(receipt),true);
assert.equal(b.getProcurement().purchaseOrders[0][4],'Partially received');
assert.equal(b.createGoodsReceipt({...receipt,qty:60,documentRef:'DC-002'}),true);
assert.equal(b.getProcurement().purchaseOrders[0][4],'Received');
assert.equal(b.getProcurement().receipts.length,2);
for(const patch of [{qty:1,unit:'Kg'},{qty:1,condition:'Damaged'},{qty:1,date:'2026-02-30'},{qty:1,receiver:''},{qty:1,poRef:'unknown'},{qty:1.1}]) assert.equal(b.createGoodsReceipt({...receipt,...patch}),false);
saved=false;
assert.equal(b.createGoodsReceipt({...receipt,qty:1}),false);
assert.equal(b.getProcurement().receipts.length,2);
assert.equal(b.getProcurement().purchaseOrders[0][4],'Received');
console.log('Native procurement bridge tests passed: RFQ creation, supplier invite validation, duplicate references, RFQ save rollback, PO creation, receipt reconciliation, status automation and rollback.');
