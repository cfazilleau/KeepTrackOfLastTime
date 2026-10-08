package dev.cfaz.timeclicker.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracker_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Display order of chips and sections; lower comes first. */
    val position: Int,
    /** Lucide icon name, or "none" for a group without an icon. */
    @ColumnInfo(defaultValue = "none") val icon: String = "none",
)

@Entity(
    tableName = "trackers",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            // Deleting a group keeps its tiles; they become ungrouped (shown only under "All").
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("group_id")],
)
data class TrackerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Display order on the home screen; lower comes first. */
    val position: Int,
    @ColumnInfo(name = "group_id") val groupId: Long? = null,
    @ColumnInfo(defaultValue = "sage") val color: String = "sage",
    @ColumnInfo(defaultValue = "check") val icon: String = "check",
    @ColumnInfo(defaultValue = "small") val size: String = "small",
    /** File name in the app's private photo folder. */
    val photo: String? = null,
    /** The press counter counts events after this time (millis); reset by the user. */
    @ColumnInfo(name = "count_since", defaultValue = "0") val countSince: Long = 0,
    /** Reminder after [reminderEvery] [reminderUnit] ("hours", "days", "weeks") without being done; null for none. */
    @ColumnInfo(name = "reminder_every") val reminderEvery: Int? = null,
    @ColumnInfo(name = "reminder_unit") val reminderUnit: String? = null,
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

/** A tracker with its last-done time and how many times it was pressed since [TrackerEntity.countSince]. */
data class TrackerWithLastDone(
    @Embedded val tracker: TrackerEntity,
    @ColumnInfo(name = "last_done_at") val lastDoneAt: Long?,
    @ColumnInfo(name = "press_count") val pressCount: Int,
)
