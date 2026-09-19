# Copyright (c) 2026 Saman Sohani. All Rights Reserved.
# Proprietary and confidential. See the LICENSE file in the repository root.
"""Unit tests of the official-calendar importer (T-104, ADR-0040): python3 -m unittest discover -s tools/sources/iran."""
import datetime
import hashlib
import pathlib
import shutil
import subprocess
import sys
import unittest

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import official_calendar_glyphs as glyphs  # noqa: E402
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


def line(text, y, x0=47.0):
    """One printed occasion line starting at [x0] (the column's left margin, 47, when it fills the column)."""
    return [word(text, x0, 296.0, y)]


def weekday(y):
    return {"y": y, "x0": 505.0, "text": "شنبه"}


def occasion_rows(cells, lines):
    words = [item for printed in lines for item in printed]
    return [" ".join(word["text"] for printed in row for word in printed)
            for row in pdf.occasion_lines(words, cells, [300.0] * len(cells))]


class OccasionLinesTest(unittest.TestCase):
    """Which day a printed occasion line belongs to: the three placements found in the editions."""

    def test_a_top_aligned_cell_keeps_its_wrapped_holiday_line(self):
        # 1384 page 15: the 22 Bahman cell starts on its weekday's line and wraps down past the halfway line.
        cells = [weekday(508.4), weekday(525.9), weekday(555.0)]
        rows = occasion_rows(cells, [line("اسرا", 508.7, 206.0), line("پیروزی", 526.2),
                                     line("(تعطیل)", 540.5, 148.0)])
        self.assertEqual(rows, ["اسرا", "پیروزی (تعطیل)", ""])

    def test_a_centred_cell_reaching_past_the_halfway_line_stays_with_its_day(self):
        # 1404 page 6: 14 Khordad's three lines are centred on it; 15 Khordad has two; 16 Khordad one.
        cells = [weekday(370.5), weekday(397.5), weekday(430.1), weekday(454.9)]
        rows = occasion_rows(cells, [
            line("باقر", 367.4, 150.0), line("خمینی", 384.0), line("رهبری", 399.4), line("1368", 412.3, 250.0),
            line("عرفه", 425.6), line("محیط", 438.4, 240.0), line("قربان", 451.7, 120.0)])
        self.assertEqual(rows, ["باقر", "خمینی رهبری 1368", "عرفه محیط", "قربان"])

    def test_one_line_cells_on_neighbouring_days_are_not_merged(self):
        # 1403 page 7: Tasua, Ashura and the next day each print one short line; a wrapped cell above them fills
        # the column, as every page has one.
        cells = [weekday(622.8), weekday(646.1), weekday(665.4), weekday(683.5)]
        rows = occasion_rows(cells, [line("بهشتی", 616.0), line("سردشت", 629.6, 150.0),
                                     line("تاسوعا", 648.4, 120.0), line("عاشورا", 667.6, 110.0),
                                     line("اسرا", 683.5, 180.0)])
        self.assertEqual(rows, ["بهشتی سردشت", "تاسوعا", "عاشورا", "اسرا"])

    def test_the_last_row_of_a_page_keeps_its_last_line(self):
        # 1403 page 9: the last day's cell is centred on it and its third line, with "(holiday)", is low on the page.
        cells = [weekday(711.5), weekday(730.8), weekday(762.0)]
        rows = occasion_rows(cells, [line("شهریار", 711.7, 82.0), line("رسول", 748.5), line("صادق", 762.8),
                                     line("(تعطیل)", 777.1, 70.0)])
        self.assertEqual(rows, ["شهریار", "", "رسول صادق (تعطیل)"])


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
    def test_editions_with_misnamed_digits_are_read_from_their_glyphs(self):
        self.assertEqual([1395, 1396, 1401, 1402], sources.glyph_years())
        self.assertEqual({}, sources.REJECTED)
        self.assertFalse(set(sources.glyph_years()) & set(sources.readable_years()))
        self.assertEqual(list(range(1381, 1406)), sorted(sources.readable_years() + sources.glyph_years()))

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

    # REVIEW R12: papers are cited, not archived, and the exclusion is an allow-list rather than a list of names.

    @staticmethod
    def data_names():
        return {name for name, row in pdf.manifest_rows().items() if row["Kind"] != "paper"}

    @staticmethod
    def allow_listed():
        lines = (MANIFEST.parent / ".gitignore").read_text(encoding="utf-8").splitlines()
        names = {line[1:].replace("\\", "") for line in lines if line.startswith("!")}
        return names - {".gitignore", "MANIFEST.md"}

    def test_the_gitignore_allow_list_is_exactly_the_data_rows(self):
        self.assertEqual(self.data_names(), self.allow_listed())
        self.assertIn("*", (MANIFEST.parent / ".gitignore").read_text(encoding="utf-8").splitlines())

    @unittest.skipUnless(shutil.which("git"), "needs git")
    def test_papers_and_unclassified_files_are_ignored_and_data_is_not(self):
        def ignored(name):
            result = subprocess.run(["git", "check-ignore", "-q", "--no-index", name], cwd=MANIFEST.parent)
            return result.returncode == 0

        papers = {name for name, row in pdf.manifest_rows().items() if row["Kind"] == "paper"}
        for name in sorted(papers | {"new-paper.pdf", "Unknown1405-notes.pdf"}):
            self.assertTrue(ignored(name), name)
        for name in sorted(self.data_names()):
            self.assertFalse(ignored(name), name)

    @unittest.skipUnless(shutil.which("git"), "needs git")
    def test_every_tracked_source_file_is_classified_as_data(self):
        listed = subprocess.run(
            ["git", "ls-files", "-z", "docs/sources"], cwd=ROOT, capture_output=True, check=True
        ).stdout.decode("utf-8").split("\0")
        tracked = [path for path in listed if path]
        self.assertTrue(tracked)
        rows = pdf.manifest_rows()
        root_manifest = (ROOT / "docs/sources/MANIFEST.md").read_text(encoding="utf-8")
        for path in tracked:
            relative = path.removeprefix("docs/sources/")
            with self.subTest(path=path):
                if relative.startswith("iran/"):
                    name = relative.removeprefix("iran/")
                    if name in (".gitignore", "MANIFEST.md"):
                        continue
                    self.assertIn(name, rows)
                    self.assertNotEqual("paper", rows[name]["Kind"])
                elif relative != "MANIFEST.md":
                    self.assertIn(relative, root_manifest)

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


