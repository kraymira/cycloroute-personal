#!/usr/bin/env python3
"""Build a reproducible, isolated CycloRoute derivative and its demo map."""
from pathlib import Path
import xml.etree.ElementTree as ET
import colorsys, copy, json, re, zipfile, hashlib, html

ROOT = Path(__file__).resolve().parent
SRC, DIST = ROOT/'source', ROOT/'dist'
RENDER = DIST/'rendering'
RENDER.mkdir(parents=True, exist_ok=True)
BLUE, LANE, MIXED, STREET, SHARED = '#174BCC','#6C91EF','#4A9FD9','#7DB1D4','#7DC8D4'
PALETTE = {'cycleway':BLUE,'lane':LANE,'mixed':MIXED,'street':STREET,'shared':SHARED}
WIDTH_PROPERTY = 'cyclorouteBikeWidth'
WIDTH_MODES = (('Thicker', 1.5), ('Maximum', 2))
# Keep saved v1.2 preference values working without showing Russian choices.
LEGACY_WIDTH_MODES = (('Толще', 1.5), ('Максимальная', 2))

def load(name):
    return ET.parse(SRC/name, parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True))).getroot()

def write_xml(root, path):
    ET.indent(root, space='  ')
    ET.ElementTree(root).write(path, encoding='utf-8', xml_declaration=True)

def cold(v):
    if not re.fullmatch(r'#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?',v): return False
    s=v[-6:]; rgb=[int(s[i:i+2],16)/255 for i in (0,2,4)]
    h,sat,val=colorsys.rgb_to_hsv(*rgb)
    return 155 <= h*360 <= 280 and sat>.07

def warm(v):
    if not cold(v): return v
    prefix=v[:-6]; rgb=[int(v[-6:][i:i+2],16)/255 for i in (0,2,4)]
    h,l,s=colorsys.rgb_to_hls(*rgb)
    return prefix+''.join(f'{round(x*255):02X}' for x in colorsys.hls_to_rgb(32/360,l,min(s,.30)))

def recolor_tree(e):
    for x in e.iter():
        for k,v in list(x.attrib.items()):
            if 'color' in k.lower(): x.set(k,warm(v))

def set_attribute(root,name,day,night=None):
    e=next((e for e in root.findall('renderingAttribute') if e.get('name')==name),None)
    if e is None: e=ET.SubElement(root,'renderingAttribute',name=name)
    e[:]=[]
    if night: ET.SubElement(e,'case',nightMode='true',attrColorValue=night)
    ET.SubElement(e,'case',attrColorValue=day)
    return e

base, cyc = load('default.original.render.xml'), load('CycloRoute.original.render.xml')
base.set('name','CycloRoutePersonalBase')
cyc.set('name','CycloRoute Personal');cyc.set('depends','CycloRoutePersonalBase')
# Define in the parent: bicycle-street rules are parsed there as well.
# OsmAnd adds the empty/default choice itself, giving exactly three choices.
base.insert(0, ET.Element('renderingProperty', {
    'attr': WIDTH_PROPERTY, 'name': 'Bicycle line width', 'type': 'string',
    'description': 'Standard: x1; Thicker: x1.5; Maximum: x2. Bicycle infrastructure only.',
    'possibleValues': ','.join(name for name, _ in WIDTH_MODES),
    'defaultValueDescription': 'Standard',
}))

# Freeze the parent so future changes to the built-in default cannot reintroduce
# blue road colours. Non-road sections are retained byte-for-byte semantically.
road_values={'motorway':('#C5B6A4','#756755'),'trunk':('#C5B6A4','#756755'),
 'primary':('#D8C5A5','#81705A'),'secondary':('#E5D8BD','#887B65'),
 'tertiary':('#EEE7D9','#807B70'),'residential':('#F4F1EB','#77736C'),
 'unclassified':('#F4F1EB','#77736C'),'service':('#E9E4DA','#716B62'),
 'serviceDriveway':('#E9E4DA','#716B62'),'busway':('#D9D3C8','#7A7166'),
 'busGuideway':('#D9D3C8','#7A7166'),'pedestrian':('#E6DDD4','#71675E'),
 'road':('#DDD6CA','#746B60')}
