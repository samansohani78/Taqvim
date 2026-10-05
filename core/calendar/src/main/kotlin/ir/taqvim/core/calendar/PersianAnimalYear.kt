/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

/**
 * The twelve animals of the Persian twelve-year cycle, in cycle order (DT-015).
 *
 * These are **not** the Chinese twelve. The Persian cycle has a leopard where the Chinese has a tiger, a whale where
 * it has a dragon, and a hen where it has a rooster, and the names below are the ones Persian itself uses. The
 * ordered list is Dehkhoda's (لغت‌نامه دهخدا, entry سال), which gives the Turkic name of each year with its Persian
 * gloss: «۱ - سیچقان ئیل؛ سال موش ۲ - اودئیل؛ سال گاو ۳ - بارس ئیل؛ سال پلنگ ۴ - توشقان ئیل؛ سال خرگوش ۵ - لوی ئیل؛
 * سال نهنگ ۶ - ئیلان ئیل؛ سال مار ۷ - یونت ئیل؛ سال اسب ۸ - قوی ئیل؛ سال گوسفند ۹ - بیچی ئیل؛ سال میمون ۱۰ - تخلقوی
 * ئیل؛ سال مرغ ۱۱ - ایت ئیل؛ سال سگ ۱۲ - تنگوزئیل؛ سال خوک», citing the Dīvān Lughāt al-Turk of Maḥmūd al-Kāshgharī.
 * Dehkhoda prints the same twelve as the verse of Abū Naṣr Farāhī's Niṣāb al-Ṣibyān — «موش و بقر و پلنگ و خرگوش شمار
 * / زین چار چو بگذری نهنگ آید و مار / و آنگاه به اسب و گوسفند است حساب / حمدونه و مرغ و سگ و خوک آخر کار» — so the
 * list is corroborated by a second classical source.
 */
public enum class PersianZodiacAnimal {
    /** موش */
    MOUSE,

    /** گاو */
    OX,

    /** پلنگ — a leopard, where the Chinese cycle has a tiger. */
    LEOPARD,

    /** خرگوش */
    RABBIT,

    /** نهنگ — a whale, where the Chinese cycle has a dragon. */
    WHALE,

    /** مار */
    SNAKE,

    /** اسب */
    HORSE,

    /** گوسفند */
    SHEEP,

    /** میمون — Dehkhoda's standalone entry پیچین ئیل glosses the same year as بوزینه, and the Niṣāb verse as حمدونه. */
    MONKEY,

    /** مرغ */
    HEN,

    /** سگ */
    DOG,

    /** خوک */
    PIG,
}

/**
 * The twelve-animal cycle as it was used in Iran (DT-015), which is **not** aligned with the Chinese one.
 *
 * R. Abdollahy, "CALENDARS ii. In the Islamic period", *Encyclopaedia Iranica* IV/6-7, pp. 658–677, states the rule
 * outright: "add 6 to the year in question and divide by 12; the remainder … 1 = mouse, 2 = ox, 3 = tiger, and so on
 * up to 12 = pig". The year it applies to is the **Solar Hijri** year, so the animal changes at Nowruz rather than at
 * the Chinese new year, and a Gregorian year spans two of them. Checked against the years in force: 1403 is the
 * whale, 1404 the snake, 1405 the horse.
 *
 * Abdollahy also dates the modern usage: the Majles adopted it in 1329/1911 and dropped it in 1344/1925, "the naming
 * of years for animals is still customary in certain Persian almanacs". [IN_FORCE_YEARS] records that, so a caller
 * can say when the naming was official rather than implying it always was.
 */
public object PersianAnimalYear {
    private const val OFFSET = 6
    private const val CYCLE_YEARS = 12

    /** Solar Hijri years in which the animal naming was official (Majles 1329 SH to its abrogation in 1344 SH). */
    public val IN_FORCE_YEARS: IntRange = 1329..1344

    /** The animal of Solar Hijri [persianYear], by Abdollahy's rule. */
    public fun of(persianYear: Int): PersianZodiacAnimal =
        PersianZodiacAnimal.entries[Math.floorMod(persianYear + OFFSET - 1, CYCLE_YEARS)]
}
