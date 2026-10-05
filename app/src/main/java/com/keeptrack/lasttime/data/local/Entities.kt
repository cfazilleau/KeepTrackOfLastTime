package com.keeptrack.lasttime.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trackers")
data class TrackerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Display order on the home screen; lower comes first. */
    val position: Int,
)

/**
 * One row per time a tracker was "done". The tracker's last-done time is the latest event,
 * so the full history is kept for future features (stats, history view, editing past entries).
 */
@Entity(
    tableName = "tracker_events",
    foreignKeys = [
        ForeignKey(
            entity = TrackerEntity::class,
            parentColumns = ["id"],
            childColumns = ["tracker_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tracker_id", "done_at")],
)
data class TrackerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "tracker_id") val trackerId: Long,
    @ColumnInfo(name = "done_at") val doneAt: Long,
)

data class TrackerWithLastDone(
    @Embedded val tracker: TrackerEntity,
    @ColumnInfo(name = "last_done_at") val lastDoneAt: Long?,
)
