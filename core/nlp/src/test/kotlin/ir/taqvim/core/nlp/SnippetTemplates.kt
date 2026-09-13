/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem.GREGORIAN
import ir.taqvim.core.model.CalendarSystem.ISLAMIC
import ir.taqvim.core.model.CalendarSystem.PERSIAN
import ir.taqvim.core.model.Jdn

/**
 * Everyday texts written for the T-501 corpus (messages, notices, e-mails, receipts); `{d}` is a date slot and `{r}` a
 * range slot. The negative texts are full of numbers (times, prices, phone numbers, versions, addresses) but no date.
 */
internal object SnippetTemplates {
    val FA =
        listOf(
            "جلسه هیئت مدیره روز {d} ساعت 10:30 در اتاق 204 برگزار می‌شود.",
            "مهلت ثبت‌نام تا {d} تمدید شد؛ برای اطلاعات بیشتر با 021-88776655 تماس بگیرید.",
            "قرارداد شماره 4567 {r} اعتبار دارد.",
            "یادآوری: قسط وام شما به مبلغ 12,500,000 ریال در تاریخ {d} سررسید می‌شود.",
            "سفارش شما {d} تحویل شد و کد رهگیری آن 987654321 است.",
            "امتحان ریاضی {d} و امتحان فیزیک {d} برگزار خواهد شد.",
            "پرواز تهران–مشهد مورخ {d} ساعت 21:45 لغو شد.",
            "نمایشگاه کتاب {r} در مصلی برپا می‌شود.",
            "طبق بخشنامه مورخ {d}، ساعت کاری ادارات از 7 تا 13 خواهد بود.",
            "لطفاً ایمیل مورخ {d} را بررسی کنید.",
            "تاریخ تولد: {d} — محل صدور: شیراز",
            "برنامه سفر: حرکت {d}، بازگشت {d}.",
            "اطلاعیه: کلاس‌ها {r} به صورت مجازی است.",
            "آخرین به‌روزرسانی {d} انجام شد (نسخه 3.2).",
        )

    val FA_NONE =
        listOf(
            "ساعت 18:30 جلسه داریم، لطفاً 10 دقیقه زودتر بیایید.",
            "قیمت این کالا 2,450,000 تومان است و 15 درصد تخفیف دارد.",
            "شماره تماس پشتیبانی 0912-345-6789 است.",
            "نسخه 2.4.1 نرم‌افزار منتشر شد.",
            "در این مسابقه 1405 نفر شرکت کردند و 32 نفر برنده شدند.",
            "کد پستی 14356-78912 را وارد کنید.",
            "آدرس سرور 192.168.1.20 است.",
            "امروز هوا آفتابی است و فردا باران می‌بارد.",
            "صفحه 12 از 300 را بخوانید.",
            "بین 10 و 20 نفر در صف بودند.",
            "دمای هوا به 38 درجه رسید و رطوبت 12 درصد بود.",
            "پلاک 45 واحد 3 طبقه 2",
        )

    val EN =
        listOf(
            "The board meeting is scheduled for {d} at 10:30 in room 204.",
            "Registration closes on {d}; call 555-0142 for details.",
            "Your lease is valid {r}.",
            "Invoice #4521 is due {d}. Amount: $1,250.00",
            "Flight TK 879 on {d} was cancelled due to weather.",
            "Exams: math on {d}, physics on {d}.",
            "Order shipped {d}, expected delivery {d}.",
            "The conference runs {r} at the convention center.",
            "Posted {d} by admin — 3 comments",
            "Version 2.4.1 was released on {d}.",
            "Born {d} in Shiraz; passport issued {d}.",
            "Reminder: your subscription renews on {d} for $9.99.",
            "Office closed {r} for maintenance.",
            "Last updated {d} (build 5120).",
        )

    val EN_NONE =
        listOf(
            "Call me at 10:45 tomorrow, or next week if that fails.",
            "The package weighs 2.5 kg and costs $19.99.",
            "Room 1405, floor 12, building 3.",
            "Between 1998 and 2003 about 40 people moved here.",
            "Server 10.0.0.12 responded in 350 ms.",
            "Order #20260913 has shipped.",
            "Score: 3-1 at half time, 4-2 at full time.",
            "Chapter 12 covers pages 300 to 345.",
            "Temperature reached 38 degrees with 12% humidity.",
            "Use version 1.2.3 or later with Java 21.",
            "Top 10 tips: 1 plan, 2 act, 3 review.",
            "The recipe needs 3 eggs, 250 g flour and 2 cups of milk.",
        )
}

/** Date phrases in the forms people write them, with days computed by `:core:calendar`. */
internal object DateRenderers {
    const val VARIANTS = 5

    private fun named(
        kit: CorpusKit,
        language: String,
        date: CalendarDate,
    ): String {
        val spec = if (language == "fa") kit.fa else kit.en
        return "${date.day} ${kit.monthName(spec, date.system, date.month)} ${date.year}"
    }

