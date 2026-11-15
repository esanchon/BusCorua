package com.esanchon.buscorua.presentation.repository

import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSourceContract
import com.esanchon.buscorua.presentation.datasource.CatalogLocalDataSource
import com.esanchon.buscorua.presentation.domain.Catalog
import com.esanchon.buscorua.presentation.domain.LineInfo
import com.esanchon.buscorua.presentation.domain.StopInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Catálogo de líneas y paradas. Se lee de memoria, si no del fichero guardado,
 * y se descarga si no existe o tiene más de una semana. Si la descarga falla
 * se usa el guardado aunque sea antiguo. null solo si nunca se pudo descargar.
 */
class CatalogRepository(
    private val remote: BusRemoteDataSourceContract,
    private val local: CatalogLocalDataSource
) {

    suspend fun getCatalog(): Catalog? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val age = System.currentTimeMillis() - local.savedAt()
            if (age < MAX_AGE_MS) {
                memory?.let { return@withLock it }
                local.read()?.let(::parse)?.let { memory = it; return@withLock it }
            }

            val downloaded = remote.getCatalogJson()?.let { json ->
                parse(json)?.also { local.write(json) }
            }
            if (downloaded != null) {
                memory = downloaded
                return@withLock downloaded
            }

            // Sin red: mejor un catálogo antiguo que ninguno
            memory ?: local.read()?.let(::parse)?.also { memory = it }
        }
    }

    private fun parse(json: String): Catalog? = try {
        val update = JSONObject(json).getJSONObject("iTranvias").getJSONObject("actualizacion")

        val lines = mutableMapOf<String, LineInfo>()
        val linesJson = update.getJSONArray("lineas")
        for (i in 0 until linesJson.length()) {
            val line = linesJson.getJSONObject(i)
            val id = line.optString("id")
            val name = line.optString("lin_comer")
            if (id.isEmpty() || name.isEmpty()) continue

            // Recorridos (ida, vuelta y variantes) unidos en orden y sin repetir paradas
            val stops = LinkedHashSet<Int>()
            val routes = line.optJSONArray("rutas")
            for (r in 0 until (routes?.length() ?: 0)) {
                val routeStops = routes!!.getJSONObject(r).optJSONArray("paradas") ?: continue
                for (s in 0 until routeStops.length()) stops += routeStops.getInt(s)
            }
            lines[id] = LineInfo(
                id = id,
                name = name,
                origin = line.optString("nombre_orig"),
                destination = line.optString("nombre_dest"),
                stops = stops.toList()
            )
        }

        val stops = mutableMapOf<Int, StopInfo>()
        val stopsJson = update.getJSONArray("paradas")
        for (i in 0 until stopsJson.length()) {
            val stop = stopsJson.getJSONObject(i)
            val id = stop.optInt("id", -1).takeIf { it > 0 } ?: continue
            val links = stop.optJSONArray("enlaces")
            val lineIds = (0 until (links?.length() ?: 0)).map { links!!.get(it).toString() }.toSet()
            stops[id] = StopInfo(id, stop.optString("nombre"), lineIds)
        }

        if (lines.isEmpty() || stops.isEmpty()) null else Catalog(lines, stops)
    } catch (err: Exception) {
        err.printStackTrace()
        null
    }

    companion object {
        private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

        // Compartido entre pantallas para no leer/descargar el catálogo varias veces
        private val mutex = Mutex()
        @Volatile private var memory: Catalog? = null
    }
}
