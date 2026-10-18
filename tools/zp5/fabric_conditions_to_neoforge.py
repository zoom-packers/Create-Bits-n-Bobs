"""Convert Fabric API resource conditions to NeoForge's in data JSON, in place.

    "fabric:load_conditions": [{"condition": "ns:id", ...}]  ->  "neoforge:conditions": [{"type": "ns:id", ...}]

Fabric's own combinators (fabric:and / or / not / all_mods_loaded / any_mods_loaded) map to NeoForge's (neoforge:and,
or, not, mod_loaded). The key keeps its position in the object, so datagen key order survives. Idempotent.

    python -I tools/zp5/fabric_conditions_to_neoforge.py src/main/resources src/generated/resources
"""
import json
import pathlib
import sys

KEY_FABRIC = 'fabric:load_conditions'
KEY_NEO = 'neoforge:conditions'


def condition(c):
    c = dict(c)
    kind = c.pop('condition')
    if kind in ('fabric:and', 'fabric:or'):
        return {'type': 'neoforge:' + kind.split(':')[1], 'values': [condition(v) for v in c['values']]}
    if kind == 'fabric:not':
        return {'type': 'neoforge:not', 'value': condition(c['value'])}
    if kind == 'fabric:all_mods_loaded':
        return {'type': 'neoforge:and', 'values': [{'type': 'neoforge:mod_loaded', 'modid': m} for m in c['values']]}
    if kind == 'fabric:any_mods_loaded':
        return {'type': 'neoforge:or', 'values': [{'type': 'neoforge:mod_loaded', 'modid': m} for m in c['values']]}
    if kind.startswith('fabric:'):
        raise ValueError(f'unhandled Fabric condition {kind}')
    return {'type': kind, **c}


changed = 0
for root in sys.argv[1:]:
    for f in pathlib.Path(root).rglob('*.json'):
        raw = f.read_bytes().decode('utf-8')
        if KEY_FABRIC not in raw:
            continue
        data = json.loads(raw)
        if not isinstance(data, dict) or KEY_FABRIC not in data:
            raise SystemExit(f'{f}: {KEY_FABRIC} is not a top-level key')
        out = {}
        for k, v in data.items():
            if k == KEY_FABRIC:
                out[KEY_NEO] = [condition(c) for c in v]
            else:
                out[k] = v
        f.write_bytes((json.dumps(out, indent=2, ensure_ascii=False) + ('\n' if raw.endswith('\n') else '')).encode('utf-8'))
        changed += 1
print(f'{changed} files converted')
