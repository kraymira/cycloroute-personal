# To-do

Current development backlog for CycloRoute Personal. The items below are not implemented unless checked off.

## Main work

- [ ] **Make infrastructure clearer on the route line.** Investigate colouring the planned and active route by infrastructure type, so riders can distinguish bicycle facilities from ordinary-road connections without inspecting the map beneath a thin line. Treat missing infrastructure tags as unknown rather than proof that no facility exists. Confirm what is possible with stock OsmAnd and BRouter before choosing an implementation.
- [ ] **Check automatic recalculation during a real ride.** Deviate from a route and verify that OsmAnd continues to use `cycleways-preferred` through BRouter, respects access and direction restrictions, and keeps the intended route/map appearance.

## Installation and persistence checks

- [ ] **Test the normal OSF import and update flow.** Exercise the import wizard with the v1.3 package, including replacement of existing resources, the English legend, and duplicate-bookmark handling. The existing v1.3 phone check used direct USB file installation.
- [ ] **Check profile persistence after restart.** Verify the selected map profile, its style and the routing profile after restarting OsmAnd. Make any mismatch clear in the instructions, especially when the app opens in Browse map.
- [ ] **Check width-setting persistence.** Test Standard, Thicker and Maximum after an app restart and after switching profiles. Standard/Maximum switching without a restart has already been visually checked.
- [ ] **Check the English legend at night on the phone.** Confirm label readability and the distinction between the infrastructure categories. Daytime English samples and Java day/night drawing rules have already been checked.

## Optional ideas

- [ ] Show the percentage of the route with mapped bicycle infrastructure, with a clear definition of which facility types count and how unknown data is handled.
- [ ] Show route sections with their infrastructure type and length, linked to their position on the map.
- [ ] Investigate a compact legend over the current map or route, instead of opening the separate offline demonstration map.
