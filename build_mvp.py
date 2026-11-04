#!/usr/bin/env python3
"""Build the revised, non-strict MVP alongside the unchanged strict prototype."""
from pathlib import Path
import xml.etree.ElementTree as ET
import json, zipfile
ROOT = Path(__file__).resolve().parent
OUT = ROOT/'dist'/'mvp'
OUT.mkdir(exist_ok=True)
strict = (ROOT/'dist/cycleways-strict.brf').read_text()
head, rest = strict.split('---context:way', 1)
head = head[head.index('---context:global'):].replace('waypointCatchingRange = 30', 'waypointCatchingRange = 100')
head = head.replace('pass1coefficient = 1.5', 'pass1coefficient = 1.0')
head += '# No slope or turn penalties: keep detour preference modest.\nassign uphillcost = 0\nassign downhillcost = 0\n'
way, node = rest.split('---context:node', 1)
way = way[:way.index('assign costfactor =')]
way += '''# A normal road remains usable only in its legally permitted direction.
assign normal_road = highway=residential|living_street|service|unclassified|tertiary|tertiary_link|secondary|secondary_link|primary|primary_link|track|road
assign explicit_path = and highway=path|footway|pedestrian|bridleway explicit_bike_access
assign usable = or admissible and or normal_road explicit_path not normal_oneway_blocked
assign right_lane = if not cycleway:right= then cycleway:right=lane else cycleway:both=lane
assign left_lane = if not cycleway:left= then cycleway:left=lane else cycleway:both=lane
assign generic_lane = and cycleway=lane and cycleway:left= and cycleway:right= cycleway:both=
assign painted_lane =
  or and right_lane right_direction
     or and left_lane left_direction and generic_lane not normal_oneway_blocked
assign shared = or cycleway=shared_lane|share_busway
  or and cycleway:right=shared_lane|share_busway right_direction
     or and cycleway:left=shared_lane|share_busway left_direction cycleway:both=shared_lane|share_busway
assign costfactor =
  if or forbidden_kind or access_blocked direction_blocked then 10000
  else if not usable then 10000
  else if admissible then 1
  else if painted_lane then 1.08
  else if shared then 1.16
  else 1.20
assign turncost = 0
assign initialcost = 0
assign nodeaccessgranted = false
assign classifiermask = 0
'''
node = node.replace('else if highway=traffic_signals then 20\n', '')
profile = '# CycloRoute Personal MVP: prefer infrastructure, allow legal road connections.\n# 1.20 is a soft cost setting, NOT an enforced maximum route-length ratio.\n'+head+'\n---context:way'+way+'\n---context:node'+node
(OUT/'cycleways-preferred.brf').write_text(profile)
baseline = profile[:profile.index('assign costfactor =')] + profile[profile.index('assign costfactor ='):].replace('else if admissible then 1\n  else if painted_lane then 1.08\n  else if shared then 1.16\n  else 1.20', 'else 1')
(OUT/'cycleways-baseline.brf').write_text(baseline)
# Separate optional style: dash gaps expose the existing infrastructure colours.
style = ET.parse(ROOT/'dist/rendering/CycloRoutePersonal.render.xml')
style.getroot().set('name', 'CycloRoute Personal MVP')
route = next(e for e in style.getroot().findall('renderingAttribute') if e.get('name')=='route')
route.clear(); route.set('name','route')
ET.SubElement(route,'case',color='#E0C03EAD',strokeWidth='0.7:0',pathEffect='5_7',color_3='#F7BB36',strokeWidth_3='1.5:0')
ET.indent(style,space='  ')
style.write(OUT/'CycloRoutePersonalMVP.render.xml',encoding='utf-8',xml_declaration=True)
items=[{'type':'FILE','file':'/rendering/'+name,'subtype':'rendering_style'} for name in ['CycloRoutePersonalMVP.render.xml','CycloRoutePersonalBase.render.xml']]
with zipfile.ZipFile(OUT/'CycloRoute-Personal-MVP.osf','w',zipfile.ZIP_DEFLATED) as z:
    z.writestr('items.json',json.dumps({'version':1,'items':items}))
    z.write(OUT/'CycloRoutePersonalMVP.render.xml','rendering/CycloRoutePersonalMVP.render.xml')
    z.write(ROOT/'dist/rendering/CycloRoutePersonalBase.render.xml','rendering/CycloRoutePersonalBase.render.xml')
print('Built separate MVP profiles and optional route-visibility style in',OUT)

# Build an installable profile package from the verified OsmAnd export.
mode_path = ROOT/'source/mvp-appmode-template.json'
profile_path = ROOT/'source/mvp-profile-template.json'
if mode_path.exists() and profile_path.exists():
    mode = json.loads(mode_path.read_text())
    items = [{'type':'PROFILE','file':'profile_bicycle_cycleways_mvp.json','appMode':mode}]
    items += [{'type':'FILE','file':'/rendering/'+name,'subtype':'rendering_style'} for name in ['CycloRoutePersonalMVP.render.xml','CycloRoutePersonalBase.render.xml']]
    with zipfile.ZipFile(OUT/'CycloRoute-MVP-Android.osf','w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('items.json',json.dumps({'version':3,'items':items}))
        z.write(profile_path,'profile_bicycle_cycleways_mvp.json')
        z.write(OUT/'CycloRoutePersonalMVP.render.xml','rendering/CycloRoutePersonalMVP.render.xml')
        z.write(ROOT/'dist/rendering/CycloRoutePersonalBase.render.xml','rendering/CycloRoutePersonalBase.render.xml')
