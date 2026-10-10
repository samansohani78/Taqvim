# Localization debt: dataset event titles (2026-10-10, re-measured after the English exonym pass)

The app ships in 24 languages. Its **interface** is translated into all of them. Its **dataset event titles** — the
names of the 329 official observances — are not, and this is the measured state of that, not a plan to fix it by
machine.

`EventTitle.forLanguage` falls back `requested → fa → ne`. So a reader whose language has no title for a record sees
**Persian text inside an otherwise translated screen**. That is the user-visible shape of this debt.

## Titles present, by language

| Language | Titles | Missing of 329 | What the missing ones show |
|---|---|---|---|
| fa Persian | 299 | 30 | Nepali (records with no Persian title) |
| en English | 280 | 49 | Persian |
| zh Chinese | 99 | 230 | Persian |
| fr French | 99 | 230 | Persian |
| ru Russian | 98 | 231 | Persian |
| ar Arabic | 97 | 232 | Persian |
| es Spanish | 95 | 234 | Persian |
| ne Nepali | 30 | 299 | Persian |
| prs Dari | 10 | 319 | Persian |
| az Azerbaijani | **0** | 329 | Persian |
| ckb Kurdish (Sorani) | **0** | 329 | Persian |
| ku Kurdish (Kurmanji) | **0** | 329 | Persian |
| uz Uzbek | **0** | 329 | Persian |
| bn Bengali | **0** | 329 | Persian |
| de German | **0** | 329 | Persian |
| hi Hindi | **0** | 329 | Persian |
| id Indonesian | **0** | 329 | Persian |
| ja Japanese | **0** | 329 | Persian |
| ms Malay | **0** | 329 | Persian |
| ps Pashto | **0** | 329 | Persian |
| ta Tamil | **0** | 329 | Persian |
| tg Tajik | **0** | 329 | Persian |
| tr Turkish | **0** | 329 | Persian |
| ur Urdu | **0** | 329 | Persian |

**Fifteen of the twenty-four launch languages have no dataset titles at all.**

## What the English pass of 2026-10-10 did and did not do

Eighteen records gained an English title, taking English from 262 to 280 of 329. Every one of them is a
transliterated proper noun with a single dominant English form: Nowruz (4 days), Eid al-Fitr and its following
holiday, Eid al-Adha, Eid al-Ghadir, Tasua, Ashura, Arbaeen, and Afghanistan's Day of Arafah and its three days
each of Eid al-Fitr and Eid al-Adha. All eighteen carry `titleReview: ["en"]`, so none is claimed as reviewed.

What was deliberately left alone, and why — these are the records whose wording is the thing being recorded, so
rendering them is editorial work and not transliteration:

- **The Shia commemorations** (`شهادت حضرت امام رضا علیه السلام` and eleven others). "Martyrdom of Imam Reza" is
  the usual English, but the honorifics are part of the official wording, and deciding whether an English title
  drops them is not a translator's call to make quietly.
- **The political and civic days** (`پیروزی انقلاب اسلامی ایران و سقوط نظام شاهنشاهی`, `قیام خونین ۱۵ خرداد`,
  `روز ملی شدن صنعت نفت ایران`, the Soviet withdrawal). Each is a sentence, not a name, and the phrasing carries
  the claim.
- **`شب یلدا و ترویج فرهنگ میهمانی و پیوند با خویشان`**, whose title is "Yalda Night" plus a clause about
  hospitality that the official listing includes on purpose.
- **All 30 Nepali records**, which have no Persian title either and are cited to Nepali sources: the inverse
  problem, and one only those sources can close.

## Why this is not fixed by translating it

These are not interface strings. They are the names of official observances, each carrying a citation to the
primary source it was taken from, and several are legal or religious terms whose wording is the point:

- **Dari (prs) and Pashto (ps)** are the languages of the Afghan announcements the records cite. A title invented in
  English and rendered back into Pashto is not what the gazette said.
- **Azerbaijani, Kurdish (both scripts), Uzbek, Tajik** name observances that exist in those communities with
  established local wording. A machine rendering of a Persian title is a guess wearing the right alphabet.
- **Nepali** records are cited to Nepali sources and 299 of them have no Nepali title, which is the inverse problem.

Machine-translating 329 × 15 titles would fill the table and destroy the thing the dataset is for: every title would
read as sourced when none of it was. `reviewedBy`, `titleReview` and the `MT: needs review` markers exist precisely
to keep that distinction, and nothing here changes them.

## What is safe to do, and what is not

Safe, record by record:

- a title that the cited source itself prints in that language — copy it, cite it, set `reviewedBy`;
- a proper name with an established exonym in the target language (a country, a month, a well-known festival);
- a correction a native speaker reviews and signs.

Not safe, and deliberately not done here:

- translating a title automatically and marking it reviewed;
- translating from the English title, which is itself often a rendering of the Persian;
- guessing Azerbaijani, Kurdish, Uzbek, Tajik, Pashto or Dari wording. **Flagged for native review.**

## Order of work, if it is picked up

1. **prs (Dari)** and **ps (Pashto)** — the Afghan records cite sources already written in them; this is
   transcription from the citation, not translation.
2. **ne (Nepali)** — same argument for the Nepali records.
3. **ar** — many Islamic observances have an Arabic name that the sources print.
4. **en** — the 67 missing are reachable without a native reviewer for most proper nouns.
5. Everything else — needs a speaker, and should wait for one.
