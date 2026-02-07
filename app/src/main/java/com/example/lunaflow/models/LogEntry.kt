package com.example.lunaflow.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.TypeParceler

// Parceler for Map<String, Any> to preserve types
object MapParceler : Parceler<Map<String, Any>> {
    override fun create(parcel: Parcel): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        parcel.readMap(map, Map::class.java.classLoader)
        return map
    }

    override fun Map<String, Any>.write(parcel: Parcel, flags: Int) {
        parcel.writeMap(this)
    }
}

@Parcelize
@TypeParceler<Map<String, Any>, MapParceler>
data class LogEntry(
    val id: String = "",
    val cycleId: String = "",
    val type: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "",
    val details: String = "",
    val data: Map<String, Any> = emptyMap()
) : Parcelable
