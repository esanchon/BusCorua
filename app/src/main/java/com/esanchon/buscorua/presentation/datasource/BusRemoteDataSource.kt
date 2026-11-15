package com.esanchon.buscorua.presentation.datasource

import com.esanchon.buscorua.presentation.domain.BusTime
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

class BusRemoteDataSource : BusRemoteDataSourceContract {

    override fun getTime(busStop: Int): BusTime? {
        val client: OkHttpClient = OkHttpClient().newBuilder().build()

        try {
            val busTime = BusTime()
            val timestamp = System.currentTimeMillis().toString()
            val url = URL("https://itranvias.com/queryitr_v3.php?&dato=${busStop}&func=0&_=${timestamp}")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: return null

            val jsonToken = JSONTokener(responseString).nextValue()
            if (jsonToken !is JSONObject) return null

            val buses = jsonToken.optJSONObject("buses") ?: return null
            val lines = buses.optJSONArray("lineas") ?: return null

            var foundTime: String? = null
            var foundLine: String? = null

            for (i in 0 until lines.length()) {
                val line = lines.getJSONObject(i)
                val lineaNum = line.optString("linea")
                val busesArray = line.optJSONArray("buses")
                if (busesArray != null && busesArray.length() > 0) {
                    val timeVal = busesArray.getJSONObject(0).optString("tiempo", "N/A")
                    if (lineaNum == "301" || foundTime == null) {
                        foundTime = timeVal
                        foundLine = lineaNum
                        if (lineaNum == "301") break
                    }
                }
            }

            busTime.time = foundTime ?: "N/A"
            busTime.line = foundLine ?: "-"
            return busTime
        } catch (err: Exception) {
            err.printStackTrace()
            return null
        }
    }
}