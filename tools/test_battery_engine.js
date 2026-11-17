const assert = require('node:assert/strict');
const { calculate } = require('../app/src/main/assets/battery_engine.js');

const typical = {
  loadKw: 3,
  backupHours: 8,
  dodPercent: 80,
  dischargeEfficiencyPercent: 90,
  busVoltage: 51.2,
  moduleVoltage: 12.8,
  moduleCapacityAh: 100,
  moduleMaxDischargeA: 100,
  moduleMaxChargeA: 50,
  chargerCurrentA: 0
};

const sized = calculate(typical);
assert.equal(sized.ok, true);
assert.equal(sized.usableEnergyKwh, 24);
assert.ok(Math.abs(sized.requiredNominalKwh - 33.3333333333) < 1e-8);
assert.equal(sized.seriesModules, 4);
assert.equal(sized.parallelStrings, 7);
assert.equal(sized.totalModules, 28);
assert.equal(sized.installedNominalKwh, 35.84);
assert.equal(sized.installedBankAh, 700);
assert.equal(sized.dischargeCurrentPass, true);
assert.equal(sized.chargeCurrentPass, true);
assert.equal(sized.status, 'WITHIN ENTERED MODULE CURRENT LIMITS');

assert.equal(calculate({...typical, loadKw: 0}).ok, false);
assert.equal(calculate({...typical, dodPercent: 0}).ok, false);
assert.equal(calculate({...typical, dischargeEfficiencyPercent: 101}).ok, false);
assert.equal(calculate({...typical, busVoltage: 48}).ok, false);
assert.equal(calculate({...typical, moduleCapacityAh: Infinity}).ok, false);

const dischargeOverload = calculate({...typical, moduleMaxDischargeA: 5});
assert.equal(dischargeOverload.ok, true);
assert.equal(dischargeOverload.dischargeCurrentPass, false);
assert.equal(dischargeOverload.status, 'REVIEW REQUIRED');
assert.ok(dischargeOverload.warnings.some(x => x.includes('discharge-current')));

const chargerOverload = calculate({...typical, chargerCurrentA: 400});
assert.equal(chargerOverload.ok, true);
assert.equal(chargerOverload.chargeCurrentPass, false);
assert.ok(chargerOverload.warnings.some(x => x.includes('charger maximum-current')));

const noChargeInput = calculate({...typical, chargerCurrentA: 0});
assert.equal(noChargeInput.chargeCurrentPass, true);
assert.equal(noChargeInput.chargerCurrentA, 0);

console.log('Battery bank engine passed: energy reserve, series/parallel sizing, Ah, discharge/charge current limits and invalid-input rejection.');
