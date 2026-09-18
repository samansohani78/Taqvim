# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Unit tests of the official-calendar importer (T-104, ADR-0040): python3 -m unittest discover -s tools/iran."""
import datetime
import hashlib
import pathlib
import shutil
import subprocess
import sys
import unittest

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import official_calendar_import as importer  # noqa: E402
import official_calendar_pdf as pdf  # noqa: E402
import official_calendar_sources as sources  # noqa: E402


def lunar(year, month, day):
    return {"hijri_year": year, "hijri_month": month, "hijri_day": day}


class FollowsTest(unittest.TestCase):
    def test_next_day_in_the_same_month(self):
        self.assertTrue(importer.follows(lunar(1447, 3, 5), lunar(1447, 3, 6)))

    def test_new_month_only_after_day_29_or_30(self):
        self.assertTrue(importer.follows(lunar(1447, 3, 29), lunar(1447, 4, 1)))
        self.assertTrue(importer.follows(lunar(1447, 3, 30), lunar(1447, 4, 1)))
        self.assertFalse(importer.follows(lunar(1447, 3, 28), lunar(1447, 4, 1)))

    def test_new_year_after_dhu_al_hijja(self):
        self.assertTrue(importer.follows(lunar(1447, 12, 30), lunar(1448, 1, 1)))
        self.assertFalse(importer.follows(lunar(1447, 12, 30), lunar(1447, 1, 1)))

    def test_skipped_or_repeated_days_are_rejected(self):
        self.assertFalse(importer.follows(lunar(1447, 3, 5), lunar(1447, 3, 7)))
        self.assertFalse(importer.follows(lunar(1447, 3, 5), lunar(1447, 3, 5)))
        self.assertFalse(importer.follows(lunar(1447, 3, 5), lunar(1447, 4, 6)))


class PersianArithmeticTest(unittest.TestCase):
    def test_month_lengths_follow_the_official_leap_table(self):
        self.assertEqual(31, importer.persian_length(1404, 1))
        self.assertEqual(30, importer.persian_length(1404, 7))
        self.assertEqual(30, importer.persian_length(1403, 12))  # 1403 is a leap year
        self.assertEqual(29, importer.persian_length(1404, 12))

    def test_persian_before_crosses_month_and_year(self):
        self.assertEqual((1404, 1, 1), importer.persian_before((1404, 1, 1), 0))
        self.assertEqual((1403, 12, 30), importer.persian_before((1404, 1, 1), 1))
        self.assertEqual((1404, 1, 31), importer.persian_before((1404, 2, 1), 1))
        self.assertEqual((1403, 12, 1), importer.persian_before((1404, 1, 1), 30))


class NameKeyTest(unittest.TestCase):
    def test_swapped_lam_alef_matches(self):
        self.assertEqual(pdf.name_key("الاول"), pdf.name_key("لااول"))

    def test_arabic_letter_forms_match_persian(self):
        self.assertEqual(pdf.name_key("ربيع"), pdf.name_key("ربیع"))

    def test_unknown_name_fails_loudly(self):
        with self.assertRaises(ValueError):
            pdf.index_of("نامعلوم", pdf.HIJRI_MONTHS, "lunar month")

    def test_every_hijri_month_is_found(self):
        for index, names in enumerate(pdf.HIJRI_MONTH_NAMES, 1):
            first = names if isinstance(names, str) else names[0]
            self.assertEqual(index, pdf.index_of(first, pdf.HIJRI_MONTHS, "lunar month"))


def word(text, x0, x1, y=100.0):
    return {"text": text, "x0": x0, "x1": x1, "y": y}


class CellTest(unittest.TestCase):
    def test_a_number_split_into_words_reads_left_to_right(self):
        groups = pdf.groups_of([word("ژوئن", 330, 341), word("6", 326, 329), word("200", 308, 325)])
        self.assertEqual("ژوئن2006", groups[0]["text"])

    def test_a_day_set_against_its_weekday_stays_in_the_row(self):
        words = [word("سهشنبه", 505, 549), word("1", 495, 498), word("فروردین", 454, 480)]
        cell = pdf.weekday_cells(words)[0]
        self.assertEqual("سهشنبه", cell["text"])
        self.assertLess(words[1]["x0"], cell["x0"])

    def test_brackets_kept_in_visual_shape_are_turned_round(self):
        self.assertEqual("عید نوروز (تعطیل)", pdf.occasion_text(
            [word("عید", 300, 315), word("نوروز", 280, 298), word(")", 276, 279), word("تعطیل", 255, 275),
             word("(", 250, 254)]))


class ErratumTest(unittest.TestCase):
    erratum = pdf.Erratum("hijri_day", (6, 2), (6, 17), 6, 9, "test")

    def test_a_lunar_day_run_is_shifted_where_the_page_prints_it(self):
        self.assertTrue(self.erratum.applies(6, 10))
        self.assertEqual(14, self.erratum.printed(6, 10))
        self.assertEqual(23, self.erratum.value(6, 10))
        self.assertFalse(self.erratum.applies(6, 18))

    def test_every_erratum_names_a_real_edition(self):
        self.assertEqual({1381, 1383}, set(sources.ERRATA))


class SourcesTest(unittest.TestCase):
    def test_rejected_editions_are_listed_with_a_reason_and_never_read(self):
        self.assertEqual([1395, 1396, 1401, 1402], sorted(sources.REJECTED))
        self.assertTrue(all(sources.REJECTED.values()))
        self.assertFalse(set(sources.REJECTED) & set(sources.readable_years()))

    def test_every_year_from_1381_to_1405_has_a_layout(self):
        self.assertEqual(list(range(1381, 1406)), sorted(sources.LAYOUTS))


