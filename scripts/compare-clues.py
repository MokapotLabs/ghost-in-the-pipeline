#!/usr/bin/env python3
"""Compare live clue-disabled/enabled trials; never runs a solver or changes a maze."""
import argparse, json, statistics, re
from pathlib import Path


def require(condition, message):
    if not condition:
        raise ValueError(message)


def compare(off, on):
    require(off['protocol'] == on['protocol'] and off['protocol'] in ['ghost-haunted-v1','ghost-action-prompt-v1'], 'Wrong experiment protocol')
    if off['protocol']=='ghost-action-prompt-v1':
        require(off.get('decisionPrompt')==on.get('decisionPrompt')=='action-focused-v1','Wrong decision prompt')
    require(off['agentClues'] is False and on['agentClues'] is True, 'Expected off/on arms')
    require(len(off['players'])==1 and off['players'][0] in ['TEV1','TEV1_4B'], 'Compare one genuine model policy at a time')
    expected_hashes={
        'sourceSha256':{'connectors/src/main/java/dev/ghost/'+name+'.java' for name in ['Rules','Observations','Baselines','Signals']},
        'runtimeClassesSha256':{'dev/ghost/'+name+'.class' for name in ['Rules','Observations','Baselines','DecodeChoice','Runs','Session','Signals','NavigationSignal','SignalReading','ActionLog','RunState','ChooseTurn']}}
    for report in [off,on]:
        for field,keys in expected_hashes.items():
            values=report.get(field,{})
            require(set(values)==keys and all(isinstance(v,str) and re.fullmatch('[0-9a-f]{64}',v) for v in values.values()), 'Incomplete or invalid build identity: '+field)
    for key in ['seeds','levels','players','maxDecisions','durationMs','localModels','localOllamaVersion','sourceSha256','runtimeClassesSha256','presentationDelayMs','cachePolicy']:
        require(key in off and key in on, 'Missing control: ' + key)
        require(off[key] == on[key], 'Control differs: ' + key)
    require(off['maxDecisions']==128 and off['durationMs']==120000 and off['presentationDelayMs']==0, 'Budgets or presentation pacing changed')
    require(bool(off.get('localModels')) and bool(off.get('runtimeClassesSha256')), 'Missing model/build identity')
    cases={(s,l,p) for s in off['seeds'] for l in off['levels'] for p in off['players']}
    summaries=[]
    for enabled, report in [(False, off),(True,on)]:
        require(len(report['trials'])==len(cases), 'Missing or duplicate trials')
        keyed={(t['seed'],t['level'],t['player']):t for t in report['trials']}
        require(set(keyed)==cases,'Different cases')
        require(all(t['agentClues'] is enabled and not t['executionFailure'] for t in report['trials']), 'Wrong clue mode or provider failure')
        require(all(t['termination'] in ['WON','LOST','DECISION_LIMIT','TIME_LIMIT'] for t in report['trials']), 'Interrupted attempt')
        require(all(0<t['decisions']<=128 and len(t['actions'])==t['decisions'] for t in report['trials']), 'Missing actual decisions or excessive budget')
        if not enabled: first=keyed
        else:
            for key in cases:
                require(first[key]['initialObservation']==keyed[key]['initialObservation'],'Starting observation differs')
                require(first[key]['initialNavigation']==keyed[key]['initialNavigation'],'Starting signal differs')
                require(first[key]['model']==keyed[key]['model'],'Model changed')
        lat=[a['latencyMs'] for t in report['trials'] for a in t['actions']]
        summaries.append({'agentClues':enabled,'trials':len(cases),'escapes':sum(t['exitFound'] for t in report['trials']),
                          **{key:sum(t[key] for t in report['trials']) for key in ['decisions','moves','collisions','repeatedPositions','roomsExplored']},
                          'roomExhaustions':sum(t['status']=='LOST' for t in report['trials']),
                          'terminationReasons':{reason:sum(t['termination']==reason for t in report['trials']) for reason in sorted({t['termination'] for t in report['trials']})},
                          'latencyMs':{'median':statistics.median(lat),'max':max(lat)}})
    return {'result':'PASS','controls':'Identical seeds, initial observations/readings, model identities, compiled game classes and budgets; clue availability is the experimental switch.',
            'players':off['players'],'levels':off['levels'],'seeds':off['seeds'],'models':off['localModels'],'arms':summaries,
            'limits':'One attempt per case/arm. Timings are local and warm; deadline cancellation can omit one pending inference from committed action totals.'}


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('off',type=Path);p.add_argument('on',type=Path);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    result=compare(json.loads(a.off.read_text()),json.loads(a.on.read_text()))
    result['inputs']=[str(a.off),str(a.on)];a.out.write_text(json.dumps(result,indent=2)+'\n')
    for arm in result['arms']:print('clues',arm['agentClues'],arm['escapes'],'/',arm['trials'],'escapes;',arm['moves'],'moves;',arm['collisions'],'wall collisions')

if __name__=='__main__':main()
