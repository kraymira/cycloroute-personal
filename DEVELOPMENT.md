# Сборка и проверка

```sh
python3 release.py
```

Сборка использует Python 3 и стандартную библиотеку. На выходе — корневой `CycloRoute-Personal.osf` и `manifest.json` с SHA-256. Конфигурация толщин — `line-widths.json`. Диапазоны должны идти подряд от zoom 10, последний диапазон не имеет `max`. Значения — единицы рендерера, а не километры на линейке; 3D, широта и magnifier влияют на соответствие физическому охвату.

В установочном пакете два стиля (`CycloRoutePersonalMVP`, его зафиксированный родитель), карта легенды и избранное. Он не импортирует профиль навигации. Существующий стиль CycloRoute остаётся доступным отдельно. Карта легенды поставляется готовой; её исходник `source/CycloRoutePersonalLegend.osm` создаётся сборщиком. Для изменения геометрии легенды пересоберите OBF через OsmAndMapCreator `generate-map`, без раздела routing, затем замените `source/CycloRoutePersonalLegend.obf`.

Версия 1.1 повторяет файлы оформления на Galaxy S23, сверенные по SHA-256 4 ноября 2026; её пакет сохранён в `releases/`. Утолщение 1.1 и magnifier 25% проверены на линейке 5 km. Версия 1.2 добавляет параметр `cyclorouteBikeWidth`; импорт и переключение в Android пока не проверены, потому что телефон недоступен по USB. Полнота объектов во всех регионах не проверялась и зависит от OSM/OBF. Настройка magnifier меняет подробность всей карты и не является частью XML стиля.

`cyclorouteBikeWidth` — строковое свойство, объявленное в родительском стиле и наследуемое дочерним. Пустое значение соответствует «Текущая» (×1), `Толще` — ×1.5, `Максимальная` — ×2. OsmAnd сам добавляет вариант по умолчанию, поэтому `possibleValues` содержит только два остальных значения. Названия значений являются сохраняемыми ключами: не переименовывайте их без миграции. Неизвестное значение использует базовую толщину. Множитель применяется только в `bike_widths`; подложка, где она уже была, равна результату плюс 2. Пороги видимости унаследованных улиц не меняются. Цвета и штрихи запрета, обычные дороги и атрибут `route` не зависят от свойства.

Для повторения проверок правил нужны JDK 21 и распакованный официальный OsmAndMapCreator. Пример для macOS/Linux:

```sh
mkdir -p dist/testclasses
javac -encoding UTF-8 -cp "$MAPCREATOR/OsmAndMapCreator.jar:$MAPCREATOR/lib/*" -d dist/testclasses tests/VerifyStyle.java tests/VerifyMvpStyle.java tests/VerifyWidthModes.java
cp dist/rendering/CycloRoutePersonalBase.render.xml dist/mvp/
java -cp "dist/testclasses:$MAPCREATOR/OsmAndMapCreator.jar:$MAPCREATOR/lib/*" VerifyStyle dist/rendering
java -cp "dist/testclasses:$MAPCREATOR/OsmAndMapCreator.jar:$MAPCREATOR/lib/*" VerifyMvpStyle dist/mvp
java -cp "dist/testclasses:$MAPCREATOR/OsmAndMapCreator.jar:$MAPCREATOR/lib/*" VerifyWidthModes dist/mvp line-widths.json
python3 tests/verify_package.py
```

Перед командами задайте `MAPCREATOR` как путь к распакованному каталогу. `VerifyWidthModes` проверяет все категории при zoom 10–19, день/ночь, сохранность цветов, обводки, обычных дорог, запретов и маршрута. Итоги и оставшиеся ручные проверки — в `VALIDATION.md`.

Код `tests/VerifyStyle.java` проверяет реальные правила OsmAnd, включая цвета, толщины и обводку; журнал содержит 1839 проверок. Для запуска Java-проверок нужны Java 21, OsmAndMapCreator и его зависимости. Поскольку этот тест ожидает основной стиль `CycloRoutePersonal`, указывайте `dist/rendering`; `VerifyMvpStyle` проверяет вариант MVP из `dist/mvp` вместе с базовым XML. Тесты не заменяют проверку нативного Android-рендерера. В частности, `pathEffect` активного маршрута нативно не дал ожидаемых разрывов: в текущем варианте используется тонкая сплошная линия.

Лицензии и исходные коммиты: `NOTICE.md`. Исходники не требуют доступа к телефону, аккаунту или сохранённым картам пользователя.
