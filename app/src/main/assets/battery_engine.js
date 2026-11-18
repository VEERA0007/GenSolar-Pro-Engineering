(function(root,factory){
  const api=factory();
  if(typeof module!=='undefined'&&module.exports)module.exports=api;
  if(root)root.GenSolarBatteryEngine=api;
})(typeof window!=='undefined'?window:globalThis,function(){
  'use strict';
  const fail=message=>({ok:false,error:message});
  const read=(input,key)=>Number(input[key]);
  function calculate(input){
    if(!input||typeof input!=='object')return fail('Battery design inputs are required.');
    const v={
      loadKw:read(input,'loadKw'),
      backupHours:read(input,'backupHours'),
      dodPercent:read(input,'dodPercent'),
      dischargeEfficiencyPercent:read(input,'dischargeEfficiencyPercent'),
      busVoltage:read(input,'busVoltage'),
      moduleVoltage:read(input,'moduleVoltage'),
      moduleCapacityAh:read(input,'moduleCapacityAh'),
      moduleMaxDischargeA:read(input,'moduleMaxDischargeA'),
      moduleMaxChargeA:read(input,'moduleMaxChargeA'),
      chargerCurrentA:read(input,'chargerCurrentA')
    };
    if(!Object.values(v).every(Number.isFinite))return fail('All battery inputs must be finite numbers.');
    if(v.loadKw<=0||v.loadKw>10000)return fail('Connected load must be greater than 0 and no more than 10,000 kW.');
    if(v.backupHours<=0||v.backupHours>240)return fail('Backup duration must be greater than 0 and no more than 240 hours.');
    if(v.dodPercent<1||v.dodPercent>100)return fail('Depth of discharge must be between 1% and 100%. Use the battery manufacturer limit.');
    if(v.dischargeEfficiencyPercent<1||v.dischargeEfficiencyPercent>100)return fail('Discharge / inverter efficiency must be between 1% and 100%.');
    if(v.busVoltage<12||v.busVoltage>1500)return fail('Battery DC bus voltage must be between 12 V and 1,500 V.');
    if(v.moduleVoltage<=0||v.moduleVoltage>1500||v.moduleVoltage>v.busVoltage)return fail('Battery module voltage must be positive and no higher than the DC bus voltage.');
    if(v.moduleCapacityAh<=0||v.moduleCapacityAh>5000)return fail('Battery module capacity must be between 0 and 5,000 Ah.');
    if(v.moduleMaxDischargeA<=0||v.moduleMaxDischargeA>10000)return fail('Module continuous discharge current must be between 0 and 10,000 A.');
    if(v.moduleMaxChargeA<=0||v.moduleMaxChargeA>10000)return fail('Module continuous charge current must be between 0 and 10,000 A.');
    if(v.chargerCurrentA<0||v.chargerCurrentA>100000)return fail('Charger current must be between 0 and 100,000 A; use 0 when not specified.');
    const ratio=v.busVoltage/v.moduleVoltage,seriesModules=Math.round(ratio);
    if(seriesModules<1||Math.abs(ratio-seriesModules)>1e-6)return fail('DC bus voltage must be an integer multiple of the selected module nominal voltage. Verify the manufacturer-approved series topology.');
    const usableEnergyKwh=v.loadKw*v.backupHours;
    const efficiency=v.dischargeEfficiencyPercent/100,dod=v.dodPercent/100;
    const requiredNominalKwh=usableEnergyKwh/(dod*efficiency);
    const moduleEnergyKwh=v.moduleVoltage*v.moduleCapacityAh/1000;
    const stringEnergyKwh=moduleEnergyKwh*seriesModules;
    const parallelStrings=Math.ceil(requiredNominalKwh/stringEnergyKwh);
    const totalModules=seriesModules*parallelStrings;
    if(!Number.isSafeInteger(totalModules)||totalModules<1||totalModules>2000)return fail('Calculated bank exceeds 2,000 modules. Recheck the system voltage, module size and design load.');
    const installedNominalKwh=stringEnergyKwh*parallelStrings;
    const installedUsableKwh=installedNominalKwh*dod*efficiency;
    const requiredBankAh=requiredNominalKwh*1000/v.busVoltage;
    const installedBankAh=v.moduleCapacityAh*parallelStrings;
    const designDischargeCurrentA=v.loadKw*1000/(v.busVoltage*efficiency);
    const availableDischargeCurrentA=v.moduleMaxDischargeA*parallelStrings;
    const availableChargeCurrentA=v.moduleMaxChargeA*parallelStrings;
    const dischargeCurrentPass=availableDischargeCurrentA+1e-9>=designDischargeCurrentA;
    const chargeCurrentPass=v.chargerCurrentA===0||availableChargeCurrentA+1e-9>=v.chargerCurrentA;
    const warnings=[];
    if(!dischargeCurrentPass)warnings.push('Parallel module strings do not meet the entered continuous discharge-current requirement.');
    if(!chargeCurrentPass)warnings.push('Parallel module strings do not meet the entered charger maximum-current requirement.');
    return{
      ok:true,
      status:warnings.length?'REVIEW REQUIRED':'WITHIN ENTERED MODULE CURRENT LIMITS',
      input:v,
      usableEnergyKwh,
      requiredNominalKwh,
      moduleEnergyKwh,
      stringEnergyKwh,
      seriesModules,
      parallelStrings,
      totalModules,
      installedNominalKwh,
      installedUsableKwh,
      requiredBankAh,
      installedBankAh,
      designDischargeCurrentA,
      availableDischargeCurrentA,
      availableChargeCurrentA,
      dischargeCurrentPass,
      chargerCurrentA:v.chargerCurrentA,
      chargeCurrentPass,
      warnings
    };
  }
  return Object.freeze({calculate});
});
