package com.triviamap.data.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine

/**
 * Parses the curated Strasbourg stations and lines JSON.
 */
object GeoJsonParser {

    private val gson = Gson()

    fun parseData(json: String): List<TramLine> {
        val root = gson.fromJson(json, JsonObject::class.java)
        val linesObj = root.getAsJsonObject("lines") ?: return emptyList()
        
        return linesObj.entrySet().map { (lineId, element) ->
            val lineData = element.asJsonObject
            val colorHex = lineData.get("color")?.asString ?: "#888888"
            val textColorHex = lineData.get("text_color")?.asString ?: "#FFFFFF"
            
            val stations = lineData.getAsJsonArray("stations").map { s ->
                val sObj = s.asJsonObject
                Station(
                    id = sObj.get("id").asString,
                    name = sObj.get("name").asString,
                    position = GeoPoint(sObj.get("x").asDouble, sObj.get("y").asDouble),
                    lines = listOf(lineId) // In this dataset, stations are per line
                )
            }
            
            val geometry = lineData.getAsJsonArray("geometry").map { g ->
                val gArr = g.asJsonArray
                GeoPoint(gArr[0].asDouble, gArr[1].asDouble)
            }

            TramLine(
                id = lineId,
                name = "Ligne $lineId",
                color = parseColor(colorHex),
                textColor = parseColor(textColorHex),
                geometry = geometry,
                stations = stations
            )
        }.sortedBy { it.id }
    }

    private fun parseColor(hex: String): Long {
        val clean = hex.trimStart('#')
        return try {
            when (clean.length) {
                6 -> 0xFF000000L or clean.toLong(16)
                8 -> clean.toLong(16)
                else -> 0xFF888888L
            }
        } catch (e: Exception) {
            0xFF888888L
        }
    }
}
