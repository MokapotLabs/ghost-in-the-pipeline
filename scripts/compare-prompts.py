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
    return re.sub(r'\s+','',source)


def compare(old_off, old_on, new_off, new_on):
    old = clues.compare(old_off, old_on)
    new = clues.compare(new_off, new_on)
    clues.require(old_off['protocol'] == 'ghost-haunted-v1' and new_off['protocol'] == 'ghost-action-prompt-v1', 'Expected historical and action-focused protocols')
    path='connectors/src/main/java/dev/ghost/Observations.java'
    previous=subprocess.check_output(['git','show','1f263f8:'+path],cwd=ROOT).decode()
    current=(ROOT/path).read_text()
    clues.require(non_question_source(previous)==non_question_source(current),'Observation logic changed outside the question/constants')
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
                clues.require(a[key][field] == b[key][field], 'Prompt starting control changed: '+field)
    return {'result':'PASS','controls':'Same seeds, models, initial observations/readings, rules, baselines, signals and budgets. Only observation-authoring class changed among recorded game classes; fixture export shows state unchanged and question text changed.', 'previous':old['arms'],'actionFocused':new['arms']}


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('inputs',nargs=4,type=Path,metavar='JSON')
    p.add_argument('--out',required=True,type=Path)
    args=p.parse_args()
    result=compare(*(json.loads(f.read_text()) for f in args.inputs))
    result['inputsSha256']={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in args.inputs}
    args.out.write_text(json.dumps(result,indent=2)+'\n')
    print('PASS: historical/new prompt controls')
