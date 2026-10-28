// Ejecuta la colección con Newman y produce evidencia sin tokens ni contraseñas.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const newman = require('newman');
const root = path.resolve(__dirname, '..');
const variables = Object.fromEntries(fs.readFileSync(path.join(root, '.env'), 'utf8').replace(/^\uFEFF/, '').trim().split(/\r?\n/).filter(l => l.includes('=')).map(l => [l.slice(0,l.indexOf('=')),l.slice(l.indexOf('=')+1)]));
const header = Buffer.from(JSON.stringify({alg:'HS256',typ:'JWT'})).toString('base64url');
const now = Math.floor(Date.now()/1000);
const payload = Buffer.from(JSON.stringify({iss:'EcoAPI',sub:'1',iat:now-4000,exp:now-1,privilegios:[]})).toString('base64url');
const firma = crypto.createHmac('sha256', variables.JWT_SECRET).update(header+'.'+payload).digest('base64url');
const result = [];
const reporte = path.join(root, 'evidencias', 'Postman_Newman_resultados.txt');
fs.mkdirSync(path.dirname(reporte), {recursive:true});
newman.run({
  collection: require(path.join(root,'postman','EcoAPI_JorgeQuispe_Unidad2.postman_collection.json')),
  environment: {values:[{key:'baseUrl',value:process.env.ECO_BASE_URL || 'http://localhost:8080'},{key:'claveExamen',value:variables.SEED_PASSWORD},{key:'tokenVencido',value:header+'.'+payload+'.'+firma}]},
  reporters: [],
}, (err, summary) => {
  if (err) { console.error(err.message); process.exitCode=1; return; }
  const stats=summary.run.stats;
  const text=['EcoAPI — Jorge Quispe — Ejecución real con Newman',new Date().toISOString(),'Comando: node scripts/probar-postman.cjs',...result,`Solicitudes: ${stats.requests.total}; fallos: ${stats.requests.failed}`,`Aserciones: ${stats.assertions.total}; fallos: ${stats.assertions.failed}`];
  for(const fail of summary.run.failures) text.push('FALLO: '+fail.source?.name+' — '+fail.error?.message);
  fs.writeFileSync(reporte,text.join('\n')+'\n');console.log(text.join('\n'));
  const exportada=summary.collection.toJSON();
  exportada.variable=(exportada.variable || []).map(v => ({...v,value:/token|clave|password/i.test(v.key)?'':v.value}));
  fs.writeFileSync(path.join(root,'postman','EcoAPI_JorgeQuispe_Unidad2.postman_collection.json'),JSON.stringify(exportada,null,2)+'\n');
  if(summary.run.failures.length) process.exitCode=1;
}).on('request', (error,args) => {
  result.push(`${args.item.name}: ${error ? error.message : args.response.code}`);
});
