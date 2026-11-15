package com.esanchon.buscorua.presentation.datasource

import com.esanchon.buscorua.presentation.domain.Arrival
import okhttp3.Request
import org.json.JSONObject

class BusRemoteDataSource : BusRemoteDataSourceContract {

    override fun getArrivals(busStop: Int): List<Arrival>? {
        val json = query("dato=$busStop&func=0&_=${System.currentTimeMillis()}") ?: return null
        val lines = json.optJSONObject("buses")?.optJSONArray("lineas") ?: return emptyList()

        val arrivals = mutableListOf<Arrival>()
        for (i in 0 until lines.length()) {
            val line = lines.getJSONObject(i)
            val lineId = line.optString("linea")
            val buses = line.optJSONArray("buses") ?: continue
            for (j in 0 until buses.length()) {
                val minutes = buses.getJSONObject(j).optString("tiempo").toIntOrNull() ?: continue
                arrivals += Arrival(stop = busStop, lineId = lineId, minutes = minutes)
            }
        }
        return arrivals
    }

    override fun getCatalogJson(): String? {
        // La fecha antigua hace que el servidor devuelva el catálogo completo
        val json = query("func=7&dato=20160101T000000_gl_0_20160101T000000") ?: return null
        return if (json.has("iTranvias")) json.toString() else null
    }

    private fun query(params: String): JSONObject? = try {
        val request = Request.Builder()
            .url("https://itranvias.com/queryitr_v3.php?$params")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        HttpClient.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null
            else response.body?.string()?.let { JSONObject(it) }
        }
    } catch (err: Exception) {
        err.printStackTrace()
        null
    }
}
