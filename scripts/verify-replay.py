#!/usr/bin/env python3
"""Verify native replay identity, nested invocation lifetime and event ordering."""
import argparse,json
from pathlib import Path

def inspect(path):
 j=json.loads(path.read_text());events=j['events'];assert events,f'{path}: no native events'
 traces={e['traceId'] for e in events if e.get('traceId')};assert len(traces)==1,f'{path}: multiple root traces'
 seq=[e['sequence'] for e in events];assert seq==sorted(set(seq)),f'{path}: sequence not strictly ordered'
 assert all(e['endTime']>=e['startTime'] for e in events),f'{path}: inverted event interval'
 starts={e['spanId']:e for e in events if e['event']=='start' and e.get('spanId')}
 ends={e['spanId']:e for e in events if e['event'] in ('success','error','failure') and e.get('spanId')}
 roots=[e for e in starts.values() if e['step'].startswith('PipelineInvocation_') and not e.get('from')]
 # Native exports include nested composition spans. Root entry is the first invocation span.
 assert roots,f'{path}: no native root invocation'
 root=min(roots,key=lambda e:e['sequence']);assert root['spanId'] in ends,f'{path}: root has no terminal event'
 terminal=ends[root['spanId']]
 nested=[e for e in starts.values() if e['step'].startswith('PipelineInvocation_') and e is not root]
 for event in nested:
  assert event.get('parentSpanId'),f'{path}: nested invocation has no parent identity'
  assert event['traceId']==root['traceId'],f'{path}: recursive turn changed root trace'
  assert event['spanId'] in ends,f'{path}: nested invocation has no terminal event'
  assert root['startTime']<=event['startTime']<=ends[event['spanId']]['endTime']<=terminal['endTime'],f'{path}: nested invocation outlived root'
 model=sum(e['event']=='success' and e['step'] in ('ProcessAskTev1','ProcessAskTev14b','ProcessAskJev') for e in events)
 return {'file':path.name,'traceId':root['traceId'],'status':j['status'],'events':len(events),'nestedInvocations':len(nested),'modelQueries':model,'rootLineage':'PASS'}
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('directory',type=Path);p.add_argument('--out',type=Path);a=p.parse_args();files=sorted(a.directory.glob('*.json'));assert files,'No native replay documents found';results=[inspect(f) for f in files];summary={'result':'PASS','documentsDiagnostic':len(results),'distinctRootTracesDiagnostic':len({r['traceId'] for r in results}),'runs':results};s=json.dumps(summary,indent=2)+'\n';print(s)
 if a.out:a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(s)
if __name__=='__main__':main()
