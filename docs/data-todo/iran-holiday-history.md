# generated-by: IranOfficialHolidayHistoryTest (:tools:dataset)
# Iran official holidays 1381–1405 against the dataset rules

Every official calendar of Iran that the importer can read (docs/sources/iran/MANIFEST.md) compared with the
records of `dataset/iran/iran-official-holidays.json`. Read with the lunar dates each calendar prints, the
records give exactly its holidays in every year. Read with the computed Iranian lunar calendar (the app's
default, ADR-0027), a holiday moves with its month wherever that calendar starts the month on another day
than the official one; in 1381 and 1383–1385 the printed calendar was the Calendar Center's prediction,
and an announcement later moved one month (the notice on page 1 of each), so those holidays moved too.
Written by `IranOfficialHolidayHistoryTest`; run `./gradlew :tools:dataset:test
-Ptaqvim.updateSnapshots=true` to refresh this page.

| Year | Official holidays | Printed lunar dates: missing / extra | Left out by `validity` | Computed calendar: lunar-calendar / announced shift |
|---|---|---|---|---|
| 1381 | 26 | 0 / 0 | `imam-hasan-askari-martyrdom` 02-31, `eid-al-fitr-holiday` 09-16 | 0 / 0 |
| 1382 | 25 | 0 / 0 | `imam-hasan-askari-martyrdom` 02-20, `eid-al-fitr-holiday` 09-06 | 8 / 0 |
| 1383 | 25 | 0 / 0 | `imam-hasan-askari-martyrdom` 02-09, `eid-al-fitr-holiday` 08-26 | 4 / 4 |
| 1384 | 25 | 0 / 0 | `imam-hasan-askari-martyrdom` 01-28, `eid-al-fitr-holiday` 08-14 | 0 / 2 |
| 1385 | 26 | 0 / 0 | `imam-hasan-askari-martyrdom` 01-18, `eid-al-fitr-holiday` 08-04 | 0 / 4 |
| 1386 | 25 | 0 / 0 | `imam-hasan-askari-martyrdom` 01-08, `eid-al-fitr-holiday` 07-22, `imam-hasan-askari-martyrdom` 12-26 | 2 / 0 |
| 1387 | 26 | 0 / 0 | `eid-al-fitr-holiday` 07-11, `imam-hasan-askari-martyrdom` 12-16 | 8 / 0 |
| 1388 | 25 | 0 / 0 | `eid-al-fitr-holiday` 06-30, `imam-hasan-askari-martyrdom` 12-04 | 4 / 0 |
| 1389 | 25 | 0 / 0 | `eid-al-fitr-holiday` 06-20, `imam-hasan-askari-martyrdom` 11-23 | 2 / 0 |
| 1390 | 25 | 0 / 0 | `eid-al-fitr-holiday` 06-10, `imam-hasan-askari-martyrdom` 11-12 | 2 / 0 |
| 1391 | 25 | 0 / 0 | `imam-hasan-askari-martyrdom` 11-01 | 4 / 0 |
| 1392 | 26 | 0 / 0 | `imam-hasan-askari-martyrdom` 10-20 | 4 / 0 |
| 1393 | 26 | 0 / 0 | `imam-hasan-askari-martyrdom` 10-10 | 2 / 0 |
| 1394 | 26 | 0 / 0 | `imam-hasan-askari-martyrdom` 09-29 | 0 / 0 |
| 1395 | 26 | 0 / 0 | `imam-hasan-askari-martyrdom` 09-18 | 0 / 0 |
| 1396 | 26 | 0 / 0 | `imam-hasan-askari-martyrdom` 09-06 | 0 / 0 |
| 1397 | 27 | 0 / 0 | — | 0 / 0 |
| 1398 | 26 | 0 / 0 | — | 0 / 0 |
| 1399 | 27 | 0 / 0 | — | 0 / 0 |
| 1400 | 28 | 0 / 0 | — | 0 / 0 |
| 1401 | 27 | 0 / 0 | — | 0 / 0 |
| 1402 | 27 | 0 / 0 | — | 0 / 0 |
| 1403 | 26 | 0 / 0 | — | 0 / 0 |
| 1404 | 26 | 0 / 0 | — | 0 / 0 |
| 1405 | 26 | 0 / 0 | — | 0 / 0 |

Two holidays were added by law during these years; each record's `validity` starts at the first year the
official calendar marks the day (تعطیل), with that page as its citation:

