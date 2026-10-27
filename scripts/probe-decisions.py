#!/usr/bin/env python3
"""Bounded diagnostic requests, separate from gameplay. Never applies an action."""
import argparse, datetime, json, re, time, urllib.request
from pathlib import Path


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--out', default='evidence/decision-diagnostics.json')
    args = p.parse_args()
    repo = Path(__file__).resolve().parents[1]
    source = (repo / 'connectors/src/main/java/dev/ghost/Observations.java').read_text()
    chunk = source.split('DecisionQuestionType.CHOICE,')[1].split('List.of(')[0]
    instructions = ''.join(json.loads(s) for s in re.findall(r'"(?:[^"\\]|\\.)*"', chunk))
    descriptions = {
        'FORWARD': 'Move one room in the current facing direction, or cross an exit ahead.',
        'TURN_LEFT': 'Rotate 90 degrees left; stay in the same room.',
        'TURN_RIGHT': 'Rotate 90 degrees right; stay in the same room.',
    }
    saved_trial = json.loads((repo / 'evidence/evaluation.json').read_text())['trials'][0]
    observation = saved_trial['initialObservation']
    # The second fixture is the real post-collision observation from the saved trial.
    blocked = saved_trial['finalObservation']
    report = {'recordedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'purpose': 'Diagnostic only; no gameplay state is modified. Counterbalance choice order and add redundant relative-edge facts derived only from the same visible observation.',
              'timeoutSeconds': 10, 'retries': 0, 'probes': []}
    for key, endpoint in [('localModels', 'tags'), ('localOllamaVersion', 'version')]:
        with urllib.request.urlopen('http://localhost:11434/api/' + endpoint, timeout=3) as response:
            data = json.load(response)
        report[key] = [m for m in data['models'] if m['name'] in ['tev1:0.8b', 'tev1:latest']] if endpoint == 'tags' else data
    out = Path(args.out)
    for model in ['tev1:0.8b', 'tev1:latest']:
        for name, state in [('start', observation), ('after-collisions', blocked)]:
            for representation in ['compact', 'relative-edges']:
                state = dict(state)
                if representation == 'relative-edges':
                    labels = {'#': 'wall', '.': 'open passage', 'E': 'exit', '?': 'unseen'}
                    state['relativeEdges'] = {key: labels[state['edges'][(state['direction'] + offset) % 4]]
                                              for key, offset in [('front', 0), ('left', 3), ('right', 1)]}
                actions = list(descriptions)
                for offset in range(3):
                    order = actions[offset:] + actions[:offset]
                    request = {'state': state, 'questions': {'action': {'type': 'choice', 'instructions': instructions,
                                'criteria': {a: descriptions[a] for a in order}}}, 'model': model}
                    start = time.monotonic()
                    try:
                        req = urllib.request.Request('http://localhost:11434/v1/systemone', data=json.dumps(request).encode(),
                                                     headers={'Content-Type': 'application/json'})
                        with urllib.request.urlopen(req, timeout=10) as response:
                            result = json.load(response)
                    except Exception as e:
                        result = {'error': type(e).__name__}
                    report['probes'].append({'fixture': name, 'representation': representation, 'order': order,
                                             'request': request, 'response': result,
                                             'latencyMs': round((time.monotonic() - start) * 1000)})
                    out.write_text(json.dumps(report, indent=2) + '\n')
                    print(model, name, representation, order[0], result.get('answers', {}).get('action', {}).get('choice', result), flush=True)


if __name__ == '__main__':
    main()
