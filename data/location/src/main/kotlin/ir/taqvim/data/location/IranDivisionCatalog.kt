/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

/** The bundled Iran province/county list (T-603, T-1502) with lookup by code and by parent. */
class IranDivisionCatalog(
    val divisions: List<IranDivision>,
) {
    private val byCode: Map<String, IranDivision> = divisions.associateBy(IranDivision::code)
    private val byParentCode: Map<String, List<IranDivision>> =
        divisions.filter { it.parentCode != null }.groupBy { it.parentCode!! }

    /** The 31 provinces, in the order they are bundled. */
    val provinces: List<IranDivision> = divisions.filter { it.level == IranDivisionLevel.PROVINCE }

    fun division(code: String): IranDivision? = byCode[code]

    /** The counties of the province [code], or an empty list for an unknown or leaf code. */
    fun counties(code: String): List<IranDivision> = byParentCode[code].orEmpty()

    companion object {
        // Absolute: R8 repackages the classes of a release build, so a relative resource name misses the file.
        private const val RESOURCE = "/ir/taqvim/data/location/iran-divisions.tsv"

        /** Loads the bundled list (466 divisions); call it off the main thread. */
        fun loadBundled(): IranDivisionCatalog {
            val stream =
                checkNotNull(IranDivisionCatalog::class.java.getResourceAsStream(RESOURCE)) { "$RESOURCE is missing" }
            return IranDivisionCatalog(stream.bufferedReader(Charsets.UTF_8).useLines(IranDivisionTableParser::parse))
        }
    }
}
