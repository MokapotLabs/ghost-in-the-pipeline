#!/usr/bin/env python3
"""Paired live trials through the application's actual TPF entrypoints. No solver."""
import argparse,json,time,urllib.request,statistics,datetime,os
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--url',default='http://127.0.0.1:8087');p.add_argument('--out',default='evidence/evaluation.json');args=p.parse_args()
 def call(path='',body=None):
  req=urllib.request.Request(args.url+'/api/game'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'})
  with urllib.request.urlopen(req,timeout=20) as r:return json.load(r)
 out=Path(args.out);out.parent.mkdir(parents=True,exist_ok=True)
 report={'protocol':'ghost-paired-v1','recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'seeds':[1983,1986,2026],'levels':[1,2,3],'players':['TEV1','RANDOM','EXPLORER'],'maxDecisions':128,'durationMs':120000,'information':'local visible edges plus persistent observed-room memory; no full map or witness','trials':[]}
 try:
  configured_model=call().get('model')
  with urllib.request.urlopen('http://localhost:11434/api/tags',timeout=3) as r:report['localModels']=[m for m in json.load(r)['models'] if m['name']==configured_model]
 except OSError:report['localModels']=[]
 try:
  with urllib.request.urlopen('http://localhost:11434/api/version',timeout=3) as r:report['localOllamaVersion']=json.load(r)
 except OSError:report['localOllamaVersion']=None
 report['presentationDelayMs']=int(os.getenv('GHOST_PACE_MS','160'))
 report['cachePolicy']='bypass-cache'
 report['latencyDefinition']='Wall time from control observation to validated model response; excludes presentation delay and includes connector/projection overhead.'
 out.write_text(json.dumps(report,indent=2)+'\n')
 for seed in report['seeds']:
  for level in report['levels']:
   for player in report['players']:
    f=call(body={'seed':seed,'level':level,'singleLevel':True});initial=f['observation'];generation=f['generation'];start=time.monotonic()
    segment=call('/play',{'player':player,'revision':f['revision'],'maxDecisions':128,'durationMs':120000})['segment']
    while True:
     f=call()
     if f['generation']!=generation or f['segment']!=segment:raise RuntimeError('Evaluation interrupted by another session action')
     if not f['active']:break
     if time.monotonic()-start>130:raise RuntimeError('Application exceeded its outer time bound')
     time.sleep(.1)
    history=[e for e in call('/history') if e['action']!='START'];lat=[e['latencyMs'] for e in history] if player=='TEV1' else []
    trial={'seed':seed,'level':level,'size':[3,4,6][level-1],'player':player,'model':f['model'] if player=='TEV1' else None,'initialObservation':initial,'exitFound':f['exitsFound']>0,'status':f['status'],'termination':f['reason'],'decisions':f['decisions'],'moves':f['moves'],'collisions':f['collisions'],'repeatedPositions':f['repeats'],'roomsExplored':f['explored'],'rejectedActions':f['collisions']+sum(e['action'] not in ('FORWARD','TURN_LEFT','TURN_RIGHT') for e in history),'executionFailure':f['reason'].startswith('PROVIDER_'),'elapsedSeconds':round(time.monotonic()-start,3),'decisionLatencyMs':{'median':statistics.median(lat) if lat else None,'max':max(lat) if lat else None},'actions':history,'finalObservation':f['observation']}
    report['trials'].append(trial);out.write_text(json.dumps(report,indent=2)+'\n')
    print(f'{seed}/{level} {player:8} {f["reason"]:20} {f["decisions"]:3} decisions {f["moves"]:3} moves {f["explored"]:2} rooms',flush=True)
 for player in report['players']:
  ts=[t for t in report['trials'] if t['player']==player];print(player, sum(t['exitFound'] for t in ts),'/',len(ts),'escaped',flush=True)
 if any(t['executionFailure'] for t in report['trials']):raise SystemExit('Provider/pipeline failures recorded; this is not a complete live-model comparison.')
 # A fresh ordinary campaign leaves the same application ready for a manual recording.
 call(body={'seed':1983,'level':1,'singleLevel':False})
if __name__=='__main__':main()
