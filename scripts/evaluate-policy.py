#!/usr/bin/env python3
"""Compare two frozen questions on identical local observations; no gameplay writes."""
import argparse, copy, datetime, hashlib, json, statistics, time, urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]


def expected_action(state):
    front,left,right=(state['relativeEdges'][key] for key in ['front','left','right'])
    if front=='exit':return 'FORWARD'
    if left=='exit':return 'TURN_LEFT'
    if right=='exit':return 'TURN_RIGHT'
    if state['previousAction'] in ['TURN_LEFT','TURN_RIGHT'] and front=='open passage':return 'FORWARD'
    if front=='open passage' and state['current_room_visits']==1:return 'FORWARD'
    if left=='open passage':return 'TURN_LEFT'
    if right=='open passage':return 'TURN_RIGHT'
    if front=='open passage':return 'FORWARD'
    return 'TURN_RIGHT'


def build_request(case, definition, model):
    return {'state':copy.deepcopy(case['state']),'questions':copy.deepcopy(definition['off']['questions']),'model':model}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--out',type=Path,default=ROOT/'evidence/policy-decision-cases.json')
    a=p.parse_args()
    if a.out.exists():raise ValueError('Choose a fresh output path; earlier results are preserved')
    a.out.parent.mkdir(parents=True,exist_ok=True)
    fixture=ROOT/'evidence/policy-fixtures.json'
    cases=json.loads(fixture.read_text())['cases']
    if len({c['id'] for c in cases})!=15 or len(cases)!=15:raise ValueError('Expected 15 unique frozen cases')
    if not all(expected_action(c['state'])==c['expected'] for c in cases):raise ValueError('Fixture expected labels differ from reference policy')
    definitions=[ROOT/'evidence/forward-prompt-definition.json',ROOT/'evidence/policy-prompt-definition.json']
    prompts=[json.loads(f.read_text()) for f in definitions]
    if [d['promptVersion'] for d in prompts]!=['forward-first-v2','ordered-policy-v3']:raise ValueError('Unexpected prompt versions')
    if prompts[0]['off']['questions']['action']['criteria']!=prompts[1]['off']['questions']['action']['criteria']:raise ValueError('Action criteria changed')
    with urllib.request.urlopen('http://localhost:11434/api/tags',timeout=3) as r:
        models=[m for m in json.load(r)['models'] if m['name'] in ['tev1:0.8b','tev1:latest']]
    report={'recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'Policy compliance on synthetic local observations, not maze escapes. No game action is applied. Expected labels remain outside requests.','fixturesSha256':hashlib.sha256(fixture.read_bytes()).hexdigest(),'definitionsSha256':{str(f.relative_to(ROOT)):hashlib.sha256(f.read_bytes()).hexdigest() for f in definitions},'models':models,'timeoutSeconds':10,'retries':0,'trials':[]}
    def save():a.out.write_text(json.dumps(report,indent=2)+'\n')
    save()
    for model in ['tev1:0.8b','tev1:latest']:
        for definition in prompts:
            for case in cases:
                request=build_request(case,definition,model)
                start=time.monotonic()
                try:
                    req=urllib.request.Request('http://localhost:11434/v1/systemone',data=json.dumps(request).encode(),headers={'Content-Type':'application/json'})
                    with urllib.request.urlopen(req,timeout=10) as response:result=json.load(response)
                    choice=result['answers']['action']['choice']
                    if choice not in ['FORWARD','TURN_LEFT','TURN_RIGHT']:raise ValueError('Invalid choice')
                    error=None
                except Exception as e:
                    result={'error':type(e).__name__};choice=None;error=type(e).__name__
                report['trials'].append({'case':case['id'],'model':model,'prompt':definition['promptVersion'],'expected':case['expected'],'choice':choice,'correct':choice==case['expected'],'forwardIntoWall':choice=='FORWARD' and case['state']['relativeEdges']['front']=='wall','missedTurnCommit':case['id'].startswith('commit-after-') and choice!='FORWARD','error':error,'latencyMs':round((time.monotonic()-start)*1000),'request':request,'response':result})
                save()
            ts=[t for t in report['trials'] if t['model']==model and t['prompt']==definition['promptVersion']]
            print(model,definition['promptVersion'],sum(t['correct'] for t in ts),'/',len(ts),'policy matches',flush=True)
    report['summary']=[{'model':model,'prompt':version,'correct':sum(t['correct'] for t in ts),'cases':len(ts),'wallAttempts':sum(t['forwardIntoWall'] for t in ts),'missedTurnCommits':sum(t['missedTurnCommit'] for t in ts),'providerErrors':sum(bool(t['error']) for t in ts),'medianLatencyMs':statistics.median(t['latencyMs'] for t in ts)} for model in ['tev1:0.8b','tev1:latest'] for version in ['forward-first-v2','ordered-policy-v3'] for ts in [[t for t in report['trials'] if t['model']==model and t['prompt']==version]]]
    save()
    if any(t['error'] for t in report['trials']):raise SystemExit('Provider failures retained; comparison incomplete')


if __name__=='__main__':main()