    private fun two(value: Int): String = value.toString().padStart(2, '0')

    fun faSingle(
        kit: CorpusKit,
        reference: Jdn,
        variant: Int,
    ): Phrase {
        val persian = TextSnippets.near(kit, PERSIAN, reference)
        return when (variant) {
            0 -> {
                Phrase(named(kit, "fa", persian), kit.jdn(persian))
            }

            1 -> {
                Phrase("${persian.year}/${two(persian.month)}/${two(persian.day)}", kit.jdn(persian))
            }

            2 -> {
                TextSnippets
                    .near(
                        kit,
                        ISLAMIC,
                        reference,
                    ).let { Phrase(named(kit, "fa", it), kit.jdn(it)) }
            }

            3 -> {
                TextSnippets.near(kit, GREGORIAN, reference).let { Phrase(named(kit, "fa", it), kit.jdn(it)) }
            }

            4 -> {
                weekday(kit, "fa", named(kit, "fa", persian), kit.jdn(persian), separator = " ")
            }

            else -> {
                yearless(kit, reference)
            }
        }
    }

    fun enSingle(
        kit: CorpusKit,
        reference: Jdn,
        variant: Int,
    ): Phrase {
        val date = TextSnippets.near(kit, GREGORIAN, reference)
        val month = kit.monthName(kit.en, GREGORIAN, date.month)
        return when (variant) {
            0 -> {
                Phrase("$month ${date.day}, ${date.year}", kit.jdn(date))
            }

            1 -> {
                Phrase(named(kit, "en", date), kit.jdn(date))
            }

            2 -> {
                Phrase("${date.year}-${two(date.month)}-${two(date.day)}", kit.jdn(date))
            }

            3 -> {
                TextSnippets.near(kit, PERSIAN, reference).let { Phrase(named(kit, "en", it), kit.jdn(it)) }
            }

            4 -> {
                TextSnippets
                    .near(
                        kit,
                        ISLAMIC,
                        reference,
                    ).let { Phrase(named(kit, "en", it), kit.jdn(it)) }
            }

            else -> {
                weekday(kit, "en", "$month ${date.day}, ${date.year}", kit.jdn(date), separator = ", ")
            }
        }
    }

    fun faRange(
        kit: CorpusKit,
        reference: Jdn,
        variant: Int,
    ): Phrase =
        when (variant % 3) {
            0 -> spanning(kit, "fa", TextSnippets.near(kit, PERSIAN, reference), "از", "تا")
            1 -> dayRange(kit, "fa", TextSnippets.near(kit, PERSIAN, reference), "از", "تا")
            else -> spanning(kit, "fa", TextSnippets.near(kit, GREGORIAN, reference), "از", "تا")
        }

    fun enRange(
        kit: CorpusKit,
        reference: Jdn,
        variant: Int,
    ): Phrase {
        val date = TextSnippets.near(kit, GREGORIAN, reference)
        return if (variant % 2 ==
            0
        ) {
            spanning(kit, "en", date, "from", "to")
        } else {
            dayRange(kit, "en", date, "from", "to")
        }
    }

    private fun spanning(
        kit: CorpusKit,
        language: String,
        first: CalendarDate,
        from: String,
        to: String,
    ): Phrase {
        val calendar = kit.calendar(first.system)
        val end = kit.jdn(first) + TextSnippets.rangeDays(kit)
        val last = calendar.fromJdn(end)
        return Phrase("$from ${named(kit, language, first)} $to ${named(kit, language, last)}", kit.jdn(first), end)
    }

    private fun dayRange(
        kit: CorpusKit,
        language: String,
        date: CalendarDate,
        from: String,
        to: String,
    ): Phrase {
        val lastDay = kit.number(2..minOf(date.day + 1, kit.calendar(date.system).monthLength(date.year, date.month)))
        val firstDay = kit.number(1 until lastDay)
        val last = date.copy(day = lastDay)
        val text = "$from $firstDay $to ${named(kit, language, last)}"
        return Phrase(text, kit.jdn(date.copy(day = firstDay)), kit.jdn(last))
    }

    private fun weekday(
        kit: CorpusKit,
        language: String,
        date: String,
        jdn: Jdn,
        separator: String,
    ): Phrase {
        val name = kit.weekdayName(if (language == "fa") kit.fa else kit.en, jdn.weekday())
        return Phrase("$name$separator$date", jdn, spanStart = name.length + separator.length)
    }

    private fun yearless(
        kit: CorpusKit,
        reference: Jdn,
    ): Phrase {
        val month = kit.number(1..12)
        val day = TextSnippets.yearlessDay(kit)
        val text = "$day ${kit.monthName(kit.fa, PERSIAN, month)}"
        return Phrase(text, TextSnippets.nearest(kit, reference, month, day))
    }
}
