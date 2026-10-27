// Disposable UI-only fixture server. No model, maze solver or real game session is used.
const {chromium, webkit} = require('playwright');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const assets = path.join(root, 'app/src/main/resources/META-INF/resources');
const original = JSON.parse(fs.readFileSync(path.join(root, 'evidence/haunted-08b-off.json'))).trials[0];
const sockets = new Set(); let frame, actions, resets, generation=0;
function fresh() {
  actions=[];resets=0;
  frame={ready:true,generation:'ui-'+(++generation),segment:'fixture',revision:1,seed:1983,
    observation:structuredClone(original.initialObservation),navigation:structuredClone(original.initialNavigation),agentClues:false,
    level:1,status:'PLAYING',message:'UI regression fixture',active:false,pausing:false,reason:'HUMAN_TURN',player:'HUMAN',
    moves:0,actions:0,collisions:0,repeats:0,explored:1,exitsFound:0,decisions:0,latestAction:'START',latencyMs:0,
    model:'tev1:0.8b',models:{TEV1:'tev1:0.8b',TEV1_4B:'tev1:latest'}};
}
const server=http.createServer(async(req,res)=>{
  if(req.url==='/api/game/events') {
    res.writeHead(200,{'Content-Type':'text/event-stream','Cache-Control':'no-cache'});
    const timer=setInterval(()=>res.write('data: '+JSON.stringify(frame)+'\n\n'),150);
    req.on('close',()=>clearInterval(timer));return;
  }
  if(req.url.startsWith('/api/game')) {
    let data='';for await(const chunk of req)data+=chunk;
    const body=data?JSON.parse(data):{};
    if(req.method==='POST'&&req.url==='/api/game/actions') {
      actions.push(body.action);frame.revision++;frame.latestAction=body.action;
      if(body.action==='TURN_RIGHT')frame.observation.direction=(frame.observation.direction+1)%4;
      if(body.action==='TURN_LEFT')frame.observation.direction=(frame.observation.direction+3)%4;
      frame.observation.facing=['N','E','S','W'][frame.observation.direction];
    } else if(req.method==='POST'&&req.url==='/api/game/play') {frame.active=true;frame.player=body.player;}
    else if(req.method==='POST'&&req.url==='/api/game/pause') {frame.active=false;frame.reason='PAUSED';}
    else if(req.method==='POST'&&req.url==='/api/game')resets++;
    res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify(frame));return;
  }
  const pathname=req.url.split('?')[0];
  const file=pathname==='/'?'index.html':pathname.slice(1);
  if(!['index.html','game.js','sound.js','style.css','ghost.svg'].includes(file)){res.writeHead(404);res.end();return;}
  res.writeHead(200,{'Content-Type':file.endsWith('.js')?'application/javascript':file.endsWith('.css')?'text/css':file.endsWith('.svg')?'image/svg+xml':'text/html'});
  res.end(fs.readFileSync(path.join(assets,file)));
});
server.on('connection',s=>{sockets.add(s);s.on('close',()=>sockets.delete(s))});
async function run() {
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const results=[];
  for(const [name,engine] of [['WebKit',webkit],['Chromium',chromium]]) {
    fresh();const browser=await engine.launch({headless:true});
    try {
      const page=await browser.newPage({viewport:{width:1280,height:1000}}), errors=[];
      page.on('pageerror',e=>errors.push(e.message));
      await page.goto('http://127.0.0.1:'+server.address().port);
      await page.getByRole('option',{name:'TEV1-4B · tev1:latest',exact:true}).waitFor({state:'attached'});
      await page.evaluate(()=>{
        window.optionMutations=0;
        new MutationObserver(events=>{window.optionMutations+=events.length}).observe(document.getElementById('player'),{subtree:true,childList:true,characterData:true,attributes:true});
      });
      await page.waitForTimeout(1100);
      assert.equal(await page.evaluate(()=>window.optionMutations),0,'idle frame stream must not rebuild the native picker');
      for(const player of ['TEV1_4B','EXPLORER','RANDOM','TEV1']) {
        await page.locator('#player').selectOption(player);
        assert.equal(await page.locator('#player').inputValue(),player);
        assert.equal(await page.evaluate(()=>document.activeElement.id),'maze','selection returns movement focus');
        const before=actions.length;await page.keyboard.press('ArrowRight');
        await page.waitForFunction(count=>document.querySelector('#decision-action').textContent==='TURN RIGHT',before);
        await page.waitForTimeout(80);assert.equal(actions.length,before+1);assert.equal(actions.at(-1),'TURN_RIGHT');
      }
      await page.locator('#visual-clues').focus();const before=actions.length;await page.keyboard.press('w');await page.waitForTimeout(100);
      assert.equal(actions.length,before+1,'clue-checkbox focus must not steal movement keys');
      await page.locator('#seed').fill('2026');const count=actions.length;await page.locator('#seed').press('ArrowUp');await page.waitForTimeout(80);
      assert.equal(actions.length,count,'seed editing must not move the player');
      await page.locator('#player').selectOption('EXPLORER');await page.locator('#play').click();
      await page.locator('#player:disabled').waitFor();assert(await page.locator('#pause').isEnabled());
      await page.locator('#pause').click();await page.locator('#player:enabled').waitFor();
      await page.locator('#player').selectOption('TEV1_4B');
      const boxes=await page.locator('.clue-controls input').evaluateAll(nodes=>nodes.map(n=>({width:n.getBoundingClientRect().width,height:n.getBoundingClientRect().height,appearance:getComputedStyle(n).appearance})));
      assert(boxes.every(b=>b.width===16&&b.height===16&&b.appearance==='none'),'Safari checkbox appearance is explicitly sized');
      frame.status='WON';await page.waitForTimeout(250);await page.locator('#seed').focus();await page.locator('#seed').press('Enter');await page.waitForTimeout(80);
      assert.equal(resets,0,'Enter while editing a control must not reset the game');
      assert.deepEqual(errors,[]);
      results.push({engine:name,version:browser.version(),result:'PASS',checks:['no picker mutations during frame updates','all four choices commit','movement focus restored','checkbox keys work','seed editing isolated','pause returns control','16px checkbox appearance','editable Enter isolated']});
      console.log(name,'PASS');
    } finally {await browser.close();}
  }
  if(process.env.BROWSER_TEST_OUT)fs.writeFileSync(process.env.BROWSER_TEST_OUT,JSON.stringify({result:'PASS',scope:'Disposable UI-only fixture; not inference evidence',results},null,2)+'\n');
}
run().catch(e=>{console.error(e);process.exitCode=1}).finally(()=>{for(const socket of sockets)socket.destroy();server.close()});