for root in (base,cyc):
    for e in root.findall('renderingAttribute'):
        name=e.get('name','')
        if any(t in name.lower() for t in ['roadcolor','roadlowzoomcolor','roadshadowcolor','surface','tracktype','sacscale','pathcolor','pathlowzoom','trackcolor','iceroad']):
            recolor_tree(e)
    for e in root.findall('renderingConstant'):
        if any(t in e.get('name','').lower() for t in ['surface','sacscale','pathcolor','trackcolor','cycleroute']):
            e.set('value',warm(e.get('value','')))
    for stem,(day,night) in road_values.items():
        for suffix in ['RoadColor','RoadLowZoomColor']:
            name=stem+suffix
            if any(e.get('name')==name for e in root.findall('renderingAttribute')):
                set_attribute(root,name,day,night)

    for section in ['line','polygon','text']:
        sec=root.find(section)
        if sec is None: continue
        for group in sec:
            tags={x.get('tag') for x in group.iter() if x.get('tag')}
            if tags and all(t in {'highway','abandoned:highway','area:highway','cycleway','cycleway:both','bicycle_road','sidewalk'} or t.startswith('osmand_cycleway_') for t in tags):
                recolor_tree(group)
                # Only bicycle=no gets a new access marking. Keep other data,
                # but remove inherited "showAccess" decorations on roads.
                for par in list(group.iter()):
                    for ch in list(par):
                        if ch.get('showAccess')=='true': par.remove(ch)

for e in cyc.findall('renderingConstant'):
    name=e.get('name')
    if name=='cyclewayColorDay':e.set('value',BLUE)
    if name=='pathColorDay':e.set('value','#8C7458')
    if name=='pathColorNight':e.set('value','#AE9C82')
    if name=='trackColor':e.set('value','#73634E')
    if name=='bridlewayColorDay':e.set('value','#897440')
    routes={'cycleRoutelcnColor':'#B29969','cycleRoutercnColor':'#A48143','cycleRoutencnColor':'#916735','cycleRouteicnColor':'#784B26'}
    if name in routes:e.set('value',routes[name])

# These attributes are road overlays, never water/POI colours.
for n in ['surfaceConcreteColor','surfacePavedColor','surfacePavingStonesColor','surfaceSettColor','surfaceCobblestoneColor','surfacePebblestoneColor','surfaceStoneColor','surfaceMetalColor','surfaceWoodColor']:
    set_attribute(cyc,n,'#B2A087','#C6BBA9')
for n in ['surfaceGravelColor','surfaceCompactedColor','surfaceFineGravelColor']:
    set_attribute(cyc,n,'#A89778','#BDB093')
for n in ['surfaceSaltColor','surfaceSnowColor','surfaceIceColor']:
    set_attribute(cyc,n,'#D9D0BF','#D5CFC3')

def bike_widths(parent, attr='strokeWidth', extra=0, casing=None):
    # Keep cycling infrastructure readable when zooming out. Low-zoom rules
    # apply only where the loaded OBF actually contains the relevant objects.
    for row in json.loads((ROOT/'line-widths.json').read_text()):
        lo,hi,w = row['min'],row.get('max'),row['width']
        a={'minzoom':str(lo),attr:str(w+extra)}
        if casing:a[casing]=str(w+extra+2)
        if hi is not None:a['maxzoom']=str(hi)
        rule = ET.SubElement(parent,'apply_if',a)
        for name, multiplier in WIDTH_MODES + LEGACY_WIDTH_MODES:
            scaled = (w + extra) * multiplier
            override = {WIDTH_PROPERTY: name, attr: f'{scaled:g}'}
            if casing: override[casing] = f'{scaled + 2:g}'
            ET.SubElement(rule, 'apply_if', override)

def prohibition(parent, width='9:3'):
    ET.SubElement(parent,'apply_if',{'additional':'bicycle=no','minzoom':'13','color_5':'#D32F2F','strokeWidth_5':width,'pathEffect_5':'1_7','cap_5':'BUTT'})

line=cyc.find('line')
# Route-network overlays must not cover the wider infrastructure colours.
for group in [list(line)[0],*list(line)]:
    if group is list(line)[0] or any(e.get('tag')=='route_mtb' for e in group.iter()):
        for x in group.iter():
            if 'strokeWidth' in x.attrib:x.set('strokeWidth','1.5')