def features(*values):
    return glyphs.numpy.array(values, dtype=float)


@unittest.skipUnless(glyphs.AVAILABLE, "needs numpy and Pillow")
class TemplatesTest(unittest.TestCase):
    """The acceptance rule of official_calendar_glyphs.Templates, on made-up feature vectors."""

    def setUp(self):
        self.templates = glyphs.Templates([
            ("1", "7", features(0, 0), 1),
            ("1", "8", features(100, 0), 2),
            ("1", "0", features(0, 30), 3),
            ("6", "2", features(5, 5), 1),
        ])

    def test_a_glyph_equal_to_a_template_is_read_as_its_digit(self):
        self.assertEqual("7", self.templates.read("1", features(0, 0)))
        self.assertEqual("2", self.templates.read("6", features(5, 5.5)))

    def test_only_templates_of_the_same_extracted_character_are_candidates(self):
        self.assertIsNone(self.templates.read("3", features(0, 0)))
        self.assertIsNone(self.templates.read("6", features(0, 0)))

    def test_a_glyph_near_no_template_is_not_read(self):
        self.assertIsNone(self.templates.read("1", features(50, 0)))

    def test_a_glyph_near_two_digits_is_not_read(self):
        close = glyphs.Templates(self.templates.samples + [("1", "9", features(0, 10), 4)])
        self.assertIsNone(close.read("1", features(0, 0)))

    def test_the_excluded_page_does_not_vote(self):
        self.assertIsNone(self.templates.read("1", features(0, 0), excluded_page=1))


def cell_word(text, x0, x1):
    return {"x0": x0, "x1": x1, "y": 100.0, "y1": 110.0, "text": text}


class RowTokensTest(unittest.TestCase):
    def test_a_number_keeps_its_words_and_names_stay_text(self):
        inside = [cell_word("6", 500, 505), cell_word('"', 470, 475), cell_word("0", 432, 436),
                  cell_word("01", 437, 441), cell_word("رمضان", 400, 424), cell_word("0443", 378, 396)]
        tokens = glyphs.row_tokens(inside)
        self.assertEqual(["6", '"', "001", "رمضان", "0443"], [text for text, _ in tokens])
        self.assertEqual(["0", "01"], [part["text"] for part in tokens[2][1]])
        self.assertIsNone(tokens[1][1])


@unittest.skipUnless(POPPLER and shutil.which("pdftoppm") and glyphs.AVAILABLE, "needs poppler-utils, numpy, Pillow")
class GlyphLayoutTest(unittest.TestCase):
    """The 1401 edition, whose text layer reads the page title as 0410: its digits come from their glyphs."""

    @classmethod
    def setUpClass(cls):
        path = sources.calendar_path(1401)
        cls.digits = glyphs.DigitReader(path, datetime.date(2022, 3, 21), importer.month_lengths(1401))
        cls.reader = pdf.Reader(path, 1401, words_of=cls.digits.page_words)

    def test_every_labelled_digit_is_read_right_from_the_other_pages(self):
        self.assertEqual(list(range(4, 16)), self.digits.month_pages)
        self.assertEqual(len(self.digits.templates.samples), self.digits.validated)

    def test_the_first_row_of_ordibehesht_is_read_as_printed(self):
        self.assertEqual(31, self.reader.read_page(5))
        row = self.reader.rows[0]
        self.assertEqual("1401-02-01", row["persian"])
        self.assertEqual(4, row["weekday_iso"])
        self.assertEqual((1443, 9, 19), (row["hijri_year"], row["hijri_month"], row["hijri_day"]))
        self.assertEqual(datetime.date(2022, 4, 21), row["gregorian"])

    def test_occasion_digits_in_the_table_font_are_read_too(self):
        reader = pdf.Reader(sources.calendar_path(1401), 1401, words_of=self.digits.page_words)
        self.assertEqual(31, reader.read_page(4))
        self.assertIn("(1342 هو ش)", reader.rows[1]["occasion"])

    def test_the_text_layer_alone_misreads_the_same_row(self):
        words = [word for word in pdf.page_words(sources.calendar_path(1401), 5) if word["text"].isdigit()]
        self.assertIn("0443", [word["text"] for word in words])


@unittest.skipUnless(POPPLER and shutil.which("pdftoppm") and glyphs.AVAILABLE, "needs poppler-utils, numpy, Pillow")
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
