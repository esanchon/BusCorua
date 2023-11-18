package com.esanchon.buscorua.presentation.datasource

import com.esanchon.buscorua.presentation.domain.BusTime
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL


class BusRemoteDataSource : BusRemoteDataSourceContract {

    override fun getTime(busStop : Int): BusTime? {
        // Create OkHttp Client
        val client: OkHttpClient = OkHttpClient().newBuilder().build()

        try {
            var busTime: BusTime = BusTime()
            var timestamp = System.currentTimeMillis().toString()
            // Create URL
            var url = URL("https://itranvias.com/queryitr_v3.php?&dato=${busStop}&func=0&_=${timestamp}")
            // Build request
            var request = Request.Builder().url(url).build()

            // Execute request
            var response = client.newCall(request).execute()

            var responseJSON = JSONTokener(response.body?.string()).nextValue() as JSONObject
            var buses = responseJSON.getJSONObject("buses")
            var lines = buses.getJSONArray("lineas")

            for (i in 0 until lines.length()) {
                val line = lines.getJSONObject(i)
                if (line.getString("linea") == "301") {
                    //busTime?.time = line.getJSONArray("buses").getJSONObject(0).getString("tiempo")
                    busTime.time =
                        line.getJSONArray("buses").getJSONObject(0).getString("tiempo")
                }

            }
            return busTime
        } catch (err: Error) {
            return null
        } catch (err: Exception) {
            return null
        }
    }
}
