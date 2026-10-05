package com.keeptrack.lasttime.data

import java.time.Instant

data class Tracker(
    val id: Long,
    val name: String,
    val lastDoneAt: Instant,
)
