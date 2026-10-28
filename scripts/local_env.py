"""Read only the approved host credential from a local dotenv file; never execute it."""
import os
from pathlib import Path
import re
import shlex


def openrouter_key(path=Path('.env.local')):
    existing = os.getenv('OPENROUTER_API_KEY', '').strip()
    if existing:
        return existing
    if not path.exists():
        return ''
    key = ''
    for line in path.read_text().splitlines():
        match = re.match(r'^\s*(?:export\s+)?OPENROUTER_API_KEY\s*=(.*)$', line)
        if match:
            try:
                values = shlex.split(match[1], comments=True)
            except ValueError:
                raise ValueError('Invalid OPENROUTER_API_KEY assignment in local dotenv') from None
            if len(values) > 1:
                raise ValueError('Quote the OPENROUTER_API_KEY value in local dotenv')
            key = values[0].strip() if values else ''
    return key


if __name__ == '__main__':
    print(openrouter_key(), end='')
