package com.karwa.mdtnavigation.model

import android.util.Log
import com.mapbox.geojson.Point

data class ChunkModel(
    var isVisited: Boolean = false,
    var list: ArrayList<Point>,
    var linearDistanceInMeter: Double = 0.0
)

fun ArrayList<ChunkModel>.getDistance(isOffRoute: Boolean): Double {
    var distance = 0.0
    var currentIndex = -1
    for (index in this.indices) {
        if (!this[index].isVisited) {
            if (currentIndex == -1)
                currentIndex = index
            if (currentIndex == index) {
                if (!isOffRoute)
                    distance += this[index].linearDistanceInMeter
            } else {
                distance += this[index].linearDistanceInMeter
            }
        }
    }
    Log.e("distance", "Total Remaining Chunk Distance::: " + distance)
    return distance * 1000
}