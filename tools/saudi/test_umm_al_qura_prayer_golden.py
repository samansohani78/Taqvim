# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Unit tests of the Umm al-Qura prayer-table tools (T-601, DT-011):

    python3 -m unittest discover -s tools/saudi
"""
import pathlib
import sys
import unittest

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import umm_al_qura_prayer_golden as golden  # noqa: E402
import umm_al_qura_prayer_tables as tables  # noqa: E402

# The front page's shape, cut down to two cities: the Hijri date, the Gregorian date, the column headers and rows.
PAGE = """<html><body><table>
<tr><td>1</td><td>محرم</td><td>(</td><td>1</td><td>)</td><td>Muharram</td><td>1431</td><td>&#1607;&#1600;</td></tr>
<tr><td>{day}</td><td>{arabic}</td><td>{english}</td><td>(</td><td>{number}</td><td>)</td><td>{year}</td></tr>
<tr><td>المدينة</td><td>الفجر</td><td>الشروق</td><td>الظهر</td><td>العصر</td><td>المغرب</td><td>العشاء</td></tr>
{rows}
</table></body></html>"""
ROW = "<tr><td>{city}</td><td>{times}</td></tr>"


def page(day="18", arabic="ديسمبر - كانون الأول", english="December", number="12", year="2009", rows=None):
    body = "\n".join(
        ROW.format(city=city, times="</td><td>".join(times))
        for city, times in (rows if rows is not None else [(c, ["5:31", "6:52", "12:18", "3:22", "5:43", "7:13"])
                                                           for c in tables.CITIES])
    )
    return PAGE.format(day=day, arabic=arabic, english=english, number=number, year=year, rows=body)


class GregorianDateTest(unittest.TestCase):
    def test_december_is_read_from_the_number_not_the_misprinted_name(self):
        # The site prints months[number % 12], so December's name comes out as January; its number is right.
        parsed = tables.parse(page(english="January", number="12"))
        self.assertEqual("2009-12-18", parsed["date"])

    def test_an_ordinary_month(self):
        self.assertEqual(
            "2010-08-19",
            tables.parse(page(day="19", arabic="أغسطس - آب", english="August", number="8", year="2010"))["date"],
        )

    def test_a_page_without_a_timetable(self):
        self.assertIsNone(tables.parse("<html><body>no table here</body></html>"))

    def test_a_page_missing_a_city_is_rejected(self):
        rows = [(city, ["5:31", "6:52", "12:18", "3:22", "5:43", "7:13"]) for city in tables.CITIES[:-1]]
        self.assertIsNone(tables.parse(page(rows=rows)))


class CaptureDayTest(unittest.TestCase):
    def test_the_capture_day_itself(self):
        self.assertTrue(tables.shows_its_own_capture_day("2010-04-05", "20100405121124"))

    def test_a_late_capture_may_already_show_the_next_day(self):
        self.assertTrue(tables.shows_its_own_capture_day("2010-04-06", "20100405213000"))

    def test_the_next_day_across_a_month_and_a_year_boundary(self):
        # 21:37 UTC on 31 May is 00:37 on 1 June in Riyadh, and 22:00 UTC on 31 December is already January.
        self.assertTrue(tables.shows_its_own_capture_day("2023-06-01", "20230531213735"))
        self.assertTrue(tables.shows_its_own_capture_day("2011-01-01", "20101231220000"))

    def test_a_replayed_page_is_rejected(self):
        self.assertFalse(tables.shows_its_own_capture_day("2023-04-25", "20230429172224"))
        self.assertFalse(tables.shows_its_own_capture_day("2010-05-05", "20100405121124"))


class ClockTest(unittest.TestCase):
    def test_morning_times_are_unchanged(self):
        self.assertEqual("05:31", golden.to_24_hour("5:31", afternoon=False))

    def test_afternoon_times_gain_twelve_hours(self):
        self.assertEqual("15:22", golden.to_24_hour("3:22", afternoon=True))

    def test_noon_is_already_noon(self):
        self.assertEqual("12:18", golden.to_24_hour("12:18", afternoon=False))
        self.assertEqual("12:41", golden.to_24_hour("12:41", afternoon=True))

    def test_a_time_the_clock_cannot_show(self):
        for value in ("0:10", "13:00", "5:70"):
            with self.assertRaises(ValueError):
                golden.to_24_hour(value, afternoon=False)


class RowsTest(unittest.TestCase):
    def archive(self, times):
        return {
            "columns": ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"],
            "days": {"2010-04-05": {"capture": "20100405121124", "page_sha256": "ab" * 32,
                                    "rows": {city: times for city in golden.CITIES}}},
        }

    def test_a_day_becomes_one_row_per_city(self):
        rows = golden.rows(self.archive(["4:53", "6:10", "12:24", "3:49", "6:37", "8:07"]))
        self.assertEqual(len(golden.CITIES), len(rows))
        self.assertEqual(
            "2010-04-05,Makkah,04:53,06:10,12:24,15:49,18:37,20:07,20100405121124",
            rows[0],
        )

    def test_times_out_of_order_are_refused(self):
        with self.assertRaises(ValueError):
            golden.rows(self.archive(["6:10", "4:53", "12:24", "3:49", "6:37", "8:07"]))


class GoldenTest(unittest.TestCase):
    def test_the_committed_golden_matches_the_archive(self):
        self.assertEqual(golden.GOLDEN.read_text(), golden.text())

    def test_every_golden_row_cites_the_capture_it_came_from(self):
        lines = golden.GOLDEN.read_text().splitlines()
        body = [line for line in lines if not line.startswith("#")][1:]
        self.assertTrue(body)
        for line in body:
            fields = line.split(",")
            self.assertEqual(9, len(fields), line)
            self.assertRegex(fields[8], r"^\d{14}$")


if __name__ == "__main__":
    unittest.main()
