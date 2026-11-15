package com.esanchon.buscorua.presentation.datasource

import android.content.Context
import java.io.File

/** Guarda en un fichero la respuesta del catálogo de iTranvías (~75 KB). */
class CatalogLocalDataSource(context: Context) {

    private val file = File(context.filesDir, "catalog.json")

    fun read(): String? = try {
        if (file.exists()) file.readText() else null
    } catch (err: Exception) {
        null
    }

    /** Momento de la última descarga, 0 si nunca se ha descargado. */
    fun savedAt(): Long = if (file.exists()) file.lastModified() else 0L

    fun write(json: String) {
        try {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json)
            tmp.renameTo(file)
        } catch (err: Exception) {
            err.printStackTrace()
        }
    }
}
