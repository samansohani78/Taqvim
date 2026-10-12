# ADR-0055: Taqvim does not ask where the device is

- **Status:** Accepted
- **Date:** 2026-10-13
- **Plan reference:** docs/PLAN.md T-603 (bundled city catalog), T-1502 (location settings), T-1804, T-1503;
  ADR-0017 (manifest audit), ADR-0051 (the device calendar), ADR-0054 (no network access)

## Context

Taqvim computes prayer times, Qibla, sunrise and sunset, the sky and the crescent at a place. A place is
coordinates and a time zone — nothing more. There were three ways to get one: search the bundled catalog of 7 342
cities, pick a point on the world map, or type coordinates. A fourth asked the device: `DeviceLocator` took a
one-shot fix from the platform `LocationManager` behind `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`, and
`PlatformGeocoder` then turned the result into a name.

The feature was polite about it. The permission was requested only when the user tapped "Use my location", the app
worked fully without it, and the prompt never appeared on first launch. But the permission was still in the
manifest, which means it was in the install-time permission list, in the system's per-app permission screen, and in
every automated privacy report of the app. A calendar that *can* ask where you are is a different object from one
that cannot, regardless of when it chooses to ask.

What it bought was small: one tap instead of typing a city name, once, when setting the app up. Prayer times do not
follow a user around — they are computed for the place they chose, and nothing re-reads the position afterwards.

The geocoder was worse in a quieter way. `android.location.Geocoder` needs no permission, so nothing in the
manifest hinted at it, and on most devices it resolves over the network. Taqvim used it to put a name on
coordinates the user had typed or tapped — which meant those coordinates left the device to be labelled.

## Decision

**Taqvim requests no location permission and reads no device position.** `ACCESS_FINE_LOCATION` and
`ACCESS_COARSE_LOCATION` are removed from every manifest, `DeviceLocator` and the "use my location" page are
deleted, and no module may declare either permission again.

**The platform geocoder is removed too.** Coordinates are named by the nearest city in the bundled catalog, in the
app's language, offline. A point far from any city is simply unnamed, which is a better answer than a name obtained
by sending the coordinates somewhere.

A place remains what it always was: a catalog city, a point on the map, or typed coordinates with a time zone.

## Consequences

**Users who had it.** A place already stored as the device position keeps working exactly as before. The
coordinates, name and time zone were all stored with it, so the only thing that changes is the label for how it was
first obtained: `PLACE_SOURCE_DEVICE` reads back as `COORDINATES`. Nothing is lost and nothing needs re-entering.
An existing permission grant simply stops being used; Android leaves it until the user revokes it or the app is
reinstalled.

**What disappears from the interface.** The "My location" tab in the location settings with its button and its five
outcome states, the location row on the privacy dashboard, and the location entry in the system's permission screen
for Taqvim. The city tab and the coordinates tab are unchanged, and the map picker still names what you tap.

**What is unaffected.** Prayer times, Qibla, the compass, sunrise and sunset, the sky, the crescent, planetary
hours and every other place-dependent calculation. They need coordinates, which they still have. The compass needs
the magnetometer, not a position.

**It is enforced, not just done.** `SecurityKonsistTest` fails if any source manifest requests either permission,
and separately if any main Kotlin source names `LocationManager`, a fused provider or the platform `Geocoder` —
the second rule exists precisely because the geocoder needs no permission, so the manifest rule alone would not
catch it. `ExportedComponentsTest` refuses both permissions by name on the debug manifest and
`auditReleasePermissions` on the release one. All of it has planted-violation coverage.

**What this does not claim.** It does not mean Taqvim cannot know roughly where you are: you tell it, by choosing a
city or typing coordinates, and it stores that. The claim is narrower and checkable — the app has no permission to
ask the device, and no code that would.

**What it costs.** Setting up the app takes a city search instead of a tap, once. For a user who does not know
their coordinates and whose city is not in a 7 342-entry catalog, the map picker is the answer; it was already
there.