# Modify original path groups without replacing their tracktype, surface,
# smoothness, bridge or tunnel logic.
path_group=list(line)[1]
for group in path_group.findall('group'):
    sel=next((e for e in group if e.get('tag')=='highway'),None)
    if sel is None:continue
    kind=sel.get('value')
    if kind in ('footway','path'):
        for ch in list(group):
            if ch.get('additional')=='bicycle=designated':group.remove(ch)
        spec=ET.SubElement(group,'groupFilter',{'additional':'bicycle=designated','minzoom':'10','color_5':MIXED,'pathEffect_5':'','cap_5':'ROUND'})
        bike_widths(spec,'strokeWidth_5',casing='strokeWidth_4')
        ET.SubElement(spec,'apply',{'color_4':'#FFF9EF'})
        ET.SubElement(spec,'apply_if',{'nightMode':'true','color_4':'#29251E'})
    if kind=='cycleway':
        spec=ET.SubElement(group,'groupFilter',{'minzoom':'10','color_5':BLUE,'pathEffect_5':'','cap_5':'ROUND'})
        ET.SubElement(spec,'apply_if',{'additional':'foot=designated','color_5':MIXED})
        bike_widths(spec,'strokeWidth_5',casing='strokeWidth_4')
        ET.SubElement(spec,'apply',{'color_4':'#FFF9EF'})
        ET.SubElement(spec,'apply_if',{'nightMode':'true','color_4':'#29251E'})
prohibition(path_group)

# Replace the old combined blue cycling overlay with per-type rules.
# Stock OBF encodes side markers and type as separate MAIN types. "additional"
# cannot inspect main types, so type-specific colour goes on the road centre;
# neutral side markers retain the known side without inventing its subtype.
for group in list(line):
    tags={e.get('tag') for e in group.iter() if e.get('tag')}
    if tags and any(t in {'cycleway','cycleway:both','bicycle_road','sidewalk'} or t.startswith('osmand_cycleway_') for t in tags):
        line.remove(group)
for tag in ['cycleway','cycleway:both']:
    for value,col,dash in [('track',BLUE,''),('opposite_track',BLUE,''),('lane',LANE,'14_6'),('opposite_lane',LANE,'14_6'),('both',LANE,'14_6'),('shared_lane',SHARED,'5_13'),('share_busway',SHARED,'5_13')]:
        e=ET.SubElement(line,'case',{'tag':tag,'value':value,'minzoom':'10','color':col,'cap':'ROUND','pathEffect':dash,'color_0':'#FFF9EF'})
        ET.SubElement(e,'apply_if',{'nightMode':'true','color_0':'#29251E'})
        bike_widths(e,casing='strokeWidth_0')
        prohibition(e)
    # Contraflow permission by itself is not a dedicated bicycle facility.
    e=ET.SubElement(line,'case',{'tag':tag,'value':'opposite','minzoom':'14','color':'#8C7458','strokeWidth':'1.5','pathEffect':'3_8'})

side=next(e for e in base.find('line') if any(x.get('tag','').startswith('osmand_cycleway_') for x in e.iter()))
side=copy.deepcopy(side);side.attrib.pop('baseAppMode',None)
for x in side.iter():
    if 'color' in x.attrib:x.set('color','#9A8D7A')
line.append(side)
ET.SubElement(line,'case',{'tag':'sidewalk','value':'yes','minzoom':'15','color':'#8C7458','strokeWidth':'1.5','pathEffect':'4_2'})

# Add explicit overlays after base road styling, above surfaces but below the
# explicit prohibition ticks. No global access colouring is added.
for group in base.find('line'):
    if any(e.get('tag')=='highway' and e.get('value') in road_values for e in group.iter()):
        for tag in ['bicycle_road','cyclestreet']:
            e=ET.SubElement(group,'apply_if',{'additional':tag+'=yes','color_5':STREET,'pathEffect_5':'14_4_2_4','cap_5':'ROUND'})
            bike_widths(e,'strokeWidth_5')
        prohibition(group,'14:4')

# Retain original display-only properties, with fewer blue road-level fallbacks.
for e in base.findall('renderingAttribute'):
    if e.get('name') in ['cyclewayColor']:
        e[:]=[];ET.SubElement(e,'case',attrColorValue=BLUE)
write_xml(base,RENDER/'CycloRoutePersonalBase.render.xml')
write_xml(cyc,RENDER/'CycloRoutePersonal.render.xml')

# A compact, non-routable offshore demonstration atlas, never uploaded to OSM.
osm=ET.Element('osm',version='0.6',generator='CycloRoute Personal local legend')
counter=1000000000
def node(lat,lon,tags=None):
    global counter
    counter+=1;e=ET.SubElement(osm,'node',id=str(counter),lat=f'{lat:.7f}',lon=f'{lon:.7f}',version='1')
    for k,v in (tags or {}).items():ET.SubElement(e,'tag',k=k,v=v)
    return str(counter)
