/* GenSolar in-app 2D portal-frame stiffness solver.
 * Units: geometry m, force kN, stress kN/m², area m², inertia m⁴.
 * This is a plane-frame solver; it does not solve out-of-plane purlins/bracing.
 */
(function(root){
  "use strict";
  function solveLinear(A,b){
    const n=b.length, M=A.map((r,i)=>r.slice().concat([b[i]]));
    for(let k=0;k<n;k++){
      let p=k; for(let i=k+1;i<n;i++) if(Math.abs(M[i][k])>Math.abs(M[p][k])) p=i;
      if(Math.abs(M[p][k])<1e-11) throw new Error("Singular stiffness matrix: check supports, geometry and member properties.");
      [M[k],M[p]]=[M[p],M[k]];
      const d=M[k][k]; for(let j=k;j<=n;j++) M[k][j]/=d;
      for(let i=0;i<n;i++){if(i===k)continue;const f=M[i][k];if(!f)continue;for(let j=k;j<=n;j++)M[i][j]-=f*M[k][j];}
    }
    return M.map(r=>r[n]);
  }
  function zeros(n,m){return Array.from({length:n},()=>Array(m).fill(0));}
  function matVec(A,x){return A.map(r=>r.reduce((s,v,i)=>s+v*x[i],0));}
  function transpose(A){return A[0].map((_,j)=>A.map(r=>r[j]));}
  function multiply(A,B){const Bt=transpose(B);return A.map(r=>Bt.map(c=>r.reduce((s,v,i)=>s+v*c[i],0)));}
  function elementMatrices(a,b,E,A,I){
    const dx=b.x-a.x,dy=b.y-a.y,L=Math.hypot(dx,dy),c=dx/L,s=dy/L;
    if(!(L>1e-8&&E>0&&A>0&&I>0))throw new Error("Every active portal member needs positive length, E, area and inertia.");
    const EA=E*A/L, EI=E*I, L2=L*L,L3=L2*L;
    const k=zeros(6,6);
    k[0][0]=k[3][3]=EA;k[0][3]=k[3][0]=-EA;
    const v=[[12*EI/L3,6*EI/L2,-12*EI/L3,6*EI/L2],
             [6*EI/L2,4*EI/L,-6*EI/L2,2*EI/L],
             [-12*EI/L3,-6*EI/L2,12*EI/L3,-6*EI/L2],
             [6*EI/L2,2*EI/L,-6*EI/L2,4*EI/L]];
    const ix=[1,2,4,5]; for(let i=0;i<4;i++)for(let j=0;j<4;j++)k[ix[i]][ix[j]]=v[i][j];
    const T=zeros(6,6);
    T[0][0]=c;T[0][1]=s;T[1][0]=-s;T[1][1]=c;T[2][2]=1;
    T[3][3]=c;T[3][4]=s;T[4][3]=-s;T[4][4]=c;T[5][5]=1;
    return {L,c,s,k,T,kg:multiply(transpose(T),multiply(k,T))};
  }
  function localLoad(loads,memberId,el,area,selfWeight){
    let gx=0,gy=0;
    loads.forEach(load=>(load.memberLoads||[]).forEach(q=>{if(q.member===memberId){if(q.axis==="GX")gx+=q.value;else if(q.axis==="GY")gy+=q.value;}}));
    if(selfWeight) gy-=76.8195*area;
    const qx=gx*el.c+gy*el.s, qy=-gx*el.s+gy*el.c, L=el.L;
    return [qx*L/2,qy*L/2,qy*L*L/12,qx*L/2,qy*L/2,-qy*L*L/12];
  }
  function solve(model,props,combinationId){
    if(!model||!Array.isArray(model.frames)||!Array.isArray(model.members)||!Array.isArray(model.nodes))throw new Error("Generate the portal-frame model first.");
    ["E","columnArea","columnI","rafterArea","rafterI"].forEach(k=>{if(!Number.isFinite(Number(props[k]))||Number(props[k])<=0)throw new Error("Enter a positive verified value for "+k+".");});
    const E=Number(props.E), results=[];
    let maxDisp=0,maxMoment=0,maxShear=0,maxAxial=0,allReactions=[];
    const combo=model.combinations.find(c=>c.id===Number(combinationId));
    const factors=combo?combo.factors:[[Number(combinationId),1]];
    const loadMap=new Map(model.loads.map(l=>[l.id,l]));
    const activeLoads=factors.map(([id,f])=>{const l=loadMap.get(id);if(!l)throw new Error("Load case "+id+" is missing.");return {load:l,factor:f};});
    for(let fi=0;fi<model.frames.length;fi++){
      const fr=model.frames[fi], nodeIds=[fr.baseL,fr.eaveL,fr.ridge,fr.eaveR,fr.baseR], nodeMap=new Map(nodeIds.map((id,i)=>[id,i]));
      const nodes=nodeIds.map(id=>model.nodes.find(n=>n.id===id));
      if(nodes.some(n=>!n))throw new Error("Portal frame references a missing node.");
      const members=model.members.filter(m=>m.frame===fi&&(m.type==="column"||m.type==="rafter"));
      if(members.length!==4)throw new Error("Each portal frame must contain two columns and two rafters.");
      const nd=nodes.length*3,K=zeros(nd,nd),F=Array(nd).fill(0),els=[];
      for(const m of members){
        const a=nodes[nodeMap.get(m.a)],b=nodes[nodeMap.get(m.b)],isCol=m.type==="column";
        const area=Number(isCol?props.columnArea:props.rafterArea),I=Number(isCol?props.columnI:props.rafterI),el=elementMatrices(a,b,E,area,I);
        el.member=m;el.area=area;el.dofs=[0,1,2,3,4,5].map((_,j)=>nodeMap.get(j<3?m.a:m.b)*3+(j%3));els.push(el);
        for(let i=0;i<6;i++)for(let j=0;j<6;j++)K[el.dofs[i]][el.dofs[j]]+=el.kg[i][j];
        const f=Array(6).fill(0);
        activeLoads.forEach(({load,factor})=>{const q=localLoad([load],m.id,el,area,!!load.selfWeight);for(let i=0;i<6;i++)f[i]+=q[i]*factor;});
        const fg=matVec(transpose(el.T),f);for(let i=0;i<6;i++)F[el.dofs[i]]+=fg[i];el.fe=f;
      }
      const fixed=[];[fr.baseL,fr.baseR].forEach(id=>{const n=nodeMap.get(id);fixed.push(n*3,n*3+1);if(model.support==="FIXED")fixed.push(n*3+2);});
      const fixedSet=new Set(fixed),free=Array.from({length:nd},(_,i)=>i).filter(i=>!fixedSet.has(i));
      const Kff=free.map(i=>free.map(j=>K[i][j])),Ff=free.map(i=>F[i]),uf=solveLinear(Kff,Ff),u=Array(nd).fill(0);
      free.forEach((d,i)=>u[d]=uf[i]);
      const Ku=matVec(K,u),R=Ku.map((v,i)=>v-F[i]);
      fixed.forEach(d=>allReactions.push({frame:fi+1,dof:d%3===0?"FX":d%3===1?"FY":"MZ",node:nodeIds[Math.floor(d/3)],value:R[d]}));
      u.forEach((v,i)=>{if(i%3!==2)maxDisp=Math.max(maxDisp,Math.abs(v));});
      for(const el of els){
        const ul=matVec(el.T,el.dofs.map(d=>u[d])),end=matVec(el.k,ul).map((v,i)=>v-el.fe[i]);
        const axial=Math.max(Math.abs(end[0]),Math.abs(end[3])),shear=Math.max(Math.abs(end[1]),Math.abs(end[4])),moment=Math.max(Math.abs(end[2]),Math.abs(end[5]));
        maxAxial=Math.max(maxAxial,axial);maxShear=Math.max(maxShear,shear);maxMoment=Math.max(maxMoment,moment);
        results.push({frame:fi+1,member:el.member.id,type:el.member.type,length:el.L,axialKN:axial,shearKN:shear,momentKNm:moment,endForces:end,localDisplacements:ul});
      }
    }
    return {combinationId:Number(combinationId),combinationName:combo?combo.name:(loadMap.get(Number(combinationId))||{}).name||"Load case",frames:model.frames.length,solvedMembers:results.length,maxTranslationM:maxDisp,maxMomentKNm:maxMoment,maxShearKN:maxShear,maxAxialKN:maxAxial,reactions:allReactions,members:results,scope:"2D in-plane portal frames only; purlins, longitudinal bracing, out-of-plane stability, connection design and full 3D effects are not included."};
  }
  root.GenSolarStructuralSolver={solve};
  if(typeof module!=="undefined"&&module.exports)module.exports=root.GenSolarStructuralSolver;
})(typeof globalThis!=="undefined"?globalThis:this);
