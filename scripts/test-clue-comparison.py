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

if __name__=='__main__':unittest.main()
