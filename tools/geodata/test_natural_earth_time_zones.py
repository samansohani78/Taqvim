# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Tests of the mixed-band detection in natural_earth_time_zones.py (review I09)."""
import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import natural_earth_time_zones as ntz  # noqa: E402

CAIRO = (31.24, 30.05, "Africa/Cairo", 20_000_000)
ISTANBUL = (28.98, 41.01, "Europe/Istanbul", 15_000_000)
ALEXANDRIA = (29.92, 31.20, "Africa/Cairo", 5_000_000)
# A catalog row with a wrong zone id: a city near Cairo tagged with Chicago's zone.
MISLABELLED = (31.30, 30.10, "America/Chicago", 1_000_000)


class Iso6709Test(unittest.TestCase):
    def test_degrees_minutes_and_seconds(self):
        self.assertEqual(ntz.iso6709("+3030+03115"), (30.5, 31.25))
        lat, lon = ntz.iso6709("-3352+15113")
        self.assertAlmostEqual(lat, -33.8667, places=3)
        self.assertAlmostEqual(lon, 151.2167, places=3)
        lat, lon = ntz.iso6709("+404251-0740023")
        self.assertAlmostEqual(lat, 40.7142, places=3)
        self.assertAlmostEqual(lon, -74.0064, places=3)


class ConflictTest(unittest.TestCase):
    def setUp(self):
        self.locations = ntz.zone_locations()

    def test_reference_locations_come_from_the_tz_database(self):
        self.assertIn("Africa/Cairo", self.locations)
        self.assertIn("Europe/Istanbul", self.locations)

    def test_distance(self):
        self.assertAlmostEqual(ntz.distance_km(0, 0, 0, 1), 111.19, places=1)
        self.assertEqual(ntz.distance_km(10, 20, 10, 20), 0)

    def test_offsets_include_daylight_saving(self):
        self.assertEqual(ntz.offsets("Asia/Tehran"), (210, 210))
        self.assertEqual(ntz.offsets("Europe/Berlin"), (60, 120))

    def test_a_band_with_cairo_and_istanbul_is_mixed(self):
        cities = [CAIRO, ISTANBUL, ALEXANDRIA]
        self.assertEqual(ntz.conflicting_zones("Africa/Cairo", cities, self.locations), ["Europe/Istanbul"])

    def test_same_offsets_and_wrong_zone_ids_do_not_count(self):
        self.assertFalse(ntz.plausible(MISLABELLED, self.locations))
        cities = [CAIRO, ALEXANDRIA, MISLABELLED]
        self.assertEqual(ntz.conflicting_zones("Africa/Cairo", cities, self.locations), [])

    def test_a_band_without_a_zone_is_never_mixed(self):
        self.assertEqual(ntz.conflicting_zones(None, [CAIRO, ISTANBUL], self.locations), [])

    def test_zones_are_listed_once_most_populous_first(self):
        amman = (35.93, 31.95, "Asia/Amman", 4_000_000)
        bursa = (29.06, 40.19, "Europe/Istanbul", 2_000_000)
        cities = [CAIRO, ISTANBUL, amman, bursa]
        self.assertEqual(
            ntz.conflicting_zones("Africa/Cairo", cities, self.locations), ["Europe/Istanbul", "Asia/Amman"]
        )


if __name__ == "__main__":
    unittest.main()
