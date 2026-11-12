(function(root){
'use strict';
const RAD=Math.PI/180, DEG=180/Math.PI;
function wrap360(v){return ((v%360)+360)%360}
function angularDifference(a,b){return Math.abs(((a-b+540)%360)-180)}
function solarPosition(date,time,latitude,longitude,timezoneOffsetMinutes){
 if(!/^\d{4}-\d{2}-\d{2}$/.test(date)||!/^\d{2}:\d{2}$/.test(time))throw new Error('Date and local time are required');
 const [year,month,day]=date.split('-').map(Number),[hour,minute]=time.split(':').map(Number);
 if(month<1||month>12||day<1||day>31||hour>23||minute>59)throw new Error('Invalid date or local time');
 if(!Number.isFinite(latitude)||latitude<-90||latitude>90||!Number.isFinite(longitude)||longitude<-180||longitude>180)throw new Error('Latitude or longitude is outside its valid range');
 if(!Number.isFinite(timezoneOffsetMinutes)||Math.abs(timezoneOffsetMinutes)>840)throw new Error('Invalid timezone offset');
 const utc=Date.UTC(year,month-1,day,hour,minute)-timezoneOffsetMinutes*60000;
 const dt=new Date(utc), doy=Math.floor((Date.UTC(dt.getUTCFullYear(),dt.getUTCMonth(),dt.getUTCDate())-Date.UTC(dt.getUTCFullYear(),0,0))/86400000);
 const utcHour=dt.getUTCHours()+dt.getUTCMinutes()/60+dt.getUTCSeconds()/3600;
 const gamma=2*Math.PI/365*(doy-1+(utcHour-12)/24);
 const eq=229.18*(.000075+.001868*Math.cos(gamma)-.032077*Math.sin(gamma)-.014615*Math.cos(2*gamma)-.040849*Math.sin(2*gamma));
 const decl=.006918-.399912*Math.cos(gamma)+.070257*Math.sin(gamma)-.006758*Math.cos(2*gamma)+.000907*Math.sin(2*gamma)-.002697*Math.cos(3*gamma)+.00148*Math.sin(3*gamma);
 const localMinutes=hour*60+minute,trueSolar=((localMinutes+eq+4*longitude-timezoneOffsetMinutes)%1440+1440)%1440,ha=(trueSolar/4-180)*RAD,phi=latitude*RAD;
 const sinAlt=Math.sin(phi)*Math.sin(decl)+Math.cos(phi)*Math.cos(decl)*Math.cos(ha),alt=Math.asin(Math.max(-1,Math.min(1,sinAlt)));
 const az=wrap360(Math.atan2(Math.sin(ha),Math.cos(ha)*Math.sin(phi)-Math.tan(decl)*Math.cos(phi))*DEG+180);
 return {altitude:alt*DEG,azimuth:az,equationOfTimeMin:eq,declinationDeg:decl*DEG,trueSolarTimeMin:trueSolar,dayOfYear:doy};
}
function assess(input){
 const p=solarPosition(input.date,input.time,input.latitude,input.longitude,input.timezoneOffsetMinutes);
 const height=Number(input.obstructionHeight),distance=Number(input.obstructionDistance),bearing=wrap360(Number(input.obstructionAzimuth)),width=Number(input.obstructionWidthDeg);
 if(!Number.isFinite(height)||height<0||!Number.isFinite(distance)||distance<0||!Number.isFinite(width)||width<0||width>360)throw new Error('Obstruction height, distance or angular width is invalid');
 const shadow=p.altitude>0.01?height/Math.tan(p.altitude*RAD):null,delta=angularDifference(p.azimuth,bearing);
 const directional=delta<=width/2,blocks=p.altitude>0.01&&shadow!==null&&shadow>=distance&&directional;
 return {...p,shadowLengthM:shadow,angularDifferenceDeg:delta,directionallyAligned:directional,potentialDirectShade:blocks};
}
function dayProfile(input,stepMinutes){
 const step=Number(stepMinutes||60);if(!Number.isInteger(step)||step<15||step>180||1440%step!==0)throw new Error('Profile interval must divide 24 hours and be 15–180 minutes');
 const rows=[];for(let m=0;m<1440;m+=step){const hh=String(Math.floor(m/60)).padStart(2,'0'),mm=String(m%60).padStart(2,'0');rows.push(assess({...input,time:hh+':'+mm}))}return rows;
}
root.GenSolarShadeEngine=Object.freeze({solarPosition,assess,dayProfile,angularDifference});
})(typeof window!=='undefined'?window:globalThis);
