import importlib.util, json, unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('compare',ROOT/'scripts/compare-prompts.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)


class PromptControls(unittest.TestCase):
    def setUp(self):
        self.reports=[json.loads((ROOT/'evidence'/f'{name}.json').read_text()) for name in ['haunted-08b-off','haunted-08b-on','action-08b-off','action-08b-on']]

    def test_actual_old_new_trials(self):
        self.assertEqual('PASS',module.compare(*self.reports)['result'])

    def test_actual_forward_first_reruns(self):
        reports=[json.loads((ROOT/'evidence'/f'{name}.json').read_text()) for name in ['action-08b-off','action-08b-on','forward-08b-off','forward-08b-on']]
        self.assertEqual('PASS',module.compare(*reports)['result'])
        before=json.loads((ROOT/'evidence/action-prompt-definition.json').read_text())
        after=json.loads((ROOT/'evidence/forward-prompt-definition.json').read_text())
        for arm in ['off','on']:
            self.assertEqual(before[arm]['state'],after[arm]['state'])
            self.assertEqual(before[arm]['questions']['action']['criteria'],after[arm]['questions']['action']['criteria'])
            self.assertIn('If front is open passage, choose FORWARD.',after[arm]['questions']['action']['instructions'])

    def test_wrong_prompt_rejected(self):
        self.reports[2]['decisionPrompt']='other'
        with self.assertRaises(ValueError):module.compare(*self.reports)

    def test_observation_logic_is_not_an_allowed_prompt_change(self):
        source=(ROOT/'connectors/src/main/java/dev/ghost/Observations.java').read_text()
        changed=source.replace('out.put("room", g.room())','out.put("room", g.room()+1)')
        self.assertNotEqual(module.non_question_source(source),module.non_question_source(changed))

    def test_literal_whitespace_is_not_discarded(self):
        source=(ROOT/'connectors/src/main/java/dev/ghost/Observations.java').read_text()
        changed=source.replace('out.put("room",','out.put("ro om",')
        self.assertNotEqual(module.non_question_source(source),module.non_question_source(changed))

    def test_changed_rules_rejected_even_when_both_new_arms_match(self):
        for r in self.reports[2:]:r['runtimeClassesSha256']['dev/ghost/Rules.class']='0'*64
        with self.assertRaises(ValueError):module.compare(*self.reports)

    def test_changed_starts_rejected_even_when_both_new_arms_match(self):
        for r in self.reports[2:]:r['trials'][0]['initialObservation']['room']=8
        with self.assertRaises(ValueError):module.compare(*self.reports)

    def test_new_clue_controls_remain_strict(self):
        self.reports[3]['agentClues']=False
        with self.assertRaises(ValueError):module.compare(*self.reports)

    def test_evaluator_rejects_old_or_unknown_serving_prompt(self):
        spec=importlib.util.spec_from_file_location('evaluate',ROOT/'scripts/evaluate.py')
        evaluator=importlib.util.module_from_spec(spec);spec.loader.exec_module(evaluator)
        for frame in [{}, {'decisionPrompt':'previous'}, {'decisionPrompt':'action-focused-v1'}]:
            with self.assertRaises(ValueError):evaluator.verify_prompt(frame)
        self.assertEqual('forward-first-v2',evaluator.verify_prompt({'decisionPrompt':'forward-first-v2'}))

    def test_actual_exported_request_states_are_unchanged(self):
        before=json.loads((ROOT/'evidence/action-prompt-previous.json').read_text())
        after=json.loads((ROOT/'evidence/action-prompt-definition.json').read_text())
        for arm in ['off','on']:
            self.assertEqual(before[arm]['state'],after[arm]['state'])
            self.assertNotEqual(before[arm]['questions'],after[arm]['questions'])


if __name__=='__main__':unittest.main()
