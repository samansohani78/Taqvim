# ADR-0024: The world map's globe is an orthographic projection drawn on the Compose canvas

- **Status:** Accepted
- **Date:** 2026-09-15
- **Plan reference:** docs/PLAN.md T-1301 ("3D globe (OpenGL ES 3 / AGSL on 13+)"; layers day/night, moon and crescent
  visibility, magnetic field, grid, location, direct path, Qibla), §9 budgets (APK size, frame time), ADR-0003
  (permissive dependencies only)

## Context

The plan names OpenGL ES 3, or AGSL on Android 13+, for the map's 3D globe. What the globe must do for users is:

- show the Earth as a sphere and turn it by dragging, with pinch zoom;
- show the same layers as the flat map, above all day and night shading, on the sphere;
- show and pick markers (the chosen place, the Sun and Moon sub-points, city markers, a picked point) and draw the
  Qibla and direct-path great circles;
- remain accessible (TalkBack actions) and testable with the rest of the map (Robolectric, Roborazzi screenshots).

An OpenGL ES renderer (`GLSurfaceView`) would need its own shaders, a tessellated land mesh built from the Natural
Earth outline, texture uploads of every layer grid, text rendering for city labels and a separate touch and
accessibility layer. It cannot be captured by the JVM screenshot tests, and it would be the only non-Compose rendering
path in the app. AGSL runtime shaders run only on Android 13+, so a second renderer would still be needed below that.

## Decision

The globe is drawn on the same Compose `Canvas` as the flat map, with the orthographic projection (the sphere seen from
far away):

- `Orthographic` projects a latitude and longitude to the unit disk and reports whether it is on the near hemisphere;
  its inverse picks the place under a tap. `GlobeView` holds the center place and zoom; a drag turns the globe so the
  surface follows the finger, and the center latitude is kept between the poles.
- Land rings and shaded grid cells that cross the horizon have their far-side points moved radially onto the rim, so
  fills end at the globe's edge; rings wholly on the far side, and cells with fewer than two corners on the near side,
  are skipped. Lines (borders, graticule, Qibla, direct path) break at the horizon, and markers on the far side are
  hidden.
- Every shaded layer (day/night and twilight, Moon, crescent under Yallop or Odeh, magnetic declination, inclination
  and strength) is drawn from the same `ShadeGrid` as on the flat map. Cells of one color are gathered into one path,
  so a layer costs one draw call per legend color instead of one per cell.
- The chip row switches between the flat map and the globe. The globe first opens centered on the chosen place. Taps,
  the accessibility actions (zoom, pick the center) and city marker hits work the same way in both projections.

No dependency is added and no OpenGL or AGSL code is written.

## Consequences

- The plan's globe features are covered on every supported Android version, with a single drawing path that the
  existing screenshot, UI and property tests exercise (`MapGlobeTest`: points behind the horizon are hidden,
  projection and screen round trips, turning and zoom limits).
- There is no lighting, relief or texture: the sphere is flat-shaded, with the day/night terminator and twilight bands
  as the only shading. Along the rim, shading ends in a slightly ragged edge of pinned or skipped cells.
- Per frame, the one-degree day/night grid projects about 16 500 cell corners on the CPU. If the T-1801 benchmarks show
  frames over budget on the low-end reference device, the fallback is a coarser grid while dragging. A GPU renderer
  would need a new ADR.