- `imam-hasan-askari-martyrdom` (8 Rabi al-Awwal): a holiday from 1440 AH (Calendar-1397.pdf page 13). The
  calendars of 1381–1396 print the day without (تعطیل), and every calendar from 1397 on with it.
- `eid-al-fitr-holiday` (2 Shawwal): a holiday from 1433 AH (Calendar-1391.pdf page 8); up to 1390 only
  1 Shawwal is.

## Months an announcement moved

The calendars of 1381 and 1383–1385 were printed before the month began; the notice on their first page
says the official month started a day off the printed one. The holiday lists above keep the printed dates.
The computed calendar is compared here with both.

| Lunar month | Printed first day | Announced first day | Computed first day |
|---|---|---|---|
| 1423-09 | 1381-08-15 | 1381-08-16 | 1381-08-15 |
| 1425-10 | 1383-08-25 | 1383-08-24 | 1383-08-24 |
| 1426-09 | 1384-07-14 | 1384-07-13 | 1384-07-13 |
| 1427-10 | 1385-08-03 | 1385-08-02 | 1385-08-02 |

## Differences with the computed calendar

| Persian date | Record | Official holiday | Printed lunar date | Computed lunar date | Class |
|---|---|---|---|---|---|
| 1382-02-02 | `ir.holiday.arbaeen` | no | 1424-02-19 | 1424-02-20 | lunar-calendar difference |
| 1382-02-03 | `ir.holiday.arbaeen` | yes | 1424-02-20 | 1424-02-21 | lunar-calendar difference |
| 1382-02-10 | `ir.holiday.prophet-demise-imam-hasan-martyrdom` | no | 1424-02-27 | 1424-02-28 | lunar-calendar difference |
| 1382-02-11 | `ir.holiday.prophet-demise-imam-hasan-martyrdom` | yes | 1424-02-28 | 1424-02-29 | lunar-calendar difference |
| 1382-11-12 | `ir.holiday.eid-al-adha` | no | 1424-12-09 | 1424-12-10 | lunar-calendar difference |
| 1382-11-13 | `ir.holiday.eid-al-adha` | yes | 1424-12-10 | 1424-12-11 | lunar-calendar difference |
| 1382-11-20 | `ir.holiday.eid-al-ghadir` | no | 1424-12-17 | 1424-12-18 | lunar-calendar difference |
| 1382-11-21 | `ir.holiday.eid-al-ghadir` | yes | 1424-12-18 | 1424-12-19 | lunar-calendar difference |
| 1383-07-09 | `ir.holiday.imam-mahdi-birth` | no | 1425-08-14 | 1425-08-15 | lunar-calendar difference |
| 1383-07-10 | `ir.holiday.imam-mahdi-birth` | yes | 1425-08-15 | 1425-08-16 | lunar-calendar difference |
| 1383-08-24 | `ir.holiday.eid-al-fitr` | no | 1425-09-30 | 1425-10-01 | announced shift |
| 1383-08-25 | `ir.holiday.eid-al-fitr` | yes | 1425-10-01 | 1425-10-02 | announced shift |
| 1383-09-18 | `ir.holiday.imam-sadiq-martyrdom` | no | 1425-10-24 | 1425-10-25 | announced shift |
| 1383-09-19 | `ir.holiday.imam-sadiq-martyrdom` | yes | 1425-10-25 | 1425-10-26 | announced shift |
| 1383-11-30 | `ir.holiday.tasua` | no | 1426-01-08 | 1426-01-09 | lunar-calendar difference |
| 1383-12-02 | `ir.holiday.ashura` | yes | 1426-01-10 | 1426-01-11 | lunar-calendar difference |
| 1384-08-03 | `ir.holiday.imam-ali-martyrdom` | no | 1426-09-20 | 1426-09-21 | announced shift |
| 1384-08-04 | `ir.holiday.imam-ali-martyrdom` | yes | 1426-09-21 | 1426-09-22 | announced shift |
| 1385-08-02 | `ir.holiday.eid-al-fitr` | no | 1427-09-30 | 1427-10-01 | announced shift |
| 1385-08-03 | `ir.holiday.eid-al-fitr` | yes | 1427-10-01 | 1427-10-02 | announced shift |
| 1385-08-26 | `ir.holiday.imam-sadiq-martyrdom` | no | 1427-10-24 | 1427-10-25 | announced shift |
| 1385-08-27 | `ir.holiday.imam-sadiq-martyrdom` | yes | 1427-10-25 | 1427-10-26 | announced shift |
| 1386-07-11 | `ir.holiday.imam-ali-martyrdom` | yes | 1428-09-21 | 1428-09-20 | lunar-calendar difference |
| 1386-07-12 | `ir.holiday.imam-ali-martyrdom` | no | 1428-09-22 | 1428-09-21 | lunar-calendar difference |
| 1387-04-26 | `ir.holiday.imam-ali-birth` | yes | 1429-07-13 | 1429-07-12 | lunar-calendar difference |
| 1387-04-27 | `ir.holiday.imam-ali-birth` | no | 1429-07-14 | 1429-07-13 | lunar-calendar difference |
| 1387-05-09 | `ir.holiday.mabath` | yes | 1429-07-27 | 1429-07-26 | lunar-calendar difference |
| 1387-05-10 | `ir.holiday.mabath` | no | 1429-07-28 | 1429-07-27 | lunar-calendar difference |
| 1387-07-10 | `ir.holiday.eid-al-fitr` | yes | 1429-10-01 | 1429-09-30 | lunar-calendar difference |
| 1387-07-11 | `ir.holiday.eid-al-fitr` | no | 1429-10-02 | 1429-10-01 | lunar-calendar difference |
| 1387-08-04 | `ir.holiday.imam-sadiq-martyrdom` | yes | 1429-10-25 | 1429-10-24 | lunar-calendar difference |
| 1387-08-05 | `ir.holiday.imam-sadiq-martyrdom` | no | 1429-10-26 | 1429-10-25 | lunar-calendar difference |
| 1388-06-29 | `ir.holiday.eid-al-fitr` | yes | 1430-10-01 | 1430-09-30 | lunar-calendar difference |
| 1388-06-30 | `ir.holiday.eid-al-fitr` | no | 1430-10-02 | 1430-10-01 | lunar-calendar difference |
| 1388-07-22 | `ir.holiday.imam-sadiq-martyrdom` | yes | 1430-10-25 | 1430-10-24 | lunar-calendar difference |
| 1388-07-23 | `ir.holiday.imam-sadiq-martyrdom` | no | 1430-10-26 | 1430-10-25 | lunar-calendar difference |
| 1389-02-27 | `ir.holiday.fatima-martyrdom` | yes | 1431-06-03 | 1431-06-02 | lunar-calendar difference |
| 1389-02-28 | `ir.holiday.fatima-martyrdom` | no | 1431-06-04 | 1431-06-03 | lunar-calendar difference |
| 1390-05-30 | `ir.holiday.imam-ali-martyrdom` | yes | 1432-09-21 | 1432-09-20 | lunar-calendar difference |
| 1390-05-31 | `ir.holiday.imam-ali-martyrdom` | no | 1432-09-22 | 1432-09-21 | lunar-calendar difference |
| 1391-05-29 | `ir.holiday.eid-al-fitr` | yes | 1433-10-01 | 1433-09-30 | lunar-calendar difference |
| 1391-05-31 | `ir.holiday.eid-al-fitr-holiday` | no | 1433-10-03 | 1433-10-02 | lunar-calendar difference |
| 1391-06-22 | `ir.holiday.imam-sadiq-martyrdom` | yes | 1433-10-25 | 1433-10-24 | lunar-calendar difference |
| 1391-06-23 | `ir.holiday.imam-sadiq-martyrdom` | no | 1433-10-26 | 1433-10-25 | lunar-calendar difference |
| 1392-10-02 | `ir.holiday.arbaeen` | yes | 1435-02-20 | 1435-02-19 | lunar-calendar difference |
| 1392-10-03 | `ir.holiday.arbaeen` | no | 1435-02-21 | 1435-02-20 | lunar-calendar difference |
| 1392-10-10 | `ir.holiday.prophet-demise-imam-hasan-martyrdom` | yes | 1435-02-28 | 1435-02-27 | lunar-calendar difference |
| 1392-10-11 | `ir.holiday.prophet-demise-imam-hasan-martyrdom` | no | 1435-02-29 | 1435-02-28 | lunar-calendar difference |
| 1393-03-23 | `ir.holiday.imam-mahdi-birth` | yes | 1435-08-15 | 1435-08-14 | lunar-calendar difference |
| 1393-03-24 | `ir.holiday.imam-mahdi-birth` | no | 1435-08-16 | 1435-08-15 | lunar-calendar difference |
