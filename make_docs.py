#!/usr/bin/env python3
"""Generate the English offline legend, searchable rule catalogue and instructions."""
from pathlib import Path
import html, json
ROOT=Path(__file__).resolve().parent
D=ROOT/'dist'
style='''body{font:17px/1.6 system-ui,sans-serif;color:#28251f;background:#fffdf8;max-width:1080px;margin:auto;padding:24px}h1,h2,h3{line-height:1.25}h2{margin-top:36px}a{color:#704814}table{border-collapse:collapse;width:100%;margin:16px 0}td,th{text-align:left;vertical-align:top;padding:10px;border-bottom:1px solid #ddd5c7}code{overflow-wrap:anywhere;background:#eee8dd;padding:2px 4px}svg{width:150px;height:30px}.note{border-left:4px solid #b58c43;padding:12px;background:#f5efe2}summary{font-weight:600;cursor:pointer}details{margin:12px 0}li{margin:7px 0}.night code{color:#28251f}.night{background:#002a2a;color:#f4eee2;padding:18px}input{font:inherit;width:min(95%,600px);padding:10px}tr[hidden]{display:none}@media(max-width:600px){body{padding:14px;font-size:16px}table,tbody,tr,td,th{display:block}table{overflow:visible}tr{padding:12px 0;border-bottom:1px solid #ddd5c7}tr:has(th){display:none}td,th{border:0;padding:4px 0;overflow-wrap:anywhere}svg{width:150px}}'''
def doc(title, body):
    return '<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>'+title+'</title><style>'+style+'</style><body>'+body+'</body></html>'
def swatch(colour, dash=''):
    return f'<svg role="img" aria-label="Line sample {colour}" viewBox="0 0 150 30"><path d="M5 15H145" stroke="{colour}" stroke-width="7" stroke-dasharray="{dash}" fill="none"/></svg>'
rows=[('Separated cycleway','#174BCC','Solid',''),('Painted cycle lane','#6C91EF','Long dashes','14 6'),('Shared pedestrian / cycle path','#4A9FD9','Solid, with or without pedestrian separation',''),('Bicycle street','#7DB1D4','Dash-dot; may allow cars','14 4 2 4'),('Shared traffic or bus lane','#7DC8D4','Spaced dashes; shared with vehicles','5 13'),('Cycling prohibited: bicycle=no','#D32F2F','Red transverse marks','1 7')]
legend='<h1>CycloRoute Personal Legend</h1><p>Version 1.3 · OsmAnd Android 5.4.11.</p><p>Open the offline demonstration map: <strong>My Places → My Favourites → CycloRoute Personal → CycloRoute Personal Legend</strong>. Zoom to level 17–18 and scroll down. This is a separate sample map, not a panel over the current route or real infrastructure. Use the location button to return to your position.</p><table><tr><th>Feature</th><th>Sample</th><th>Meaning</th></tr>'
for name,col,note,dash in rows:
    legend+=f'<tr><td>{name}<br><code>{col}</code></td><td>{swatch(col,dash)}</td><td>{note}</td></tr>'
legend+='</table><p>Bicycle line width: Configure map → Bicycle line width → Standard (×1), Thicker (×1.5), Maximum (×2). These HTML samples show colours; inspect your selected width in the native offline legend.</p><div class="night"><h2>The same bicycle colours at night</h2>'+''.join(swatch(c,d) for _,c,_,d in rows)+'</div>'
legend+='<p class="note">Infrastructure stored as road tags may be coloured along the road centreline. This does not indicate where to ride within the carriageway. No colour marking is proof of current legal access. Missing tags do not prove absence of a cycle facility.</p><h2>Along a route</h2><p>The navigation line remains thin and purple. Look at the surrounding blue/cyan map markings to identify bicycle infrastructure. It is easier at close zoom and may be ambiguous in overview. The route does not change colour by infrastructure type. Infrastructure percentages and a segment list are not implemented.</p><h2>Other roads</h2><table><tr><th>Feature</th><th>Day</th><th>Night</th></tr>'
for name,day,night in [('Motorway and trunk','#C5B6A4','#756755'),('Primary road','#D8C5A5','#81705A'),('Secondary road','#E5D8BD','#887B65'),('Tertiary road','#EEE7D9','#807B70'),('Residential and unclassified','#F4F1EB','#77736C'),('Service road','#E9E4DA','#716B62'),('Pedestrian street','#E6DDD4','#71675E'),('Track','#73634E','#73634E'),('Path','#8C7458','#AE9C82')]:
    legend+=f'<tr><td>{name}</td><td>{swatch(day)}<code>{day}</code></td><td class="night">{swatch(night)}<code>{night}</code></td></tr>'
legend+='</table><h2>Complete rendering rule catalogue</h2><p>These are rules and conditional variants, not a count of distinct map features or a promise to show everything simultaneously. Visibility depends on map data, zoom and settings. Upstream metadata is preserved verbatim.</p><label>Find a feature or tag: <input id="search" type="search" placeholder="For example: waterway, parking, bicycle"></label><p id="count"></p>'
catalog=json.loads((D/'rendering-catalog.json').read_text())
for section,title in [('line','Lines'),('point','Icons and points'),('polygon','Areas'),('text','Labels')]:
    entries=[e for e in catalog if e['section']==section]
    legend+=f'<details><summary>{title} — {len(entries)} rules</summary><table><tr><th>Feature</th><th>Conditions and appearance</th><th>Source</th></tr>'
    for e in entries:
        tag=e.get('tag','');val=e.get('value','*') or '*'
        attrs={k:v for k,v in e.items() if k not in ['source','section','tag','value']}
        legend+='<tr class="entry"><td><code>'+html.escape(tag+'='+val if tag else e.get('element','condition'))+'</code></td><td>'+html.escape(json.dumps(attrs,ensure_ascii=False))+'</td><td>'+html.escape(e['source'])+'</td></tr>'
    legend+='</table></details>'
legend+="""<script>const s=document.getElementById('search'),r=Array.from(document.querySelectorAll('.entry')),c=document.getElementById('count');function filter(){const q=s.value.toLowerCase();let n=0;r.forEach(e=>{e.hidden=!e.textContent.toLowerCase().includes(q);if(!e.hidden)n++});c.textContent='Matching rules: '+n;if(q)document.querySelectorAll('details').forEach(e=>e.open=!!e.querySelector('.entry:not([hidden])'));}s.addEventListener('input',filter);filter();</script>"""
legend+='<p>Sources: <a href="https://github.com/OsmAnd-Rendering/Cycling">CycloRoute</a> and <a href="https://github.com/osmandapp/OsmAnd-resources">OsmAnd resources</a>. Licence notices accompany the source.</p>'
page=doc('CycloRoute Personal Legend',legend)
(D/'Legend.html').write_text(page)
# Keep links and bookmarked filenames from earlier versions working.
(D/'Легенда.html').write_text(page)
manual='<h1>CycloRoute Personal — installation</h1><pre style="white-space:pre-wrap">'+html.escape((ROOT/'INSTALL.txt').read_text())+'</pre><p><a href="Legend.html">Full legend and searchable catalogue</a>. See README.md for details, VALIDATION.md for evidence, and routing/README.md for optional BRouter routing.</p>'
for name in ['Instructions.html','Инструкция.html']:
    (D/name).write_text(doc('CycloRoute Personal — installation',manual))
print('Written English offline legend and instructions')
