#!/usr/bin/env python3
"""Inventory UI/data localization without equating coverage with review quality."""
import argparse
import collections
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
LOCALES = dict(zip(
    'en fa prs ps ar ckb kmr az tr ur ne hi ta bn tg uz ru de fr es id ms zh ja'.split(),
    'values values-fa values-fa-rAF values-ps values-ar values-b+ckb values-ku values-b+az+Latn values-tr values-ur values-ne values-hi values-ta values-bn values-tg values-b+uz+Latn values-ru values-de values-fr values-es values-in values-ms values-b+zh+Hans values-ja'.split(),
))
FORMAT = re.compile(r'%%|%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z])')


def signature(text):
    result = []
    sequential = 0
    for match in FORMAT.finditer(text):
        if match[0] == '%%':
            continue
        sequential += 1
        result.append((int(match[1] or sequential), match[2]))
    return sorted(result)


def fallback_language(titles, locale):
    normalized = locale.replace('_', '-').lower()
    language = normalized.split('-')[0]
    if normalized == 'fa-af':
        language = 'prs'
    language = {'ku': 'kmr', 'in': 'id'}.get(language, language)
    related = {'prs': 'fa', 'fa': 'prs'}.get(language)
    return next(k for k in [locale, language, related, 'en', 'fa', 'ne'] if k in titles)