ROOT = HERE.parents[2]
MANIFEST = ROOT / "docs/sources/iran/MANIFEST.md"


class ManifestTest(unittest.TestCase):
    def test_every_stored_file_has_a_row_with_its_checksum_and_size(self):
        rows = pdf.manifest_rows()
        stored = {path.name for path in MANIFEST.parent.iterdir() if path.name not in ("MANIFEST.md", ".gitignore")}
        self.assertEqual(set(), stored - set(rows))
        for name in sorted(stored):
            data = (MANIFEST.parent / name).read_bytes()
            self.assertEqual(rows[name]["SHA-256"], hashlib.sha256(data).hexdigest(), name)
            self.assertEqual(rows[name]["Bytes"], str(len(data)), name)

    def test_only_papers_may_be_missing_from_the_repository(self):
        for name, row in pdf.manifest_rows().items():
            if not (MANIFEST.parent / name).exists():
                self.assertEqual("paper", row["Kind"], name)
                self.assertIn("not archived", row["Content"], name)

    def test_every_yearly_calendar_is_named_for_its_year(self):
        calendars = {name for name, row in pdf.manifest_rows().items() if row["Kind"] == "yearly-calendar"}
        self.assertEqual({f"Calendar-{year}.pdf" for year in range(1381, 1406)}, calendars)


POPPLER = shutil.which("pdftotext") and shutil.which("pdfinfo")


@unittest.skipUnless(POPPLER, "needs poppler-utils")
class LayoutTest(unittest.TestCase):
    """One page of each readable layout, read the way the importer reads it."""

    def first_row(self, year, page):
        reader = pdf.Reader(sources.calendar_path(year), year, sources.ERRATA.get(year, ()))
        self.assertGreater(reader.read_page(page), 27)
        return reader.rows[0]

    def check(self, row, persian, weekday, hijri, gregorian):
        self.assertEqual(persian, row["persian"])
        self.assertEqual(weekday, row["weekday_iso"])
        self.assertEqual(hijri, (row["hijri_year"], row["hijri_month"], row["hijri_day"]))
        self.assertEqual(datetime.date.fromisoformat(gregorian), row["gregorian"])
        self.assertTrue(row["official_holiday"])

    def test_layout_2002(self):
        self.check(self.first_row(1382, 2), "1382-01-01", 5, (1424, 1, 17), "2003-03-21")

    def test_layout_2008(self):
        self.check(self.first_row(1390, 5), "1390-01-01", 1, (1432, 4, 16), "2011-03-21")

    def test_layout_2025(self):
        self.check(self.first_row(1405, 3), "1405-01-01", 6, (1447, 10, 1), "2026-03-21")

    def test_a_page_outside_the_daily_table_leaves_no_rows(self):
        reader = pdf.Reader(sources.calendar_path(1390), 1390)
        self.assertEqual(0, reader.read_page(4))
        self.assertEqual([], reader.rows)
        self.assertEqual(4, reader.skipped[0][0])

    def test_an_edition_with_unreadable_digits_yields_no_table(self):
        reader = pdf.Reader(sources.calendar_path(1401), 1401)
        self.assertEqual(0, reader.read_page(4))

    def test_the_notices_state_the_announced_months(self):
        path = sources.calendar_path(1383)
        self.assertEqual({10: (-1, 1)}, sources.announcements(path, pdf.page_count(path)))


@unittest.skipUnless(POPPLER, "needs poppler-utils")
class StoredSourcesTest(unittest.TestCase):
    def run_tool(self, name):
        return subprocess.run([sys.executable, str(HERE / name), "--check"], capture_output=True, text=True,
                              check=False)

    def test_calendar_fixtures_are_up_to_date_with_the_stored_calendars(self):
        result = self.run_tool("official_calendar_import.py")
        self.assertEqual(0, result.returncode, result.stderr + result.stdout)
        self.assertIn("up to date", result.stdout)

    def test_nowruz_instants_are_up_to_date_with_the_stored_list(self):
        result = self.run_tool("nowruz_instants_import.py")
        self.assertEqual(0, result.returncode, result.stderr + result.stdout)
        self.assertIn("up to date: 44 Nowruz instants", result.stdout)


class NowruzListTest(unittest.TestCase):
    def test_times_are_read_with_words_and_misspellings(self):
        import nowruz_instants_import as nowruz
        entry = ["1395*", "یک شنبه 1 فروردین 1395", "20 مارس 2016", "ساعت 8 و صفر دقیقه و 12 ثانیه"]
        row = nowruz.parsed(entry)
        self.assertTrue(row["leap"])
        self.assertEqual("2016-03-20T08:00:12+03:30", row["instant"].isoformat())
        dawn = nowruz.parsed(["1402", "سه شنبه 1 فروردین 1402", "21 مارس 2023", "54 دقیقه و 28 ثانیه بامداد"])
        self.assertEqual("2023-03-21T00:54:28+03:30", dawn["instant"].isoformat())

    def test_a_time_without_minutes_is_left_empty(self):
        import nowruz_instants_import as nowruz
        row = nowruz.parsed(["1380", "سه شنبه 30 اسفند 1379", "20 مارس 2001", "ساعت 17 و 40 ثانیه"])
        self.assertIsNone(row["instant"])


if __name__ == "__main__":
    unittest.main()
