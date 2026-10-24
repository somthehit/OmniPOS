import re

filepath = r'app\src\main\java\com\example\pos\ui\screens\SettingsScreen.kt'
with open(filepath, encoding='utf-8') as f:
    lines = f.readlines()

# Find lines with hardcoded English strings in common patterns
patterns = [
    re.compile(r'text\s*=\s*"[A-Z]'),
    re.compile(r'placeholder\s*=\s*\{\s*Text\("'),
    re.compile(r'label\s*=\s*"[A-Z]'),
    re.compile(r'contentDescription\s*=\s*"[A-Z]'),
]
for i, line in enumerate(lines, 1):
    for p in patterns:
        if p.search(line):
            print(str(i) + ': ' + line.rstrip())
            break
