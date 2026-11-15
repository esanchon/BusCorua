package com.esanchon.buscorua.presentation.datasource

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Un único cliente para toda la app: reutiliza conexiones y sesiones TLS
 * entre peticiones (importante en el reloj, donde la red va por Bluetooth).
 */
object HttpClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
}
