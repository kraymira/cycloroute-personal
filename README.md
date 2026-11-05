# CycloRoute Personal

A cycling map style for **OsmAnd Android**, based on [CycloRoute](https://github.com/OsmAnd-Rendering/Cycling). It highlights bicycle infrastructure, uses muted colours for ordinary roads, and keeps the route line thin so the map remains visible around it.

**Version 1.3** translates the offline legend, its bookmark and the bicycle line width control into English. It preserves the three width settings introduced in 1.2, including previously saved Russian setting values. Earlier map and routing behaviour was checked on a Galaxy S23 with OsmAnd **5.4.11**; the English legend, bookmark and width selector were checked on that phone on 5 November 2026. See [validation](VALIDATION.md).

## Install or update

1. Download [CycloRoute-Personal.osf](CycloRoute-Personal.osf) and open it with **OsmAnd**, or use **Settings → Import from file**.
2. Import all resources. When updating, replace the existing rendering styles and legend map. The original CycloRoute remains a separate style.
3. In your cycling profile, select **Configure map → Map style → CycloRoutePersonalMVP**.
4. For more map detail at the same physical extent, hold **+** or **−** and choose **Map magnifier → 25%**, then adjust the zoom normally. This increases detail across the whole map and can also increase clutter or rendering load. The OSF does not set it automatically.

The main OSF adds two rendering files, an offline legend map and a favourites group. It does **not** install BRouter, import a navigation profile or change the routing engine. BRouter is not needed just to view the map. [OsmAnd documentation](https://osmand.net/docs/user/map/vector-maps/#map-magnifier).

## Open the legend

**My Places → My Favourites → CycloRoute Personal → CycloRoute Personal Legend**.

Open the favourite on the map, zoom to approximately level 17–18 and scroll down through the labelled samples. The legend is a **separate offline demonstration map**, not real infrastructure and not a panel over your current map or route. It contains map data only, with no routing graph. Use the location button to return to your position; save a marker first if you want to return to a different previously viewed place.

When upgrading from the Russian legend, an old favourite named **«Легенда CycloRoute Personal»** may remain. You can remove that old bookmark and use the English one. The map keeps the same filename, so choose replacement rather than keeping a second copy of the OBF.

## See infrastructure along a planned or active route

Select the intended **map profile** and `CycloRoutePersonalMVP` style as well as your routing profile. Choosing a routing mode in the route panel alone does not necessarily switch the map profile.

The route itself is a **thin, single-colour purple line**. Bicycle infrastructure is identified by the blue or cyan map markings visible around it; ordinary roads keep their neutral styling. The route line does **not** change colour for each infrastructure type. Dashes requested in the rendering XML did not appear on the tested native route line, so the working design uses a thin solid line.

This is a map-reading aid, **not a segment-by-segment infrastructure report**. It is easier to inspect at a close zoom; visibility can become ambiguous when zoomed out or where other markings overlap. Missing map tags do not prove there is no bicycle facility on the ground. The UI currently provides **no infrastructure percentage and no infrastructure segment list**. Map display uses OsmAnd OBF data; BRouter routing uses separate RD5 data, which may have a different update date.

Optional bicycle routing: [BRouter profiles and limitations](routing/README.md). The preferred profile allows legal ordinary-road connections when the bicycle network is incomplete. A strict **20% detour limit is not enforced inside OsmAnd**; the two-pass reference adapter runs separately on a computer.

## Change bicycle line width in OsmAnd

With `CycloRoutePersonalMVP` selected, open **Configure map → Bicycle line width**. Scroll down if necessary.

| Setting | Width |
| --- | --- |
| Standard | ×1, the original 1.1 widths |
| Thicker | ×1.5 |
| Maximum | ×2 |

Changing this setting does not require another import. It affects bicycle infrastructure only; ordinary roads, the navigation line and red prohibition marks keep their existing widths. Existing casings remain two renderer units wider than the coloured line. The native offline legend uses the selected width too. Previously saved 1.2 values `Толще` and `Максимальная` retain their old multipliers; new selections use English values.

For more facilities at overview zooms use **Map magnifier 25%**. Width changes alone cannot display geometry missing from that map detail level.

## Colours

| Feature | Colour |
| --- | --- |
| Separated cycleway | `#174BCC` |
| Painted cycle lane | `#6C91EF` |
| Shared pedestrian / cycle path | `#4A9FD9` |
| Bicycle street | `#7DB1D4` |
| Shared traffic or bus lane | `#7DC8D4` |
| `bicycle=no` | Red transverse marks |

Shared pedestrian/cycle paths look the same with or without pedestrian separation. Bicycle streets may allow cars; painted and shared lanes do not imply physical separation from traffic. For infrastructure encoded as tags on a road, the colour can follow the road centreline and does not show the precise lateral position of the cycle lane.

## Build and rollback

Download the source using **Code → Download ZIP**. With Python 3:

```sh
python3 release.py
python3 tests/verify_package.py
```

The normal package build uses the checked-in legend OBF and needs Python's standard library only. If you edit legend names or geometry, first regenerate `source/CycloRoutePersonalLegend.obf` with OsmAndMapCreator as described in [DEVELOPMENT.md](DEVELOPMENT.md); rebuilding only the XML does not translate an old binary map.

Edit `line-widths.json` to change base widths, run the build and reimport the OSF. To restore the original width choose **Standard**. For the complete previous 1.1 appearance import [CycloRoute-Personal-1.1.osf](releases/CycloRoute-Personal-1.1.osf) and replace matching resources; that historical package retains its Russian legend.

Pinned upstream sources and licences are in `source/`; checks are in `tests/`. See [DEVELOPMENT.md](DEVELOPMENT.md), [VALIDATION.md](VALIDATION.md) and [NOTICE.md](NOTICE.md). Project and rendering styles: **GPL-3.0**.
