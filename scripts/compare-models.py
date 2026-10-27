#!/usr/bin/env python3
"""Verify paired gameplay inputs and summarize two actual model evaluations."""
import argparse
import datetime
import json
import re
import statistics
from pathlib import Path


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate_hashes(report):
    expected = {
        'sourceSha256': {'connectors/src/main/java/dev/ghost/' + name + '.java' for name in ['Rules', 'Observations', 'Baselines']},
        'runtimeClassesSha256': {'dev/ghost/' + name + '.class' for name in ['Rules', 'Observations', 'Baselines', 'DecodeChoice', 'Runs', 'Session']},
    }
    for field, names in expected.items():
        values = report.get(field, {})
        require(isinstance(values, dict) and set(values) == names, 'Incomplete identity map: ' + field)
        require(all(isinstance(digest, str) and re.fullmatch(r'[0-9a-f]{64}', digest) for digest in values.values()), 'Invalid SHA-256: ' + field)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('small', type=Path)
    parser.add_argument('large', type=Path)
    parser.add_argument('--out', type=Path, default=Path('evidence/model-comparison.json'))
    args = parser.parse_args()
    reports = [json.loads(p.read_text()) for p in [args.small, args.large]]
    expected = {(seed, level) for seed in [1983, 1986, 2026] for level in [1, 2, 3]}
    paired = []
    for report, model in zip(reports, ['tev1:0.8b', 'tev1:latest']):
        validate_hashes(report)
        require(report['protocol'] == 'ghost-paired-v1', 'Different protocol')
        require(report['maxDecisions'] == 128 and report['durationMs'] == 120000, 'Different budgets')
        trials = [t for t in report['trials'] if t['player'] == 'TEV1']
        require(len(trials) == 9 and {(t['seed'], t['level']) for t in trials} == expected, 'Missing, duplicate or different cases')
        require(all(t['model'] == model and not t['executionFailure'] for t in trials), 'Wrong model or execution failure')
        require(all(t['decisions'] > 0 and len(t['actions']) == t['decisions'] for t in trials), 'Each live trial must contain its committed decisions and action records')
        require(all(t['termination'] in ['WON', 'LOST', 'DECISION_LIMIT', 'TIME_LIMIT'] for t in trials), 'Evaluation was interrupted or has an unknown termination')
        require(len(report['localModels']) == 1 and report['localModels'][0]['name'] == model, 'Missing model identity')
        paired.append({(t['seed'], t['level']): t for t in trials})
    require(reports[0]['sourceSha256'] == reports[1]['sourceSha256'], 'Rules, observations or baseline policy changed')
    require(bool(reports[0].get('runtimeClassesSha256')) and reports[0]['runtimeClassesSha256'] == reports[1].get('runtimeClassesSha256'), 'Missing or different serving runtime class identities')
    require(reports[0]['presentationDelayMs'] == reports[1]['presentationDelayMs'] == 0, 'Presentation delay was not disabled')
    for case in expected:
        require(paired[0][case]['initialObservation'] == paired[1][case]['initialObservation'], f'Start differs: {case}')
    summary = {'recordedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
               'protocol': 'ghost-paired-v1', 'pairedInputs': 'PASS: nine identical starting observations; identical rules/observation/policy source hashes and budgets',
               'sourceSha256': reports[0]['sourceSha256'], 'runtimeClassesSha256': reports[0]['runtimeClassesSha256'], 'inputs': [str(args.small), str(args.large)],
               'limits': 'One trial per model/case; model size and quantization differ. Warm local timings are descriptive, not an isolated speed benchmark.',
               'models': [], 'cases': []}
    for report, trials in zip(reports, paired):
        ts = list(trials.values())
        latency = [a['latencyMs'] for t in ts for a in t['actions']]
        model = {'identity': report['localModels'][0], 'exitSuccesses': sum(t['exitFound'] for t in ts), 'trials': len(ts)}
        for key in ['decisions', 'moves', 'collisions', 'rejectedActions', 'repeatedPositions', 'roomsExplored']:
            model[key] = sum(t[key] for t in ts)
        model['decisionLatencyMs'] = {'median': statistics.median(latency), 'max': max(latency)}
        model['terminationReasons'] = {reason: sum(t['termination'] == reason for t in ts) for reason in sorted({t['termination'] for t in ts})}
        model['actionCounts'] = {action: sum(a['action'] == action for t in ts for a in t['actions']) for action in ['FORWARD', 'TURN_LEFT', 'TURN_RIGHT']}
        summary['models'].append(model)
    for seed, level in sorted(expected):
        summary['cases'].append({'seed': seed, 'level': level, 'models': [
            {k: trials[(seed, level)][k] for k in ['model', 'termination', 'exitFound', 'decisions', 'moves', 'collisions', 'repeatedPositions', 'roomsExplored', 'decisionLatencyMs']}
            for trials in paired]})
    args.out.write_text(json.dumps(summary, indent=2) + '\n')
    for model in summary['models']:
        print(model['identity']['name'], f"{model['exitSuccesses']}/9 exits", model['moves'], 'moves', model['collisions'], 'collisions')


if __name__ == '__main__':
    main()
