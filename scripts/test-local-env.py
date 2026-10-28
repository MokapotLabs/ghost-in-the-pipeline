import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from local_env import openrouter_key


class LocalEnvChecks(unittest.TestCase):
    def read(self, text):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / '.env.local'
            path.write_text(text)
            return openrouter_key(path)

    @patch.dict(os.environ, {'OPENROUTER_API_KEY': ''})
    def test_only_key_assignment_is_read_without_execution(self):
        self.assertEqual('literal-$(exit)-`exit`', self.read(
            'UNRELATED=private\nexport OPENROUTER_API_KEY="literal-$(exit)-`exit`" # comment\n'))

    @patch.dict(os.environ, {'OPENROUTER_API_KEY': ' existing '})
    def test_existing_environment_takes_precedence(self):
        self.assertEqual('existing', self.read('OPENROUTER_API_KEY=other'))

    @patch.dict(os.environ, {'OPENROUTER_API_KEY': ''})
    def test_missing_and_invalid_assignments(self):
        self.assertEqual('', self.read('OTHER=value'))
        with self.assertRaises(ValueError):
            self.read('OPENROUTER_API_KEY="unterminated')
        with self.assertRaises(ValueError):
            self.read('OPENROUTER_API_KEY=multiple values')


if __name__ == '__main__':
    unittest.main()
