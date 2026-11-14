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
assert.deepEqual(Array.from(b.getProcurement().vendors[0]),['Module Supplier',125000,5,'Tier-1 modules','']);
const linkedQuote={name:'Vendor A',amount:118000,deliveryDays:6,notes:'RFQ compliant offer',rfqRef:'GS-RFQ-001'};
assert.equal(b.createVendorQuote(linkedQuote),true,'invited supplier can submit a quote against an issued RFQ');
assert.deepEqual(Array.from(b.getProcurement().vendors[1]),['Vendor A',118000,6,'RFQ compliant offer','GS-RFQ-001']);
assert.equal(b.createVendorQuote({...linkedQuote,name:'Uninvited Vendor'}),false,'supplier not invited to RFQ cannot be linked');
assert.equal(b.createVendorQuote({...linkedQuote,rfqRef:'UNKNOWN-RFQ'}),false,'quote cannot reference an unknown RFQ');
assert.equal(b.createVendorQuote({...linkedQuote,name:'Vendor B',rfqRef:'gs-rfq-001'}),true,'RFQ matching is case-insensitive and invited supplier may be linked');
for (const patch of [{name:''},{amount:0},{amount:-10},{deliveryDays:-1},{deliveryDays:1.5}]) {
  assert.equal(b.createVendorQuote({...quote,...patch}),false);
}
assert.equal(b.createVendorQuote({...quote,name:'module supplier'}),false,'supplier names are case-insensitive unique');
saved=false;
assert.equal(b.createVendorQuote({...quote,name:'Unsaved Supplier'}),false);
assert.equal(b.getProcurement().vendors.length,3,'failed save rolls back supplier quote');
saved=true;

context.state.bom=[['PV module','550 W TOPCon','Nos',20,0],['DC cable','6 sqmm','m',250,0]];
assert.equal(b.getProcurement().bom.length,2,'native procurement data exposes the active project BOM');
assert.deepEqual(Array.from(b.getProcurement().bom[0]),['PV module','550 W TOPCon','Nos',20,0]);
const po={ref:'GS-PO-001',vendor:'Solar Supplier',amount:250000,qty:100,unit:'Nos',date:'2026-12-01',status:'Ordered'};
assert.equal(b.createPurchaseOrder(po),true);
assert.equal(b.getProcurement().purchaseOrders.length,1);
for(const patch of [{ref:''},{amount:0},{qty:-1},{date:'2026-02-30'},{unit:''},{status:'Cancelled'}]) assert.equal(b.createPurchaseOrder({...po,...patch}),false);
const linkedPo={ref:'GS-PO-002',vendor:'Vendor A',amount:118000,qty:120,unit:'Nos',date:'2026-12-03',status:'Ordered',rfqRef:'GS-RFQ-001'};
assert.equal(b.createPurchaseOrder(linkedPo),true,'PO can be created from an invited supplier quote linked to the RFQ');
assert.equal(b.getProcurement().purchaseOrders[1][7],'GS-RFQ-001','source RFQ reference persists on PO');
assert.equal(b.createPurchaseOrder({...linkedPo,ref:'GS-PO-003',vendor:'Uninvited Vendor'}),false,'PO rejects supplier not invited to source RFQ');
assert.equal(b.createPurchaseOrder({...linkedPo,ref:'GS-PO-004',rfqRef:'UNKNOWN-RFQ'}),false,'PO rejects unknown source RFQ');
assert.equal(b.createPurchaseOrder({...linkedPo,ref:'GS-PO-005',vendor:'Module Supplier'}),false,'PO requires a quotation linked to the same RFQ');

