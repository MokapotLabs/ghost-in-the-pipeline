import copy, importlib.util, json, unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,ROOT/path)
    value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

policy=module('policy','scripts/evaluate-policy.py')
comparison=module('comparison','scripts/compare-prompts.py')


class PolicyChecks(unittest.TestCase):
    def test_frozen_reference_labels_and_coverage(self):
        cases=json.loads((ROOT/'evidence/policy-fixtures.json').read_text())['cases']
        self.assertEqual(15,len(cases))
        for case in cases:
            self.assertEqual(case['expected'],policy.expected_action(case['state']),case['id'])
        self.assertEqual({'FORWARD','TURN_LEFT','TURN_RIGHT'},{c['expected'] for c in cases})

    def test_turn_commit_prevents_corner_oscillation(self):
        cases={c['id']:c for c in json.loads((ROOT/'evidence/policy-fixtures.json').read_text())['cases']}
        self.assertEqual('TURN_RIGHT',policy.expected_action(cases['corner-revisited']['state']))
        turned=copy.deepcopy(cases['corner-revisited']['state'])
        turned['previousAction']='TURN_RIGHT'
        turned['relativeEdges']={'front':'open passage','left':'open passage','right':'wall'}
        self.assertEqual('FORWARD',policy.expected_action(turned))

    def test_expected_labels_never_enter_model_state(self):
        case=json.loads((ROOT/'evidence/policy-fixtures.json').read_text())['cases'][0]
        definition=json.loads((ROOT/'evidence/policy-prompt-definition.json').read_text())
        request=policy.build_request(case,definition,'tev1:0.8b')
        self.assertEqual({'state','questions','model'},set(request))
        self.assertNotIn('expected',request['state'])
        self.assertNotIn('desiredAction',request['state'])
        request['state']['memory'].clear()
        self.assertTrue(case['state']['memory'])

    def test_actual_complete_paired_matrix(self):
        reports=[json.loads((ROOT/'evidence'/f'{n}.json').read_text()) for n in ['forward-08b-off','forward-08b-on','policy-08b-off','policy-08b-on']]
        self.assertEqual('PASS',comparison.compare(*reports)['result'])

    def test_alias_cannot_hide_changed_facts(self):
        reports=[json.loads((ROOT/'evidence'/f'{n}.json').read_text()) for n in ['forward-08b-off','forward-08b-on','policy-08b-off','policy-08b-on']]
        for report in reports[2:]:report['trials'][0]['initialObservation']['current_room_visits']=99
        with self.assertRaises(ValueError):comparison.compare(*reports)

    def test_actual_requests_keep_original_facts_and_memory(self):
        previous=json.loads((ROOT/'evidence/forward-prompt-definition.json').read_text())
        current=json.loads((ROOT/'evidence/policy-prompt-definition.json').read_text())
        for arm in ['off','on']:
            state=dict(current[arm]['state']);visits=state.pop('current_room_visits')
            self.assertEqual(previous[arm]['state'],state)
            self.assertEqual(state['memory'][0][1],visits)
            self.assertEqual(previous[arm]['questions']['action']['criteria'],current[arm]['questions']['action']['criteria'])


if __name__=='__main__':unittest.main()
