import copy, importlib.util, json, unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('compare_clues',ROOT/'scripts/compare-clues.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class ClueControls(unittest.TestCase):
    def setUp(self):
        self.off=json.loads((ROOT/'evidence/haunted-08b-off.json').read_text())
        self.on=json.loads((ROOT/'evidence/haunted-08b-on.json').read_text())
    def test_actual_paired_trials(self):
        self.assertEqual('PASS',module.compare(self.off,self.on)['result'])
    def test_disabled_data_cannot_pass_as_enabled(self):
        self.on['agentClues']=False
        with self.assertRaises(ValueError):module.compare(self.off,self.on)
    def test_changed_starting_reading_rejected(self):
        self.on['trials'][0]['initialNavigation']['signal']='INTENSE'
        with self.assertRaises(ValueError):module.compare(self.off,self.on)
    def test_changed_budget_rejected(self):
        self.on['durationMs']=100000
        with self.assertRaises(ValueError):module.compare(self.off,self.on)
    def test_matching_incomplete_identities_rejected(self):
        self.off['runtimeClassesSha256'].clear();self.on['runtimeClassesSha256'].clear()
        with self.assertRaises(ValueError):module.compare(self.off,self.on)
    def test_duplicate_trial_rejected(self):
        self.on['trials'][1]=copy.deepcopy(self.on['trials'][0])
        with self.assertRaises(ValueError):module.compare(self.off,self.on)
    def test_matching_missing_control_rejected(self):
        self.off.pop('cachePolicy');self.on.pop('cachePolicy')
        with self.assertRaises(ValueError):module.compare(self.off,self.on)
    def test_provider_failures_require_explicit_diagnostic_mode(self):
        failed=self.off['trials'][0]
        failed.update(executionFailure=True,termination='PROVIDER_OR_PIPELINE_FAILURE: Test',status='PLAYING',exitFound=False)
        with self.assertRaises(ValueError):module.compare(self.off,self.on)
        result=module.compare(self.off,self.on,allow_provider_failures=True)
        self.assertEqual('INCOMPLETE_PROVIDER_FAILURE',result['result'])
        self.assertEqual(1,result['arms'][0]['providerFailures'])
        self.assertEqual(9,result['arms'][0]['trials'])
        self.on['durationMs']=100000
        with self.assertRaises(ValueError):module.compare(self.off,self.on,allow_provider_failures=True)
    def test_hosted_identity_must_match_both_arms_and_trial_labels(self):
        for report in [self.off,self.on]:
            report['players']=['JEV'];report['localModels']=[]
            report['hostedModels']=[{'name':'test-hosted-alias','provider':'decision.query.jev'}]
            for trial in report['trials']:trial.update(player='JEV',model='test-hosted-alias')
        self.assertEqual('PASS',module.compare(self.off,self.on)['result'])
        self.on['localOllamaVersion']=None
        self.assertEqual('PASS',module.compare(self.off,self.on)['result'])
        self.on['trials'][0]['model']='different'
        with self.assertRaises(ValueError):module.compare(self.off,self.on)

if __name__=='__main__':unittest.main()
