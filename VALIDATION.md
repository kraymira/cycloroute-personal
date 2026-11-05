# Version 1.3 validation

Date: 5 November 2026. Device: Samsung Galaxy S23, OsmAnd Android 5.4.11.

## Computer checks

- `python3 tests/verify_package.py`: package hashes and resource set; all existing drawing rules preserved after excluding the added width-setting branches; 25 English labels verified inside the real binary OBF; English favourite name and group; historical rollback package retained.
- `VerifyStyle`: 1839 colour/width checks with OsmAnd's actual Java rendering rules; OBF opens with BinaryMapIndexReader, map index exists and routing index is absent.
- `VerifyWidthModes`: 6884 checks covering English and legacy saved values, all infrastructure categories, zoom 10–19, day/night, casings, ordinary roads, prohibitions and unchanged navigation-line attributes.
- Compared old and new legend OSM after excluding name tags: geometry, IDs and classification/access tags are identical.

## Phone checks

- Backed up the existing rendering directory, legend OBF and project's favourites GPX before changes. Backups stay local and are not included in the repository.
- Installed the exact generated rendering files and English OBF over USB ADB. Phone SHA-256 values match the package manifest. This is a direct file installation, not a claim that the full v1.3 OSF import wizard was exercised.
- Selected the existing `Brouter[cycleways-preferred]` map profile and retained `CycloRoutePersonalMVP`, Day mode, Bold outline and Map magnifier 25%.
- Opened the legend from My Places. Verified readable English native map labels for the main categories at the 50 m scale bar; the geometry and colours remain visible.
- Renamed the existing legend bookmark, translated its description and removed the obsolete Russian duplicate. Read the saved GPX back: exactly one point, named `CycloRoute Personal Legend`, with an English description.
- Opened Configure map → Bicycle line width: Standard, Thicker and Maximum are shown in English.
- Selected Maximum and visually confirmed wider coloured lines, then restored Standard and confirmed that the map returned to the thinner appearance without an app restart.
- Left the English legend open on the phone. Screenshots: [Standard](tests/android-english-2026-11-05/english-legend-final.png), [Maximum](tests/android-english-2026-11-05/english-legend-maximum.png), [English options](tests/android-english-2026-11-05/english-width-options.png).

One brief USB disconnect was resolved by restarting the ADB server; it did not require a phone reset.

## Limits

The new English legend was visually checked in daytime. All 25 labels and day/night drawing rules were computer-checked; not every native sample or night view was reinspected. Thicker's scaling and saved legacy values were checked with the actual Java renderer; native visual comparison used Standard and Maximum. Restart persistence and reimport of the complete v1.3 OSF have not been separately tested. Routing logic and the absence of a hard native 20% detour limit are unchanged. There is still no infrastructure percentage or segment list in the route UI.

## Historical v1.2 record

# Проверки версии 1.2

Дата: 4 ноября 2026. Целевая версия: Android OsmAnd 5.4.11, Galaxy S23.

## Выполнено

- Сборка установочного `.osf` через `python3 release.py`.
- **1839 проверок** существующих цветов и толщин, плюс чтение OBF легенды — реальный Java `RenderingRulesStorage` OsmAnd.
- **10 проверок** существующего оформления навигационной линии MVP.
- **4132 проверки** нового параметра: все пять категорий, `cycleway` и `cycleway:both`, попутные/встречные полосы, zoom 10–19, день/ночь, толщина и существующая подложка, мосты/тоннели и одинаковая графика велопешеходных путей.
- Обычные дороги, параметры линии маршрута и красного обозначения `bicycle=no` не меняются при переключении.
- Неизвестное значение настройки возвращает базовую толщину. Улицы сохраняют исходный порог видимости.
- Сравнение с сохранённой 1.1: после удаления нового параметра и его условий все прежние правила XML совпадают. Данные легенды, избранное и состав пакета не изменены. SHA-256 файлов совпадают с манифестом.

Тесты: `tests/VerifyWidthModes.java`, `tests/verify_package.py`; команды — в `DEVELOPMENT.md`.

## Не выполнено на телефоне

При сборке 1.2 ADB не обнаруживает устройство. Java-проверки не подтверждают внешний вид и работу интерфейса нативного Android-рендерера.

- Импорт 1.2 и видимость пункта «Толщина велолиний» с тремя вариантами.
- Изменение карты без перезапуска, сохранение после перезапуска и переключения профиля.
- Дневной/ночной режимы, линейка 5 km при Map magnifier 25%.
- Изменение образцов офлайн-легенды и читаемость велопутей под построенным маршрутом.
- Обратный импорт сохранённого установочного пакета 1.1.

Версия 1.1 ранее проверялась на этом телефоне, включая magnifier 25% и линейку 5 km. Эти результаты не означают, что новый переключатель 1.2 уже проверен на Android.
