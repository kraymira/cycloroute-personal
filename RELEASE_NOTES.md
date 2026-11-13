# CycloRoute Personal 1.4 Android Preview

The Android preview highlights mapped bicycle infrastructure directly on the route in blue. Other sections remain purple. The route preview and the Route information widget show the distance and percentage on mapped bikeways. The preview also shows how much of the route has unknown infrastructure.

## Installation

1. Install `CycloRoute-Android-preview.apk` on an ARM64 Android phone. It appears as **OsmAnd Nightly** and can run alongside stock OsmAnd.
2. In OsmAnd Nightly, use **Settings → Import from file** to import `CycloRoute-Personal.osf` and then `CycloRoute-Navigation-Profile.osf`. Download the local map in Nightly.
3. Use BRouter with the `cycleways-preferred` profile and RD5 data for the area. Select **Brouter[cycleways-preferred]** in Navigation and calculate a route. During guidance, the **Route information** widget displays the metric.

The [OsmAnd source patch and build notes](routing/osmand/README.md) explain the integration. The APK is distributed as a GitHub release asset.

## Validation

- Galaxy S23, Gandia: blue and purple sections on a 2.67 km route, with 1.67 km of mapped bikeways (62% as displayed). The widget showed the percentage during simulated navigation.
- Galaxy S23, Oliva: a 1.07 km route calculated successfully with 0% mapped bikeways.
- Automatic recalculation after deviating on a real ride remains unverified. One upstream Java search localization test failed in the build workspace. The preview APK was built separately with that test task skipped.

[Route preview](tests/android-route-preview-2026-11-13/gandia-preview.png) · [Simulated navigation](tests/android-route-preview-2026-11-13/gandia-navigation.png) · [0% example](tests/android-route-preview-2026-11-13/oliva-zero.png)