const poStatus=()=>b.getProcurement().purchaseOrders[0][4];
assert.equal(b.updatePurchaseOrderStatus(po.ref,'On hold'),true);
assert.equal(poStatus(),'On hold');
assert.equal(b.getProcurement().history.length,1);
assert.equal(b.updatePurchaseOrderStatus(po.ref,'Received'),false,'receipt-derived status cannot be selected manually');
assert.equal(b.updatePurchaseOrderStatus(po.ref,'Partially received'),false,'partial receipt status cannot be selected manually');
assert.equal(b.updatePurchaseOrderStatus(po.ref,'On hold'),false,'same status is not a transition');
assert.equal(b.updatePurchaseOrderStatus(po.ref,'Ordered'),true);
assert.equal(poStatus(),'Ordered');
const historyBeforeRollback=b.getProcurement().history.length;
saved=false;
assert.equal(b.updatePurchaseOrderStatus(po.ref,'On hold'),false,'failed persistence must reject status update');
assert.equal(poStatus(),'Ordered','failed status update must restore previous PO status');
assert.equal(b.getProcurement().history.length,historyBeforeRollback,'failed status update must roll back audit history');
const receipt={poRef:po.ref,qty:40,unit:'Nos',date:'2026-11-20',condition:'Good',receiver:'Store Manager',documentRef:'DC-001',remarks:''};
assert.equal(b.createGoodsReceipt(receipt),false,'failed receipt persistence must be rolled back');
assert.equal(b.getProcurement().receipts.length,0);
assert.equal(poStatus(),'Ordered');
assert.equal(b.getProcurement().history.length,historyBeforeRollback);
saved=true;
assert.equal(b.createGoodsReceipt(receipt),true);
assert.equal(poStatus(),'Partially received');
assert.equal(b.getProcurement().history.length,historyBeforeRollback+1);
assert.equal(b.getProcurement().history.at(-1)[1],'Ordered');
assert.equal(b.getProcurement().history.at(-1)[2],'Partially received');
assert.equal(b.createGoodsReceipt({...receipt,qty:60,documentRef:'DC-002'}),true);
assert.equal(poStatus(),'Received');
assert.equal(b.getProcurement().receipts.length,2);
assert.equal(b.getProcurement().history.at(-1)[1],'Partially received');
assert.equal(b.getProcurement().history.at(-1)[2],'Received');
for(const patch of [{qty:1,unit:'Kg'},{qty:1,condition:'Damaged'},{qty:1,date:'2026-02-30'},{qty:1,receiver:''},{qty:1,poRef:'unknown'},{qty:1.1}]) assert.equal(b.createGoodsReceipt({...receipt,...patch}),false);
saved=false;
assert.equal(b.createGoodsReceipt({...receipt,qty:1}),false,'over-receipt must be rejected before persistence');
assert.equal(b.getProcurement().receipts.length,2);
assert.equal(poStatus(),'Received');

