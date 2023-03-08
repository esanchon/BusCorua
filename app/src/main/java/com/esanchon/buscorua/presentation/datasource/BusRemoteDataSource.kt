package com.esanchon.buscorua.presentation.datasource

import com.esanchon.buscorua.presentation.domain.BusTime
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL


class BusRemoteDataSource : BusRemoteDataSourceContract {

   override fun getTimeToWork() : BusTime? {
        // Create OkHttp Client

       val client: OkHttpClient = OkHttpClient().newBuilder().build()
//        var client: OkHttpClient = OkHttpClient().newBuilder().addNetworkInterceptor { chain ->
//            chain.proceed(
//                chain.request()
//                    .newBuilder()
//                    .header("User-Agent", "MapaBusesCoruña/1.0")
//                    .build()
//            )
//        }.build()

            try {
                val timestamp = System.currentTimeMillis().toString()
                // Create URL
                val url = URL("https://itranvias.com/queryitr_v3.php?&dato=42&func=0&_=${timestamp}")
                // Build request
                val request = Request.Builder().url(url).build()

                // Execute request
                val response = client.newCall(request).execute()

                val responseJSON = JSONTokener(response.body?.string()).nextValue() as JSONObject
                val buses = responseJSON.getJSONObject("buses")
                val lines = buses.getJSONArray("lineas")
                //var result: String? = null
                var busTime : BusTime = BusTime()


                for (i in 0 until lines.length()) {
                    val line = lines.getJSONObject(i)
                    if(line.getString("linea") == "301") {
                        //busTime?.time = line.getJSONArray("buses").getJSONObject(0).getString("tiempo")
                            busTime.time = line.getJSONArray("buses").getJSONObject(0).getString("tiempo")
                        }

                    }
                return busTime

            }
            catch(err:Error) {
                return null
            }
            catch(err:Exception) {
                return null
            }

    }

    override fun getTimeToHome(): BusTime? {
        val client: OkHttpClient = OkHttpClient().newBuilder().build()

        try {
            val timestamp = System.currentTimeMillis().toString()
            // Create URL
            val url = URL("https://itranvias.com/queryitr_v3.php?&dato=3&func=0&_=${timestamp}")
            // Build request
            val request = Request.Builder().url(url).build()

            // Execute request
            val response = client.newCall(request).execute()

            val responseJSON = JSONTokener(response.body?.string()).nextValue() as JSONObject
            val buses = responseJSON.getJSONObject("buses")
            val lines = buses.getJSONArray("lineas")
            var busTime : BusTime = BusTime()

            for (i in 0 until lines.length()) {
                val line = lines.getJSONObject(i)
                if(line.getString("linea") == "301") {
                    busTime.time = line.getJSONArray("buses").getJSONObject(0).getString("tiempo")
                }
            }
            return busTime
        }
        catch(err:Error) {
            return null
        }
        catch(err:Exception) {
            return null
        }
    }
}
