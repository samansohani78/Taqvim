# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Unit tests of the official-calendar importer (T-104, ADR-0040): python3 -m unittest discover -s tools/iran."""
import datetime
import pathlib
import shutil
import subprocess
import sys
import unittest

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import official_calendar_import as importer  # noqa: E402
import official_calendar_pdf as pdf  # noqa: E402


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


@unittest.skipUnless(shutil.which("pdftotext") and shutil.which("pdfinfo"), "needs poppler-utils")
class StoredCalendarsTest(unittest.TestCase):
    def test_fixtures_are_up_to_date_with_the_stored_calendars(self):
        result = subprocess.run([sys.executable, str(HERE / "official_calendar_import.py"), "--check"],
                                capture_output=True, text=True, check=False)
        self.assertEqual(0, result.returncode, result.stderr + result.stdout)
        self.assertIn("up to date", result.stdout)


if __name__ == "__main__":
    unittest.main()
