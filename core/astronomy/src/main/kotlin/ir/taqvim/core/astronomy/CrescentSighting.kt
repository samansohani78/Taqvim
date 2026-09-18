/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.model.Coordinates
import kotlin.time.Instant

/**
 * Whether the new crescent counts as seen from one place on one evening: the rule a calculated-observational lunar
 * calendar is calibrated on (docs/adr/0040-islamic-calibration-refit.md). [ObservationalMonthStarts] asks it once per
 * candidate evening, so an implementation may cache its answers.
 */
public fun interface CrescentSighting {
    /** Whether the crescent is seen at [place] on the first evening after [from]. */
    public fun seen(
        place: Coordinates,
        from: Instant,
    ): Boolean

    public companion object {
        /** Yallop's test (NAO Technical Note 69): seen when the class is [visibleUpTo] or better. */
        public fun yallop(visibleUpTo: CrescentVisibilityClass): CrescentSighting =
            CrescentSighting {
                place,
                from,
                ->
                Yallop.evening(place, from)?.let { it.visibility <= visibleUpTo } == true
            }

        /** Odeh's criterion (Experimental Astronomy 18, 2004): seen when the zone is [visibleUpTo] or better. */
        public fun odeh(visibleUpTo: OdehZone): CrescentSighting =
            CrescentSighting { place, from -> Odeh.evening(place, from)?.let { it.zone <= visibleUpTo } == true }
    }
}
