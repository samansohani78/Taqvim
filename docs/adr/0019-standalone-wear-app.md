# ADR-0019: A standalone Wear OS app

- **Status:** Accepted
- **Date:** 2026-09-14
- **Plan reference:** docs/PLAN.md T-1600 (screens: today, month, converter, settings on DataStore; tiles: month, next
  event/prayer; complications: date, month progress, next prayer), §2 (Wear: date app, month tile, complications), §3.1
  (`:wear`)

## Context

A watch app can get its settings from the phone through the Wearable Data Layer (`com.google.android.gms:
play-services-wearable`) or keep its own. The Data Layer is a Google Play services library published under the Android
Software Development Kit License, which is not on the ADR-0003 allow-list (Apache-2.0, MIT, BSD, ISC, Unicode, CC0 or
public domain). Everything else the watch needs is permissive: Compose for Wear OS, Tiles, ProtoLayout and the
complication data source libraries are Apache-2.0 AndroidX artifacts.

## Decision

1. **Standalone watch.** `:wear` does not use the Data Layer or any Play services library. It keeps its own
   preferences with the same `:data:preferences` DataStore as the phone (language, calendars, prayer method, chosen
   place) and offers its own settings: language, main calendar, prayer method and a city from the bundled catalog
   (`:data:location`; the 80 most populous cities with a time zone). `com.google.android.wearable.standalone` is `true`.
2. **Shared core.** All dates, formatting, official events and prayer times come from the core modules and the bundled
   dataset (`:data:events` generated sources); the watch code only maps them (`WearSetup`, `WearDayCalculator`,
   `WearMonthBuilder`, `WearConverter`) and draws them.
3. **Dependencies (Apache-2.0):** `androidx.wear.compose:compose-material3`, `compose-foundation`, `compose-navigation`
   1.6.2; `androidx.wear.tiles:tiles` 1.6.2 (test: `tiles-testing`); `androidx.wear.protolayout:protolayout` and
   `protolayout-expression` 1.4.2; `androidx.wear.watchface:watchface-complications-data-source-ktx` 1.3.0;
   `androidx.concurrent:concurrent-futures-ktx` 1.3.0. `minSdk` of `:wear` is 30 (Wear OS 3).
4. **No unused permissions.** The libraries' network (`:data:events` subscriptions), device calendar
   (`:data:device-calendar`) and location (`:data:location`) permissions are removed from the watch's merged manifest with
   `tools:node="remove"`. The Konsist network rule ignores removed permissions.
5. **Security baseline (ADR-0017) on the watch.** Exported components are the launcher activity and the tile and
   complication services, each bound only through its system permission (`BIND_TILE_PROVIDER`,
   `BIND_COMPLICATION_PROVIDER`); `wear/src/test/resources/security/wear-exported-components.txt` lists them and
   `WearManifestTest` checks the merged manifest against it. Auto Backup keeps the preferences only when end-to-end
   encrypted.

## Consequences

- Settings chosen on the phone do not reach the watch; the user sets the watch once. Syncing needs a permissively
  licensed transport or a new ADR.
- Personal events, device calendar events and subscriptions are not shown on the watch; only official occasions.
- No GPS place on the watch; a catalog city is chosen instead.
