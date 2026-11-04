"""Check the shipped package and preservation of all pre-existing style rules."""
from pathlib import Path
import hashlib
import json
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def semantic(element):
    return element.tag, sorted(element.attrib.items()), tuple(map(semantic, element))


def without_new_setting(data):
    root = ET.fromstring(data)
    for parent in list(root.iter()):
        for child in list(parent):
            if child.get('attr') == 'cyclorouteBikeWidth' or 'cyclorouteBikeWidth' in child.attrib:
                parent.remove(child)
    return semantic(root)


with zipfile.ZipFile(ROOT / 'releases/CycloRoute-Personal-1.1.osf') as old, \
        zipfile.ZipFile(ROOT / 'CycloRoute-Personal.osf') as new:
    assert set(old.namelist()) == set(new.namelist()), 'Unexpected resources added/removed'
    for name in old.namelist():
        if name.endswith('.render.xml'):
            assert semantic(ET.fromstring(old.read(name))) == without_new_setting(new.read(name)), name
        else:
            assert old.read(name) == new.read(name), f'Unchanged resource modified: {name}'
    manifest = json.loads((ROOT / 'manifest.json').read_text())
    for name, expected in manifest['sha256'].items():
        assert hashlib.sha256(new.read(name)).hexdigest() == expected, name
    assert manifest['version'] == '1.2'
    assert not manifest['imports_navigation_profile']
    assert hashlib.sha256((ROOT / 'CycloRoute-Personal.osf').read_bytes()).hexdigest() == manifest['osf_sha256']
    parent = ET.fromstring(new.read('rendering/CycloRoutePersonalBase.render.xml'))
    prop = next(p for p in parent.findall('renderingProperty') if p.get('attr') == 'cyclorouteBikeWidth')
    assert prop.get('possibleValues') == 'Толще,Максимальная'
    assert prop.get('defaultValueDescription') == 'Текущая'
print('PASS: package hashes, rollback package, unchanged legend/resources, and all existing style rules preserved')
