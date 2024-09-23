package com.karwa.mdtnavigation.model

import android.util.Log
import com.mapbox.geojson.Point

data class ChunkModel(
    var isVisited: Boolean = false,
    var list: ArrayList<Point>,
    var linearDistanceInMeter: Double = 0.0
)

fun ArrayList<ChunkModel>.getDistance(): Double {
    var distance = 0.0
    for (model in this) {
        if (!model.isVisited) {
//            Log.e("distance",""+model.linearDistanceInMeter)
            distance += model.linearDistanceInMeter
        }
    }
    Log.e("distance","Total Remaining Chunk Distance::: "+distance)
    return distance * 1000
}