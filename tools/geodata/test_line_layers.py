# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Unit tests of the map line-layer helpers (T-1301): python3 -m unittest discover -s tools/geodata -p 'test_*.py'."""
import math
import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import line_layers  # noqa: E402

# Counterclockwise outer ring and a clockwise hole, in degrees.
SQUARE = [(0.0, 0.0), (2.0, 0.0), (2.0, 2.0), (0.0, 2.0), (0.0, 0.0)]
HOLE = [(0.5, 0.5), (0.5, 1.5), (1.5, 1.5), (1.5, 0.5), (0.5, 0.5)]
CELL = [(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0), (0.0, 0.0)]


class InsideTest(unittest.TestCase):
    def test_points_inside_outside_and_in_a_hole(self):
        self.assertTrue(line_layers.inside(0.2, 0.2, [SQUARE]))
        self.assertFalse(line_layers.inside(2.5, 1.0, [SQUARE]))
        self.assertFalse(line_layers.inside(1.0, -0.1, [SQUARE]))
        self.assertFalse(line_layers.inside(1.0, 1.0, [SQUARE, HOLE]))
        self.assertTrue(line_layers.inside(0.2, 1.0, [SQUARE, HOLE]))


class AreaTest(unittest.TestCase):
    def test_a_one_degree_cell_at_the_equator_in_either_orientation(self):
        # Exact spherical area of a cell between meridians and parallels: R² · Δλ · (sin φ₂ − sin φ₁).
        expected = line_layers.EARTH_RADIUS_KM**2 * math.radians(1) * math.sin(math.radians(1))
        self.assertAlmostEqual(line_layers.polygon_area_km2([CELL]), expected, delta=expected * 1e-9)
        self.assertAlmostEqual(line_layers.polygon_area_km2([list(reversed(CELL))]), expected, delta=expected * 1e-9)

    def test_a_hole_is_subtracted(self):
        whole = line_layers.polygon_area_km2([SQUARE])
        hole = line_layers.polygon_area_km2([HOLE])
        self.assertAlmostEqual(line_layers.polygon_area_km2([SQUARE, HOLE]), whole - hole, delta=whole * 1e-9)

    def test_a_ring_along_a_parallel_encloses_the_sphere_south_of_it(self):
        # Walking east along 60° N, the formula measures the region to the south: the sphere minus the polar cap.
        ring = [(float(lon), 60.0) for lon in range(0, 361)]
        sphere = 4 * math.pi * line_layers.EARTH_RADIUS_KM**2
        cap = 2 * math.pi * line_layers.EARTH_RADIUS_KM**2 * (1 - math.sin(math.radians(60)))
        self.assertAlmostEqual(line_layers.polygon_area_km2([ring]), sphere - cap, delta=sphere * 1e-9)


class CentroidTest(unittest.TestCase):
    def test_square_and_degenerate_rings(self):
        self.assertEqual(line_layers.centroid(SQUARE), (1.0, 1.0))
        self.assertEqual(line_layers.centroid([(3.0, 4.0), (3.0, 4.0), (3.0, 4.0)]), (3.0, 4.0))


class PairSegmentsTest(unittest.TestCase):
    def test_shared_edges_are_grouped_by_the_pair_of_keys(self):
        left = [(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0), (0.0, 0.0)]
        right = [(1.0, 0.0), (2.0, 0.0), (2.0, 1.0), (1.0, 1.0), (1.0, 0.0)]
        groups, single, points = line_layers.pair_segments([(7, [right]), (3, [left])])
        self.assertEqual(list(groups), [(3, 7)])
        self.assertEqual(single, 6)
        lines = line_layers.chains(groups[(3, 7)])
        self.assertEqual(len(lines), 1)
        self.assertEqual(sorted(points[key] for key in lines[0]), [(1.0, 0.0), (1.0, 1.0)])

    def test_edges_between_polygons_of_one_key_are_dropped(self):
        left = [(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0), (0.0, 0.0)]
        right = [(1.0, 0.0), (2.0, 0.0), (2.0, 1.0), (1.0, 1.0), (1.0, 0.0)]
        groups, _, _ = line_layers.pair_segments([(5, [left]), (5, [right])])
        self.assertEqual(groups, {})


class EncodeTest(unittest.TestCase):
    def test_fields_hundredths_and_antimeridian_split(self):
        keys = [(179.0, 1.0), (179.5, 2.0), (-179.5, 1.0), (-179.0, 2.0)]
        points = {key: key for key in keys}
        lines = line_layers.encode_lines("Z 0 1", [keys], points, 0.0)
        self.assertEqual(lines, ["Z 0 1 17900,100 50,100", "Z 0 1 -17950,100 50,100"])


class DeltaPairsTest(unittest.TestCase):
    def test_first_pair_is_absolute_and_the_rest_are_steps(self):
        self.assertEqual(line_layers.delta_pairs(["100,200", "150,190", "150,190"]), ["100,200", "50,-10", "0,0"])

    def test_a_single_pair_is_unchanged(self):
        self.assertEqual(line_layers.delta_pairs(["-17950,-100"]), ["-17950,-100"])

    def test_the_steps_add_back_up_to_the_positions(self):
        pairs = ["1,2", "-3,4", "5,-6", "0,0"]
        encoded = line_layers.delta_pairs(pairs)
        lon = lat = 0
        restored = []
        for step in encoded:
            dx, dy = (int(value) for value in step.split(","))
            lon, lat = lon + dx, lat + dy
            restored.append(f"{lon},{lat}")
        self.assertEqual(restored, pairs)


if __name__ == "__main__":
    unittest.main()
