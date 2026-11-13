# To-do

Current development backlog for CycloRoute Personal. Checked items are available in the Android preview.

## Main work

The [Android source patch](routing/osmand/README.md) implements the first two items. A signed ARM64 preview APK builds successfully. The route preview and simulated navigation were verified on a Galaxy S23. Real-ride checks remain open.

- [x] **Highlight bicycle infrastructure on the navigation line.** Verified in preview and simulated navigation.
- [x] **Show bicycle infrastructure as a share of the route.** Verified in the preview and navigation widget, including a route with 0% coverage.
- [ ] **Check route contrast in night mode and at typical riding zooms.**
- [ ] **Check automatic recalculation during a real ride.** Deviate from a route and verify that OsmAnd continues to use `cycleways-preferred` through BRouter, respects access and direction restrictions, and keeps the intended route/map appearance.

## Installation and persistence checks

- [ ] **Test the normal OSF import and update flow.** Exercise the import wizard with the v1.3 package, including replacement of existing resources, the English legend, and duplicate-bookmark handling. The existing v1.3 phone check used direct USB file installation.
- [ ] **Check profile persistence after restart.** Verify the selected map profile, its style and the routing profile after restarting OsmAnd. Make any mismatch clear in the instructions, especially when the app opens in Browse map.
- [ ] **Check width-setting persistence.** Test Standard, Thicker and Maximum after an app restart and after switching profiles. Standard/Maximum switching without a restart has already been visually checked.
- [ ] **Check the English legend at night on the phone.** Confirm label readability and the distinction between the infrastructure categories. Daytime English samples and Java day/night drawing rules have already been checked.

## Optional ideas

- [ ] Show route sections with their infrastructure type and length, linked to their position on the map.
- [ ] Investigate a compact legend over the current map or route, instead of opening the separate offline demonstration map.
