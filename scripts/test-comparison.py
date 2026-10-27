#!/usr/bin/env python3
"""Check that saved-result pairing fails closed, including under python -O."""
import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

REPO = Path(__file__).resolve().parents[1]


class ComparisonValidation(unittest.TestCase):
    small_report = 'comparison-08b-compact.json'
    large_report = 'comparison-4b-compact.json'

    def run_pair(self, mutate=None, mutate_small=None):
        small = json.loads((REPO / 'evidence' / self.small_report).read_text())
        large = json.loads((REPO / 'evidence' / self.large_report).read_text())
        if mutate:
            mutate(large)
        if mutate_small:
            mutate_small(small)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name, value in [('small', small), ('large', large)]:
                (root / (name + '.json')).write_text(json.dumps(value))
            result = subprocess.run([sys.executable, '-O', str(REPO / 'scripts/compare-models.py'),
                                     str(root / 'small.json'), str(root / 'large.json'), '--out', str(root / 'out.json')],
                                    capture_output=True, text=True)
            return result.returncode, (root / 'out.json').exists()

    def test_actual_saved_pair_passes(self):
        self.assertEqual((0, True), self.run_pair())

    def assert_rejected(self, mutate, mutate_small=None):
        code, wrote_summary = self.run_pair(mutate, mutate_small)
        self.assertNotEqual(0, code)
        self.assertFalse(wrote_summary)

    def test_different_observation_rejected(self):
        self.assert_rejected(lambda r: r['trials'][0]['initialObservation'].update(facing='S'))

    def test_different_source_rejected(self):
        self.assert_rejected(lambda r: r['sourceSha256'].update(changed='yes'))

    def test_different_runtime_rejected(self):
        self.assert_rejected(lambda r: r['runtimeClassesSha256'].update(changed='yes'))

    def test_missing_source_identity_rejected(self):
        self.assert_rejected(lambda r: r['sourceSha256'].clear())

    def test_matching_empty_source_maps_still_rejected(self):
        clear = lambda r: r['sourceSha256'].clear()
        self.assert_rejected(clear, clear)

    def test_missing_runtime_class_rejected(self):
        self.assert_rejected(lambda r: r['runtimeClassesSha256'].pop('dev/ghost/Observations.class'))

    def test_matching_incomplete_runtime_maps_still_rejected(self):
        remove = lambda r: r['runtimeClassesSha256'].pop('dev/ghost/Observations.class')
        self.assert_rejected(remove, remove)

    def test_malformed_digest_rejected(self):
        malformed = lambda r: r['runtimeClassesSha256'].update({'dev/ghost/Observations.class': 'not-a-digest'})
        self.assert_rejected(malformed, malformed)

    def test_different_budget_rejected(self):
        self.assert_rejected(lambda r: r.update(maxDecisions=129))

    def test_duplicate_case_rejected(self):
        self.assert_rejected(lambda r: r['trials'].__setitem__(1, copy.deepcopy(r['trials'][0])))

    def test_execution_failure_rejected(self):
        self.assert_rejected(lambda r: r['trials'][0].update(executionFailure=True))

    def test_no_inference_actions_rejected(self):
        self.assert_rejected(lambda r: r['trials'][0].update(decisions=0, actions=[]))

    def test_interrupted_trial_rejected(self):
        self.assert_rejected(lambda r: r['trials'][0].update(termination='PAUSED'))

    def test_wrong_model_player_role_rejected(self):
        self.assert_rejected(lambda r: r['trials'][0].update(player='TEV1_4B' if r['protocol'] == 'ghost-paired-v1' else 'TEV1'))


class RelativeComparisonValidation(ComparisonValidation):
    small_report = 'relative-08b.json'
    large_report = 'relative-4b.json'

    def test_mixed_observation_protocols_rejected(self):
        self.assert_rejected(lambda r: r.update(protocol='ghost-paired-v1'))

    def test_relative_labels_required(self):
        remove = lambda r: r['trials'][0]['initialObservation'].pop('relativeEdges')
        self.assert_rejected(remove, remove)


if __name__ == '__main__':
    unittest.main()