def way(points,tags):
    global counter
    counter+=1;e=ET.SubElement(osm,'way',id=str(counter),version='1')
    for n in points:ET.SubElement(e,'nd',ref=n)
    for k,v in tags.items():ET.SubElement(e,'tag',k=k,v=v)

samples=[('Separated cycleway',{'highway':'cycleway'}),
 ('Painted cycle lane',{'highway':'residential','cycleway':'lane'}),
 ('Shared pedestrian / cycle path',{'highway':'path','bicycle':'designated','foot':'designated'}),
 ('Bicycle street',{'highway':'residential','bicycle_road':'yes'}),
 ('Shared lane with traffic',{'highway':'residential','cycleway':'shared_lane'}),
 ('Shared bus lane',{'highway':'residential','cycleway':'share_busway'}),
 ('Roadside separated cycleway',{'highway':'residential','cycleway':'track'}),
 ('Cycling prohibited',{'highway':'residential','bicycle':'no'})]
samples += [(label,{'highway':v}) for v,label in [('motorway','Motorway'),('trunk','Trunk road'),('primary','Primary road'),('secondary','Secondary road'),('tertiary','Tertiary road'),('residential','Residential street'),('service','Service road'),('track','Track'),('path','Path'),('footway','Footway'),('bridleway','Bridleway'),('steps','Steps')]]
samples += [('Bridge',{'highway':'cycleway','bridge':'yes','layer':'1'}),('Tunnel',{'highway':'cycleway','tunnel':'yes','layer':'-1'}),('Shared pedestrian / cycle path',{'highway':'cycleway','foot':'designated'}),('Cycle lane on the right',{'highway':'residential','cycleway:right':'lane'}),('Separated cycleway on the right',{'highway':'residential','cycleway:right':'track'})]
for i,(label,tags) in enumerate(samples):
    lat=45.7388-i*.00075
    way([node(lat,36.55),node(lat,36.553)],dict(tags,name=label))
write_xml(osm,SRC/'CycloRoutePersonalLegend.osm')
ns='http://www.topografix.com/GPX/1/1'
gpx=ET.Element('gpx',{'version':'1.1','creator':'CycloRoute Personal','xmlns':ns})
wp=ET.SubElement(gpx,'wpt',lat='45.7388',lon='36.5515')
ET.SubElement(wp,'name').text='CycloRoute Personal Legend'
ET.SubElement(wp,'type').text='CycloRoute Personal'
ET.SubElement(wp,'desc').text='Offline demonstration map, not real infrastructure. Zoom to level 17-18 and scroll down for samples. Use the location button to return to your position.'
write_xml(gpx,DIST/'favourites.gpx')

# Catalogue every explicit main tag/value selector in the exact shipped style
# pair. It is an index of rules, not a claim that all are visible simultaneously.
catalog=[]
for source,root in [('CycloRoute Personal',cyc),('CycloRoutePersonalBase',base)]:
    for section in ['line','point','polygon','text']:
        e=root.find(section)
        if e is None:continue
        for x in e.iter():
            if x.attrib:
                catalog.append({'source':source,'section':section,'element':str(x.tag),**x.attrib})
(DIST/'rendering-catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2))
(DIST/'palette.json').write_text(json.dumps(PALETTE,indent=2))
print('Generated styles, demo OSM, favourites and',len(catalog),'catalogue entries')

def package():
    obf=DIST/'CycloRoutePersonalLegend.obf'
    items=[{'type':'FILE','file':'/rendering/'+p.name,'subtype':'rendering_style'} for p in sorted(RENDER.glob('*.render.xml'))]
    if obf.exists():
        items += [{'type':'FILE','file':'/'+obf.name,'subtype':'obf_map'},{'type':'FAVOURITES','file':'favourites.gpx'}]
    (DIST/'items.json').write_text(json.dumps({'version':1,'items':items},ensure_ascii=False,indent=2))
    with zipfile.ZipFile(DIST/'CycloRoute-Personal.osf','w',zipfile.ZIP_DEFLATED) as z:
        z.write(DIST/'items.json','items.json')
        for p in sorted(RENDER.glob('*.render.xml')):z.write(p,'rendering/'+p.name)
        if obf.exists():z.write(obf,obf.name);z.write(DIST/'favourites.gpx','favourites.gpx')
    print('Packaged',DIST/'CycloRoute-Personal.osf','with legend:',obf.exists())

if __name__=='__main__':package()
