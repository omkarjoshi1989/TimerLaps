package com.example.timerlaps

data class Lap(
    val lapNumber: Int,
    val lapDurationMillis: Long, // Time taken for this specific lap
    val totalTimeAtLapMillis: Long // Total time from start until this lap was recorded
)