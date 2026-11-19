const assert=require('node:assert/strict');
require('../app/src/main/assets/shade_engine.js');
const e=global.GenSolarShadeEngine;
assert.ok(e,'shade engine global exported');
const hyderabad={date:'2026-06-21',time:'12:00',latitude:17.385,longitude:78.4867,timezoneOffsetMinutes:330};
const noon=e.solarPosition(hyderabad.date,hyderabad.time,hyderabad.latitude,hyderabad.longitude,hyderabad.timezoneOffsetMinutes);
assert.ok(noon.altitude>80&&noon.altitude<=90,'Hyderabad summer-solstice solar altitude near overhead');
assert.ok(noon.azimuth>=0&&noon.azimuth<360);
const sameInstant=e.solarPosition('2026-06-21','06:30',17.385,78.4867,0);
assert.ok(Math.abs(noon.altitude-sameInstant.altitude)<0.01&&e.angularDifference(noon.azimuth,sameInstant.azimuth)<0.01,'timezone representations of the same instant agree');
const morning=e.solarPosition('2026-06-21','09:00',17.385,78.4867,330);
const afternoon=e.solarPosition('2026-06-21','15:00',17.385,78.4867,330);
assert.ok(morning.azimuth<180&&afternoon.azimuth>180,'morning sun east, afternoon sun west');
const shade=e.assess({...hyderabad,time:'09:00',obstructionHeight:3,obstructionDistance:4,obstructionAzimuth:morning.azimuth,obstructionWidthDeg:30});
assert.equal(shade.directionallyAligned,true);
assert.ok(shade.shadowLengthM>0);
assert.equal(shade.potentialDirectShade,shade.shadowLengthM>=4);
const away=e.assess({...hyderabad,time:'09:00',obstructionHeight:3,obstructionDistance:0,obstructionAzimuth:wrap(morning.azimuth+180),obstructionWidthDeg:20});
assert.equal(away.directionallyAligned,false);
const profile=e.dayProfile({...hyderabad,obstructionHeight:2,obstructionDistance:5,obstructionAzimuth:180,obstructionWidthDeg:45},60);
assert.equal(profile.length,24);
assert.ok(profile.every(x=>Number.isFinite(x.altitude)&&Number.isFinite(x.azimuth)));
assert.throws(()=>e.solarPosition('2026-06-21','09:00',91,78,-330),/valid range/);
assert.throws(()=>e.solarPosition('2026-02-29','09:00',17.385,78.4867,330),/calendar date/,'non-leap February 29 must be rejected');
assert.doesNotThrow(()=>e.solarPosition('2024-02-29','09:00',17.385,78.4867,330),'valid leap-day input is accepted');
assert.throws(()=>e.solarPosition('2026-04-31','09:00',17.385,78.4867,330),/calendar date/,'April has only 30 days');
assert.throws(()=>e.solarPosition('1900-02-29','09:00',17.385,78.4867,330),/calendar date/,'1900 is not a Gregorian leap year');
assert.doesNotThrow(()=>e.solarPosition('2000-02-29','09:00',17.385,78.4867,330),'2000 is a Gregorian leap year');
assert.throws(()=>e.solarPosition('2100-02-29','09:00',17.385,78.4867,330),/calendar date/,'2100 is not a Gregorian leap year');
assert.throws(()=>e.solarPosition('0000-01-01','09:00',17.385,78.4867,330),/Invalid date/,'year zero is outside the supported calendar');
assert.throws(()=>e.solarPosition('2026-06-21','24:00',17.385,78.4867,330),/Invalid date/,'24:00 is not a valid clock time');
assert.throws(()=>e.assess({...hyderabad,obstructionHeight:2,obstructionDistance:5,obstructionAzimuth:180,obstructionWidthDeg:NaN}),/width/,'non-finite obstruction width must be rejected');
assert.throws(()=>e.assess({...hyderabad,obstructionHeight:2,obstructionDistance:5,obstructionAzimuth:NaN,obstructionWidthDeg:30}),/bearing/,'non-finite obstruction bearing must be rejected');
assert.throws(()=>e.assess({...hyderabad,obstructionHeight:2,obstructionDistance:5,obstructionAzimuth:361,obstructionWidthDeg:30}),/bearing/,'bearing above 360 degrees must be rejected');
assert.throws(()=>e.dayProfile(hyderabad,50),/interval/);
function wrap(v){return ((v%360)+360)%360}

// Multiple-obstruction aggregation: primary plus up to eight additional entries.
const multiInput={...hyderabad,time:'09:00',obstructionHeight:2,obstructionDistance:5,obstructionAzimuth:180,obstructionWidthDeg:45};
const multiSolar=e.solarPosition(multiInput.date,multiInput.time,multiInput.latitude,multiInput.longitude,multiInput.timezoneOffsetMinutes);
const alignedBearing=multiSolar.azimuth;
const multi=e.assessMultiple(multiInput,[
 {obstructionHeight:4,obstructionDistance:0,obstructionAzimuth:alignedBearing,obstructionWidthDeg:12},
 {obstructionHeight:3,obstructionDistance:50,obstructionAzimuth:wrap(alignedBearing+180),obstructionWidthDeg:10}
]);
assert.equal(multi.obstructionCount,3,'primary obstruction plus two additional entries');
assert.equal(multi.blockingCount,1,'only the aligned zero-distance obstruction blocks');
assert.equal(multi.potentialDirectShade,true);
assert.ok(multi.results.every(x=>Number.isFinite(x.altitude)&&Number.isFinite(x.azimuth)));
assert.throws(()=>e.assessMultiple(multiInput,Array(9).fill({obstructionHeight:1,obstructionDistance:1,obstructionAzimuth:0,obstructionWidthDeg:0})),/eight additional/);
const multiProfile=e.dayProfileMultiple(multiInput,[
 {obstructionHeight:4,obstructionDistance:0,obstructionAzimuth:alignedBearing,obstructionWidthDeg:12}
],60);
assert.equal(multiProfile.length,24);
assert.ok(multiProfile.every(x=>x.obstructionCount===2&&x.results.length===2));
assert.ok(multiProfile.some(x=>x.blockingCount>0),'day profile should identify blocking samples for aligned obstruction');
assert.equal(multiProfile[0].time,'00:00','profile starts at local midnight');
assert.equal(multiProfile.at(-1).time,'23:30','30-minute profile ends at 23:30');
assert.ok(multiProfile.every(x=>Number.isFinite(x.altitude)&&Number.isFinite(x.azimuth)&&Number.isInteger(x.blockingCount)),'each profile row must expose valid solar position and obstruction count');
assert.ok(multiProfile.some(x=>x.altitude>0),'profile must include daylight samples');
assert.throws(()=>e.dayProfileMultiple(multiInput,[],50),/interval/);

console.log('Solar shade engine passed: timezone-aware NOAA solar position, directional obstruction screening, day profile and input validation.');