saved=true;
const multiLinePo={ref:'GS-PO-MULTI-001',vendor:'Solar Supplier',date:'2026-12-05',status:'Ordered',lines:[
  {item:'550 W PV module',qty:20,unit:'Nos',rate:8500},
  {item:'DC cable',qty:20,unit:'Nos',rate:1250},
  {item:'MC4 connector pair',qty:10,unit:'Nos',rate:180}
]};
assert.equal(b.createPurchaseOrder(multiLinePo),true,'valid multi-line PO is accepted');
const multiSaved=b.getProcurement().purchaseOrders.find(row=>row[0]===multiLinePo.ref);
assert.equal(multiSaved[2],196800,'PO amount is calculated from each line quantity multiplied by rate');
assert.equal(multiSaved[5],50,'PO quantity is the sum of line quantities for a common unit');
assert.equal(multiSaved[6],'Nos');
assert.equal(multiSaved[8].length,3,'material line breakdown persists on the PO');
assert.equal(multiSaved[8][0].amount,170000);
assert.equal(b.createGoodsReceipt({poRef:'GS-PO-MULTI-001',item:'550 W PV module',qty:12,unit:'Nos',date:'2026-12-06',condition:'Good',receiver:'Store Manager'}),true,'item-wise receipt is accepted for a valid PO material');
assert.equal(b.getProcurement().receipts.at(-1)[8],'550 W PV module','receipt stores the received material identity');
assert.equal(b.createGoodsReceipt({poRef:'GS-PO-MULTI-001',item:'550 W PV module',qty:9,unit:'Nos',date:'2026-12-06',condition:'Good',receiver:'Store Manager'}),false,'item-wise receipt cannot exceed its material line quantity');
assert.equal(b.createGoodsReceipt({poRef:'GS-PO-MULTI-001',item:'Unknown material',qty:1,unit:'Nos',date:'2026-12-06',condition:'Good',receiver:'Store Manager'}),false,'receipt rejects materials not present on the PO');
assert.equal(b.createGoodsReceipt({poRef:'GS-PO-MULTI-001',item:'DC cable',qty:5,unit:'m',date:'2026-12-06',condition:'Good',receiver:'Store Manager'}),false,'receipt enforces the selected material unit');
assert.equal(b.createGoodsReceipt({poRef:'GS-PO-MULTI-001',item:'DC cable',qty:5,unit:'Nos',date:'2026-12-06',condition:'Good',receiver:'Store Manager'}),true,'separate material line receipts are tracked independently');
const stockLedger=b.getProcurement().stockLedger;
assert.equal(stockLedger.length,3,'stock ledger contains one row for each PO material line');
const moduleStock=stockLedger.find(row=>row[1]==='550 W PV module');
assert.equal(moduleStock[3],20);
assert.equal(moduleStock[4],12);
assert.equal(moduleStock[5],12,'Good receipts count as available stock');
assert.equal(moduleStock[9],8,'stock ledger calculates pending delivery by item');
const cableStock=stockLedger.find(row=>row[1]==='DC cable');
assert.equal(cableStock[4],5);
assert.equal(cableStock[5],5);
assert.equal(b.createStockIssue({issueRef:'GS-ISS-001',poRef:'GS-PO-MULTI-001',item:'550 W PV module',qty:4,date:'2026-12-07',issuedTo:'Site Engineer',workPackage:'Module mounting',remarks:'Roof block A'}),true,'stock issue consumes only available Good-condition material');
assert.equal(b.getProcurement().stockIssues.length,1);
let issuedModule=b.getProcurement().stockLedger.find(row=>row[1]==='550 W PV module');
assert.equal(issuedModule[10],4,'stock ledger totals material issued to site');
assert.equal(issuedModule[11],8,'on-hand stock equals Good receipts less material issues');
assert.equal(b.createStockIssue({issueRef:'GS-ISS-002',poRef:'GS-PO-MULTI-001',item:'550 W PV module',qty:9,date:'2026-12-07',issuedTo:'Site Engineer',workPackage:'Module mounting'}),false,'stock issue cannot exceed available on-hand quantity');
assert.equal(b.createStockIssue({issueRef:'GS-ISS-003',poRef:'GS-PO-MULTI-001',item:'Unknown item',qty:1,date:'2026-12-07',issuedTo:'Site Engineer',workPackage:'Module mounting'}),false,'stock issue rejects unknown PO material');
assert.equal(b.createStockIssue({issueRef:'GS-ISS-001',poRef:'GS-PO-MULTI-001',item:'550 W PV module',qty:1,date:'2026-12-07',issuedTo:'Site Engineer',workPackage:'Module mounting'}),false,'stock issue references must be unique');
assert.equal(b.createStockIssue({issueRef:'GS-ISS-004',poRef:'GS-PO-MULTI-001',item:'550 W PV module',qty:1,date:'2026-02-30',issuedTo:'Site Engineer',workPackage:'Module mounting'}),false,'stock issue date must be a real ISO date');
saved=false;
assert.equal(b.createStockIssue({issueRef:'GS-ISS-005',poRef:'GS-PO-MULTI-001',item:'550 W PV module',qty:1,date:'2026-12-07',issuedTo:'Site Engineer',workPackage:'Module mounting'}),false,'failed stock issue persistence is rejected');
assert.equal(b.getProcurement().stockIssues.length,1,'failed stock issue save rolls back inventory issue');
saved=true;



assert.equal(b.createPurchaseOrder({...multiLinePo,ref:'GS-PO-MULTI-002',lines:[]}),false,'empty line list is rejected');
assert.equal(b.createPurchaseOrder({...multiLinePo,ref:'GS-PO-MULTI-003',lines:Array.from({length:31},(_,i)=>({item:'Item '+i,qty:1,unit:'Nos',rate:1}))}),false,'more than 30 lines are rejected');
assert.equal(b.createPurchaseOrder({...multiLinePo,ref:'GS-PO-MULTI-004',lines:[{item:'Panel',qty:1,unit:'Nos',rate:10},{item:'Cable',qty:1,unit:'m',rate:5}]}),false,'mixed units are rejected to protect legacy receipt reconciliation');
assert.equal(b.createPurchaseOrder({...multiLinePo,ref:'GS-PO-MULTI-005',lines:[{item:'Panel',qty:0,unit:'Nos',rate:10}]}),false,'zero quantity line is rejected');
saved=false;
assert.equal(b.createPurchaseOrder({...multiLinePo,ref:'GS-PO-MULTI-006'}),false,'failed multi-line PO persistence is rejected');
assert.equal(b.getProcurement().purchaseOrders.some(row=>row[0]==='GS-PO-MULTI-006'),false,'failed multi-line PO save rolls back the PO');
saved=true;


console.log('Native procurement bridge tests passed: RFQ creation, invited-supplier quote traceability, duplicate references, rollback, RFQ-to-quote-to-PO traceability, PO lifecycle and goods receipt reconciliation.');
