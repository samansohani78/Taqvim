# Reviewing Taqvim's data and translations

Machine validation and human attestation are different things, and this file keeps them apart. The validator in
`:tools:dataset` says a record is *well formed*: its rule parses, its citation is an http URL, its `titleReview` tags
name languages the title actually carries. Only a person can say the record is *right* — that this really is the
Persian name of that day, that this date is the one the ministry published.

**Nothing in this repository may mark a record reviewed on a person's behalf.** `reviewedBy` stays `pending`, and a
`titleReview` tag stays in place, until a named reviewer has read the record. The validator enforces the one rule
that protects this: a `reviewedOn` date beside `reviewedBy: pending` is an error (`REVIEW_ATTESTATION`), because a
date without a reviewer claims a review nobody did.

## The queue

```
python3 tools/review/build_review_queue.py dataset docs/data-todo/review-queue.tsv
```

writes one row per thing waiting, most urgent first, with the columns a reviewer needs:

| column | what it is |
|---|---|
| `priority`, `state` | `title-machine-translated` (0), `record-unreviewed` (1), `reviewed` (2) |
| `id` | the dataset record |
| `language` | the language of the title under review, empty for a whole-record row |
| `current_title` | what the app shows today |
| `proposed_title` | empty unless a reviewer or a source has supplied one — the script invents none |
| `source`, `citation_url` | where the record's date and existence come from |
| `reviewed_by`, `reviewed_on` | the attestation, both empty or `pending` until someone makes it |

The queue is generated, never edited by hand. Regenerate it after any dataset change.

## The order, and why

1. **The machine-translated Persian titles** (`titleReview: ["fa"]`, ADR-0042). These are the only strings in the
   dataset that the app presents as Persian without a Persian source behind them, and `fa` is one of the two
   languages the project holds to a reviewed standard. A reviewer checks the title against a Persian publication,
   fixes the wording if needed, and removes `"fa"` from `titleReview`.
2. **The records themselves** (`reviewedBy: pending`). Here a reviewer confirms the date, the rule and the citation —
   that the rule really does reproduce the published dates, and that the citation says what the record claims. On
   success they put their name in `reviewedBy` and the date in `reviewedOn`.
3. **The app's own translations**, language by language, through Weblate (docs/i18n/TRANSLATING.md §2a). A language
   loses its `MT: needs review` marker only when its reviewer has read it end to end.

### Which languages first

`fa` and `prs`, then `en`, then `ar`, `ckb` and `ps`, then the rest by whatever the project's usage shows. The
reason is not quality — the machine translations are structurally sound in every language — but reach: these are the
languages the app was built for, and a wrong holiday name matters most to the people who keep that holiday.

**A release is not blocked by unreviewed languages.** They ship marked, as docs/i18n/TRANSLATING.md §1 already says;
`fa` and `en` are the reviewed pair and the release criterion. Removing a language because nobody has reviewed it
would make the app worse for its speakers, not better.

## What a reviewer changes

- A title: fix `title.<lang>`, remove that tag from `titleReview`.
- A record: set `reviewedBy` to the reviewer's name and `reviewedOn` to the date they read it.
- A language: delete the `MT: needs review` comment from each of its `strings.xml`, and record the sign-off in
  docs/i18n/TRANSLATING.md §1.

Everything else — the rules, the citations, the schema — is the ordinary change process, with its own tests.
