#!/usr/bin/env python3
"""Check frozen old/new prompt trials without changing actions or gameplay data."""
import argparse, hashlib, importlib.util, json, re, subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('clues', ROOT/'scripts/compare-clues.py')
clues = importlib.util.module_from_spec(spec)
spec.loader.exec_module(clues)


def non_question_source(source):
    # Bounded comparison of these two authored revisions, not a Java parser.
    source=re.sub(r'public static final String PROMPT_VERSION\s*=\s*"[^"\n]*";', '', source)
    for name in ['ACTION_PROMPT','CLUE_PROMPT']:
        source=re.sub(r'public static final String '+name+r'\s*=\s*""".*?"""\s*\.strip\(\);', '', source, flags=re.S)
    start=source.index('DecisionQuestionType.CHOICE,')+len('DecisionQuestionType.CHOICE,')
    end=source.index('List.of(',start)
    source=source[:start]+'<question instructions>,'+source[end:]
    for label in ['FORWARD','TURN_LEFT','TURN_RIGHT']:
        source,count=re.subn(r'new DecisionCriterion\(\s*"'+label+r'",\s*"(?:[^"\\]|\\.)*"\)', '<criterion '+label+'>', source)
        clues.require(count==1,'Expected one unchanged action label: '+label)
    # Keep every byte inside Java string/character literals, including spaces.
    return re.sub(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|\s+',
                  lambda m: '' if m.group().isspace() else m.group(), source)


def compare(old_off, old_on, new_off, new_on):
    old = clues.compare(old_off, old_on)
    new = clues.compare(new_off, new_on)
    pair=(old_off['protocol'],new_off['protocol'])
    references={('ghost-haunted-v1','ghost-action-prompt-v1'):('1f263f8','1a59ac4'),('ghost-action-prompt-v1','ghost-forward-prompt-v2'):('1a59ac4','4bd190a'),('ghost-forward-prompt-v2','ghost-ordered-policy-v3'):('4bd190a',None)}
    clues.require(pair in references,'Expected adjacent frozen prompt protocols')
    policy=pair[1]=='ghost-ordered-policy-v3'
    path='connectors/src/main/java/dev/ghost/Observations.java'
    previous_ref,current_ref=references[pair]
    previous=subprocess.check_output(['git','show',previous_ref+':'+path],cwd=ROOT).decode()
    current=subprocess.check_output(['git','show',current_ref+':'+path],cwd=ROOT).decode() if current_ref else (ROOT/path).read_text()
    before_projection,after_projection=non_question_source(previous),non_question_source(current)
    if policy:
        alias='out.put("current_room_visits",current.visits());'
        clues.require(after_projection.count(alias)==1,'Expected exactly the observed-memory visit alias')
        after_projection=after_projection.replace(alias,'',1)
    clues.require(before_projection==after_projection,'Observation logic changed outside the question and allowed visit alias')
    for before, after in [(old_off, new_off), (old_on, new_on)]:
        for report,source in [(before,previous),(after,current)]:
            clues.require(report['sourceSha256'][path]==hashlib.sha256(source.encode()).hexdigest(),'Observation source does not match recorded revision')
        for key in ['seeds','levels','players','maxDecisions','durationMs','localModels','localOllamaVersion','presentationDelayMs','cachePolicy']:
            clues.require(before[key] == after[key], 'Prompt experiment control differs: '+key)
        for field, changed in [('sourceSha256','connectors/src/main/java/dev/ghost/Observations.java'), ('runtimeClassesSha256','dev/ghost/Observations.class')]:
            clues.require({k:v for k,v in before[field].items() if k != changed} == {k:v for k,v in after[field].items() if k != changed}, 'Non-prompt gameplay identity changed')
        keyed = lambda r: {(t['seed'],t['level'],t['player']): t for t in r['trials']}
        a, b = keyed(before), keyed(after)
        clues.require(a.keys() == b.keys(), 'Different prompt cases')
        for key in a:
            for field in ['initialObservation','initialNavigation','model']:
                actual=b[key][field]
                if policy and field=='initialObservation':
                    actual=dict(actual)
                    count=actual.pop('current_room_visits',None)
                    row=next(r for r in actual['memory'] if r[0]==actual['room'])
                    clues.require(count==row[1],'Visit alias does not match observed memory')
                clues.require(a[key][field] == actual, 'Prompt starting control changed: '+field)
    return {'result':'PASS','controls':'Same seeds, models, existing starting facts/readings, rules, baselines, signals and budgets. Observation source differs only in question and, for ordered policy, the checked current_room_visits alias of observed memory.', 'previous':old['arms'],'actionFocused':new['arms']}


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('inputs',nargs=4,type=Path,metavar='JSON')
    p.add_argument('--out',required=True,type=Path)
    args=p.parse_args()
    result=compare(*(json.loads(f.read_text()) for f in args.inputs))
    result['inputsSha256']={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in args.inputs}
    args.out.write_text(json.dumps(result,indent=2)+'\n')
    print('PASS: historical/new prompt controls')
