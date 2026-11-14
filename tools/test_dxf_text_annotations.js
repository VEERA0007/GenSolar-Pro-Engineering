const assert=require("node:assert/strict");
const fs=require("node:fs");
const source=fs.readFileSync("app/src/main/assets/index.html","utf8");
const p0=source.indexOf("function parseDxf(text){");
const p1=source.indexOf("function cadDxfSummary(",p0);
const r0=source.indexOf("function renderImportedDxf(data){",p1);
const r1=source.indexOf("function importCad(file)",r0);
assert.ok(p0>=0&&p1>p0&&r0>p1&&r1>r0,"DXF parser and preview functions must exist");
const parse=source.slice(p0,p1);
const render=source.slice(r0,r1);
const context={escapeHtml:value=>String(value).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]))};
const parsed=new Function("context",parse+";return parseDxf")(context);
const preview=new Function("context",render+";return renderImportedDxf")(context);
const input=[
"0","SECTION","2","HEADER","9","$INSUNITS","70","4","0","ENDSEC",
"0","SECTION","2","ENTITIES",
"0","LWPOLYLINE","8","ROOF_ZONES","90","4","70","1","10","0","20","0","10","10000","20","0","10","10000","20","5000","10","0","20","5000",
"0","TEXT","8","ROOF_ZONE_LABELS","10","100","20","200","40","180","50","0","1","Main roof | EL 6.25 m | PITCH 12.5 deg",
"0","MTEXT","8","NOTES","10","500","20","800","40","120","1","PV layout\\PRevision 3",
"0","ENDSEC","0","EOF"
].join("\n");
const result=parsed(input);
assert.equal(result.units,4);
assert.equal(result.entities.length,3);
assert.equal(result.counts.TEXT,1);
assert.equal(result.counts.MTEXT,1);
assert.equal(result.entities[1].text,"Main roof | EL 6.25 m | PITCH 12.5 deg");
assert.equal(result.entities[2].text,"PV layout\\PRevision 3");
assert.ok(result.bounds.maxX>=10000&&result.bounds.maxY>=5000,"Text insertion points must be included in extents");
const html=preview(result);
assert.ok(html.includes("<text"),"Text annotations must appear in the SVG preview");
assert.ok(html.includes("Main roof | EL 6.25 m | PITCH 12.5 deg"));
assert.ok(html.includes("PV layout"),"MTEXT annotations must appear in the preview");
const unsafe=parsed(["0","SECTION","2","ENTITIES","0","TEXT","8","NOTES","10","0","20","0","40","1","1","<script>alert(1)</script>","0","ENDSEC","0","EOF"].join("\n"));
assert.ok(preview(unsafe).includes("&lt;script&gt;alert(1)&lt;/script&gt;"),"DXF text must be HTML escaped");
assert.ok(!preview(unsafe).includes("<script>alert(1)</script>"),"DXF text must not inject SVG markup");
console.log("DXF TEXT/MTEXT parsing, extents, preview rendering and untrusted annotation escaping passed.");
