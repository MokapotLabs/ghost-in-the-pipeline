#!/usr/bin/env python3
"""Bounded old/new question probes on identical visible fixtures; no game actions are applied."""
import copy,json,time,urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
old=json.loads((ROOT/'evidence/action-prompt-previous.json').read_text())
new=json.loads((ROOT/'evidence/action-prompt-definition.json').read_text())
out={'purpose':'Provider-only diagnosis; fixtures are actual typed requests exported from old/current application classes. No gameplay actions, retries or routes.', 'timeoutSeconds':10,'probes':[]}
for model in ['tev1:0.8b','tev1:latest']:
    for arm in ['off','on']:
        if old[arm]['state']!=new[arm]['state']:raise ValueError('Diagnostic state changed')
        for version,definition in [('previous',old),('action-focused-v1',new)]:
            request=copy.deepcopy(definition[arm]);request['model']=model
            start=time.monotonic()
            try:
                req=urllib.request.Request('http://localhost:11434/v1/systemone',data=json.dumps(request).encode(),headers={'Content-Type':'application/json'})
                with urllib.request.urlopen(req,timeout=10) as response:result=json.load(response)
            except Exception as e:result={'error':type(e).__name__}
            out['probes'].append({'model':model,'agentClues':arm=='on','prompt':version,'request':request,'response':result,'latencyMs':round((time.monotonic()-start)*1000)})
            (ROOT/'evidence/action-prompt-diagnostics.json').write_text(json.dumps(out,indent=2)+'\n')
            print(model,arm,version,result.get('answers',{}).get('action',{}).get('choice',result),flush=True)
