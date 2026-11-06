#!/usr/bin/env python3
"""Replay verification accepts released names without weakening lineage checks."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('verify_replay', Path(__file__).with_name('verify-replay.py'))
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class ReplayVerificationTest(unittest.TestCase):
    def document(self, root='PlaySegment'):
        def event(sequence, name, span, kind, time, parent='', origin=''):
            return dict(sequence=sequence, step=name, spanId=span, event=kind,
                        startTime=time, endTime=time, parentSpanId=parent,
                        traceId='one-root', **({'from': origin} if origin else {}))
        return dict(status='completed', topology={'steps': [dict(
            step=root, runtimeStepClass='dev.ghost.pipeline.PipelineInvocation_root',
            actorKind='pipeline:play')]}, events=[
                event(1, root, 'root', 'start', 0),
                event(2, 'PipelineInvocation_child', 'child', 'start', 1, 'root', root),
                event(3, 'PipelineInvocation_child', 'child', 'success', 2, 'root', root),
                event(4, root, 'root', 'success', 3)])

    def inspect(self, document):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'replay.json'
            path.write_text(json.dumps(document))
            return verifier.inspect(path)

    def test_released_authored_name_and_historical_generated_name(self):
        for name in ['PlaySegment', 'PipelineInvocation_legacy']:
            self.assertEqual(1, self.inspect(self.document(name))['nestedInvocations'])

    def test_child_must_finish_before_root(self):
        document = self.document()
        document['events'][2]['endTime'] = 4
        with self.assertRaisesRegex(AssertionError, 'outlived root'):
            self.inspect(document)

    def test_child_requires_parent_identity(self):
        document = self.document()
        document['events'][1]['parentSpanId'] = ''
        with self.assertRaisesRegex(AssertionError, 'no parent identity'):
            self.inspect(document)

    def test_child_cannot_change_root_trace(self):
        document = self.document()
        document['events'][1]['traceId'] = 'another-root'
        with self.assertRaisesRegex(AssertionError, 'multiple root traces'):
            self.inspect(document)


if __name__ == '__main__':
    unittest.main()
