package com.esanchon.buscorua.presentation.domain

/** [latitude]/[longitude] son null si el catálogo no trae la posición de la parada. */
data class StopInfo(
    val id: Int,
    val name: String,
    val lineIds: Set<String>,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * @param id código interno de la API (301).
 * @param name nombre comercial ("3A").
 * @param stops paradas del recorrido en orden (ida y luego vuelta, sin repetir).
 */
data class LineInfo(
    val id: String,
    val name: String,
    val origin: String,
    val destination: String,
    val stops: List<Int>
) {
    val description: String get() = listOf(origin, destination).filter { it.isNotBlank() }.joinToString(" – ")
}

/** Catálogo de la red de iTranvías: líneas y paradas. */
class Catalog(
    val lines: Map<String, LineInfo>,
    val stops: Map<Int, StopInfo>
) {
    fun lineName(lineId: String): String = lines[lineId]?.name ?: lineId

    fun stopName(stop: Int): String? = stops[stop]?.name

    /** Todas las líneas, ordenadas como las ve el usuario: 1, 1A, 2, 2A... y las de letras al final. */
    val sortedLines: List<LineInfo> by lazy { lines.values.distinctBy { it.name.uppercase() }.sortedWith(LINE_ORDER) }

    /** Líneas que paran en alguna de [stops], sin repetir nombre y ordenadas. */
    fun linesServing(stops: List<Int>): List<LineInfo> {
        val ids = stops.flatMap { this.stops[it]?.lineIds.orEmpty() }.toSet()
        return ids.mapNotNull { lines[it] }.distinctBy { it.name.uppercase() }.sortedWith(LINE_ORDER)
    }

    /** El usuario guarda el nombre ("3A"); también se acepta el código interno ("301"). */
    private fun lineIdsFor(line: String): Set<String> {
        val wanted = line.uppercase()
        return lines.values.filter { it.name.uppercase() == wanted || it.id == wanted }.map { it.id }.toSet()
    }

    fun unknownStops(stops: List<Int>): List<Int> = stops.filter { it !in this.stops }

    fun unknownLines(lines: List<String>): List<String> = lines.filter { lineIdsFor(it).isEmpty() }

    companion object {
        /** Orden por número y luego por sufijo: 1 < 1A < 2 < 12 < 23A < BUH < UDC. */
        val LINE_ORDER: Comparator<LineInfo> = compareBy<LineInfo>(
            { it.name.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE },
            { it.name.uppercase() }
        )
    }
}
