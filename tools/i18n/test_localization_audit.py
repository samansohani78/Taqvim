"""Regression checks for localization structure, fallback, and honest provenance."""
import json
from pathlib import Path
import unittest
from unittest.mock import patch
import tempfile

import localization_audit as audit


class LocalizationAuditTest(unittest.TestCase):
    def test_format_arguments_can_move_but_not_change_type_or_multiplicity(self):
        self.assertEqual(audit.signature('%1$s: %2$d'), audit.signature('%2$d — %1$s'))
        self.assertNotEqual(audit.signature('%1$s'), audit.signature('%1$d'))
        self.assertNotEqual(audit.signature('%1$s %1$s'), audit.signature('%1$s'))
        self.assertEqual(audit.signature('%% %s %d'), [(1, 's'), (2, 'd')])

    def test_related_languages_and_android_aliases(self):
        titles = dict(fa='original', en='reference', prs='Dari', kmr='Kurmanji', id='Indonesian')
        for requested, expected in [('fa-AF', 'prs'), ('ku', 'kmr'), ('in-ID', 'id'),
                                    ('ckb', 'en'), ('fr-CA', 'en'), ('fa', 'fa')]:
            self.assertEqual(audit.fallback_language(titles, requested), expected)
        self.assertEqual(audit.fallback_language({'fa': 'original', 'en': 'reference'}, 'prs'), 'fa')
        self.assertEqual(audit.fallback_language({'ne': 'original'}, 'en'), 'ne')

    def test_invalid_resources_are_rejected_not_just_counted(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            paths = []
            for qualifier, body in [
                ('values', '<string name="title">%1$s</string><string name="missing">Hello</string>'
                 '<plurals name="count"><item quantity="other">%1$d items</item></plurals>'),
                ('values-fa', '<string name="title">%1$d</string><string name="title">%1$d</string>'
                 '<plurals name="count"><item quantity="one">%1$s</item></plurals>'),
            ]:
                path = Path('app/src/main/res') / qualifier / 'strings.xml'
                (root / path).parent.mkdir(parents=True)
                (root / path).write_text('<resources>' + body + '</resources>')
                paths.append(str(path))
            (root / 'dataset/sample').mkdir(parents=True)
            (root / 'dataset/sample/events.json').write_text(json.dumps({'events': [
                {'id': 'example', 'title': {'fa': 'اصل'}, 'titleReview': []},
            ]}))
            with patch.object(audit, 'ROOT', root), patch.object(audit, 'LOCALES', {'en': 'values', 'fa': 'values-fa'}), \
                    patch.object(audit.subprocess, 'check_output', return_value='\n'.join(paths)):
                errors = '\n'.join(audit.inventory()['errors'])
            for message in ['duplicate title', 'format signature mismatch', 'missing missing',
                            'missing plural other', 'plural one format signature mismatch']:
                self.assertIn(message, errors)

    def test_repository_has_no_structural_errors_or_stale_provenance(self):
        report = audit.inventory()
        self.assertEqual(report['errors'], [])
        for language in ('en', 'fa'):
            self.assertEqual(report['locales'][language]['fallback_languages'], {})
        # Numeral examples may deliberately show another script. Ordinary Kurmanji
        # sentences must not regress to the Sorani translations previously shipped.
        kurmanji = report['locales']['kmr']['arabic_script_candidates']
        self.assertFalse([key for key in kurmanji if 'digit' not in key and 'numeral' not in key], kurmanji)


if __name__ == '__main__':
    unittest.main()
