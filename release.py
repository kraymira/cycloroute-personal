#!/usr/bin/env python3
"""Build the portable map-style package with Python 3 standard library only."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
DIST = ROOT / 'dist'


def build():
    widths = json.loads((ROOT / 'line-widths.json').read_text())
    previous_max = 9
    for index, row in enumerate(widths):
        minimum, maximum, width = row['min'], row.get('max'), row['width']
        if minimum != previous_max + 1 or not isinstance(width, (int, float)) or not 0 < width <= 30:
            raise ValueError('Widths must be positive (up to 30) and zoom ranges contiguous from 10.')
        if maximum is None:
            if index != len(widths) - 1:
                raise ValueError('Only the last range may have no maximum.')
        elif maximum < minimum:
            raise ValueError('Maximum zoom must not precede minimum zoom.')
        previous_max = maximum if maximum is not None else 99
    if not widths or widths[-1].get('max') is not None:
        raise ValueError('The final zoom range must be open ended.')
    DIST.mkdir(exist_ok=True)
    shutil.copyfile(ROOT / 'source/CycloRoutePersonalLegend.obf', DIST / 'CycloRoutePersonalLegend.obf')
    shutil.copyfile(ROOT / 'routing/cycleways-strict.brf', DIST / 'cycleways-strict.brf')
    for script in ['build.py', 'build_mvp.py', 'make_docs.py']:
        subprocess.run([sys.executable, str(ROOT / script)], cwd=ROOT, check=True)
    files = {
        'rendering/CycloRoutePersonalMVP.render.xml': DIST / 'mvp/CycloRoutePersonalMVP.render.xml',
        'rendering/CycloRoutePersonalBase.render.xml': DIST / 'rendering/CycloRoutePersonalBase.render.xml',
        'CycloRoutePersonalLegend.obf': DIST / 'CycloRoutePersonalLegend.obf',
        'favourites.gpx': DIST / 'favourites.gpx',
    }
    items = [
        {'type': 'FILE', 'file': '/' + name, 'subtype': 'rendering_style'}
        for name in files if name.startswith('rendering/')
    ]
    items.extend([
        {'type': 'FILE', 'file': '/CycloRoutePersonalLegend.obf', 'subtype': 'obf_map'},
        {'type': 'FAVOURITES', 'file': 'favourites.gpx'},
    ])
    output = ROOT / 'CycloRoute-Personal.osf'
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('items.json', json.dumps({'version': 1, 'items': items}, indent=2))
        for name, path in files.items():
            archive.write(path, name)
    manifest = {
        'version': '1.1-share',
        'style_display_name': 'CycloRoutePersonalMVP',
        'target': 'Android OsmAnd 5.4.11',
        'recommended_map_magnifier_percent': 25,
        'imports_navigation_profile': False,
        'upstream_cycloroute_commit': 'bddd8ba813918659a9c562ad8960c29ac1384350',
        'upstream_osmand_resources_commit': '17e7e474005ee33ce894d6f8838f3b4428b705e4',
        'sha256': {name: hashlib.sha256(path.read_bytes()).hexdigest() for name, path in files.items()},
        'osf_sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
    }
    (ROOT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('Built', output)


if __name__ == '__main__':
    build()