def inventory():
    paths = subprocess.check_output(['git', 'ls-files'], cwd=ROOT, text=True).splitlines()
    folders = collections.defaultdict(dict)
    errors, warnings = [], []
    for path in paths:
        p = Path(path)
        if '/src/main/res/values' not in path or p.suffix != '.xml':
            continue
        if p.parent.name not in LOCALES.values():
            continue
        text = (ROOT / p).read_text()
        for entry in ET.fromstring(text):
            if entry.tag not in ('string', 'plurals', 'string-array'):
                continue
            key = entry.get('name')
            folder = folders[str(p.parent)]
            if key in folder:
                errors.append(f'{path}: duplicate {key}')
            quantities = [c.get('quantity') for c in entry if c.tag == 'item']
            if entry.tag == 'plurals' and len(quantities) != len(set(quantities)):
                errors.append(f'{path}:{key}: duplicate plural quantity')
            folder[key] = {'text': ''.join(entry.itertext()), 'tag': entry.tag,
                           'translatable': entry.get('translatable') != 'false',
                           'items': {c.get('quantity', str(i)): ''.join(c.itertext()) for i, c in enumerate(entry)},
                           'markup': sorted(c.tag for c in entry.iter() if c is not entry and c.tag != 'item'),
                           'mt': 'MT: needs review' in text, 'path': path}
    totals = {}
    for locale, qualifier in LOCALES.items():
        present = expected = mt = 0
        missing, same, leakage = [], [], []
        for folder, base in folders.items():
            if not folder.endswith('/values'):
                continue
            translated = folders.get(folder[:-6] + qualifier, {})
            for key, source in base.items():
                if not source['translatable']:
                    continue
                expected += 1
                item = translated.get(key)
                if item is None:
                    missing.append(f'{folder}:{key}')
                    errors.append(f'{folder}:{locale}: missing {key}')
                    continue
                present += 1
                mt += item['mt']
                label = f"{item['path']}:{key}"
                if item['tag'] != source['tag']:
                    errors.append(label + ': resource type mismatch')
                if item['tag'] == 'string' and item['markup'] != source['markup']:
                    errors.append(label + ': inline markup mismatch')
                if item['text'].count('\\n') != source['text'].count('\\n'):
                    warnings.append(label + ': explicit newline count differs')
                if item['tag'] == 'string' and signature(item['text']) != signature(source['text']):
                    errors.append(label + ': format signature mismatch')
                if item['tag'] == 'plurals' and 'other' not in item['items']:
                    errors.append(label + ': missing plural other')
                if item['tag'] == 'plurals':
                    for quantity, value in item['items'].items():
                        reference = source['items'].get(quantity, source['items'].get('other', ''))
                        if signature(value) != signature(reference):
                            errors.append(label + f': plural {quantity} format signature mismatch')
                if item['tag'] == 'string-array' and len(item['items']) != len(source['items']):
                    errors.append(label + ': array length mismatch')
                if item['tag'] == 'string-array':
                    for index, value in item['items'].items():
                        if signature(value) != signature(source['items'].get(index, '')):
                            errors.append(label + f': array {index} format signature mismatch')
                if '\\u200c' in item['text']:
                    errors.append(label + ': literal Unicode escape')
                if locale != 'en' and item['text'] == source['text'] and re.search('[A-Za-z]{4}', item['text']):
                    same.append(key)
                if locale not in ('fa', 'prs', 'ps', 'ar', 'ckb', 'ur') and re.search('[\u0600-\u06ff]', item['text']):
                    leakage.append(key)
            errors.extend(f'{folder}:{locale}: extra {key}' for key in translated.keys() - base.keys())
        totals[locale] = {'ui_keys': present, 'ui_expected': expected, 'ui_percent': round(100 * present / expected, 2),
                          'ui_missing': missing, 'ui_mt_entries': mt, 'same_as_english_candidates': same,
                          'arabic_script_candidates': leakage}
    events = []
    datasets = {}
    event_datasets = {}
    for path in sorted((ROOT / 'dataset').glob('*/*.json')):
        rows = json.loads(path.read_text()).get('events', [])
        if rows:
            datasets[str(path.relative_to(ROOT))] = {l: sum(l in e['title'] for e in rows) for l in LOCALES}
            datasets[str(path.relative_to(ROOT))]['total'] = len(rows)
            events.extend(rows)
            event_datasets.update({e['id']: str(path.relative_to(ROOT)) for e in rows})
    provenance_path = ROOT / 'config/i18n/title-provenance.json'
    provenance = json.loads(provenance_path.read_text())['titles'] if provenance_path.exists() else {}
    for locale, total in totals.items():
        states = collections.Counter()
        fallback = collections.Counter()
        for event in events:
            titles = event['title']
            if locale not in titles:
                fallback[fallback_language(titles, locale)] += 1
            else:
                info = provenance.get(event['id'], {}).get(locale, {})
                states[info.get('origin', 'legacy-unclassified')] += 1
                if info.get('aiReviewed'):
                    states['ai-reviewed'] += 1
                if locale in event.get('titleReview', []) or info.get('needsNativeReview'):
                    states['needs-native-review'] += 1
                if info.get('humanReviewedBy'):
                    states['human-reviewed'] += 1
        count = sum(locale in e['title'] for e in events)
        total.update(dataset_titles=count, dataset_total=len(events), dataset_percent=round(count * 100 / len(events), 2),
                     fallback_languages=dict(fallback), provenance=dict(states))
    by_id = {e['id']: e for e in events}
    for event_id, translations in provenance.items():
        for locale, entry in translations.items():
            actual = by_id.get(event_id, {}).get('title', {}).get(locale)
            if entry.get('text') != actual:
                errors.append(f'{event_id}:{locale}: stale provenance text')
            if entry.get('origin') not in ('source-original', 'source-verified', 'ai-generated',
                                          'human-translated', 'legacy-unclassified'):
                errors.append(f'{event_id}:{locale}: unknown origin')
            if entry.get('origin') == 'ai-generated' and locale not in by_id.get(event_id, {}).get('titleReview', []):
                errors.append(f'{event_id}:{locale}: AI title missing review marker')
            if entry.get('origin') == 'source-verified' and not entry.get('sources'):
                errors.append(f'{event_id}:{locale}: verified title missing source')
    return {'locales': totals, 'datasets': datasets, 'errors': errors, 'warnings': warnings,
            'events': [{'id': e['id'], 'titles': e['title'],
                        'dataset': event_datasets[e['id']],
                        'canonicalLanguages': [language for language, info in provenance.get(e['id'], {}).items()
                                               if info.get('origin') == 'source-original'],
                        'missingLanguages': [l for l in LOCALES if l not in e['title']],
                        'titleReview': e.get('titleReview', []),
                        'reviewedBy': e.get('reviewedBy')} for e in events]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    report = inventory()
    text = json.dumps(report, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.write_text(text)
    else:
        print(text, end='')
    raise SystemExit(bool(report['errors']))


if __name__ == '__main__':
    main()
