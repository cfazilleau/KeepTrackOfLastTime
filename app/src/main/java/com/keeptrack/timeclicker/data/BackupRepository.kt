package com.keeptrack.timeclicker.data

import android.content.Context
import android.net.Uri
import com.keeptrack.timeclicker.data.local.GroupEntity
import com.keeptrack.timeclicker.data.local.TrackerDao
import com.keeptrack.timeclicker.data.local.TrackerEntity
import com.keeptrack.timeclicker.data.local.TrackerEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** The file could not be read as a Time Clicker backup. */
class InvalidBackupException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** What an import brought in. */
data class ImportSummary(val tiles: Int, val groups: Int)

/**
 * Exports everything (tiles, groups, full history, photos and settings) to one .zip file, and imports it back.
 *
 * The zip holds `backup.json` and the tile photos under `photos/`. Importing replaces all current data.
 */
class BackupRepository(
    private val context: Context,
    private val dao: TrackerDao,
    private val photos: PhotoStore,
    private val settings: SettingsRepository,
) {

    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val groups = dao.allGroups()
        val trackers = dao.allTrackers()
        val events = dao.allEvents()
        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", settingsToJson(settings.settings.value))
            .put("groups", JSONArray(groups.map { it.toJson() }))
            .put("trackers", JSONArray(trackers.map { it.toJson() }))
            .put("events", JSONArray(events.map { it.toJson() }))

        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(json.toString(2).toByteArray())
            zip.closeEntry()
            trackers.mapNotNull { it.photo }.distinct().forEach { name ->
                val file = photos.file(name)
                if (file.isFile) {
                    zip.putNextEntry(ZipEntry(PHOTOS_DIR + name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /** Replaces all tiles, groups, history, photos and settings with the backup at [uri]. */
    suspend fun import(uri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        // Photos are unpacked aside first, so a broken file leaves the current data untouched.
        val staging = File(context.cacheDir, "import").apply { deleteRecursively(); mkdirs() }
        try {
            var jsonText: String? = null
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
            ZipInputStream(input.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    when {
                        entry.isDirectory -> Unit
                        entry.name == JSON_ENTRY -> jsonText = zip.readBytes().decodeToString()
                        entry.name.startsWith(PHOTOS_DIR) -> {
                            val name = entry.name.removePrefix(PHOTOS_DIR)
                            // Plain file names only: nothing may be written outside the photo folder.
                            if (SafeName.matches(name)) File(staging, name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
            val backup = parse(jsonText ?: throw InvalidBackupException("No $JSON_ENTRY in the file"))

            staging.listFiles()?.forEach { staged ->
                val target = photos.file(staged.name)
                target.delete()
                if (!staged.renameTo(target)) staged.copyTo(target, overwrite = true)
            }
            dao.replaceAll(backup.groups, backup.trackers, backup.events)
            backup.settings?.let { imported -> settings.update { imported } }
            photos.retainOnly(dao.photoNames().toSet())
            ImportSummary(tiles = backup.trackers.size, groups = backup.groups.size)
        } catch (e: JSONException) {
            throw InvalidBackupException("Malformed $JSON_ENTRY", e)
        } finally {
            staging.deleteRecursively()
        }
    }

    private class Backup(
        val settings: AppSettings?,
        val groups: List<GroupEntity>,
        val trackers: List<TrackerEntity>,
        val events: List<TrackerEventEntity>,
    )

    private fun parse(text: String): Backup {
        val json = JSONObject(text)
        if (json.optString("format") != FORMAT) throw InvalidBackupException("Not a Time Clicker backup")
        if (json.optInt("version") > VERSION) throw InvalidBackupException("Made by a newer version of the app")

        val groups = json.getJSONArray("groups").objects().map {
            GroupEntity(id = it.getLong("id"), name = it.getString("name"), position = it.getInt("position"))
        }
        val groupIds = groups.map { it.id }.toSet()
        val trackers = json.getJSONArray("trackers").objects().map {
            val createdAt = it.getLong("createdAt")
            TrackerEntity(
                id = it.getLong("id"),
                name = it.getString("name"),
                createdAt = createdAt,
                position = it.getInt("position"),
                groupId = it.optLongOrNull("groupId")?.takeIf { id -> id in groupIds },
                color = TileColor.fromKey(it.optString("color")).key,
                icon = TileIcon.fromKey(it.optString("icon")).key,
                size = TileSize.fromKey(it.optString("size")).key,
                photo = it.optStringOrNull("photo")?.takeIf { name -> SafeName.matches(name) },
                countSince = it.optLong("countSince", createdAt),
            )
        }
        val trackerIds = trackers.map { it.id }.toSet()
        val events = json.getJSONArray("events").objects()
            .map { TrackerEventEntity(id = it.getLong("id"), trackerId = it.getLong("trackerId"), doneAt = it.getLong("doneAt")) }
            .filter { it.trackerId in trackerIds }
        val settings = json.optJSONObject("settings")?.let(::settingsFromJson)
        return Backup(settings, groups, trackers, events)
    }

    private fun settingsToJson(s: AppSettings) = JSONObject()
        .put(SettingsRepository.THEME, s.theme.key)
        .put(SettingsRepository.WALLPAPER_COLORS, s.wallpaperColors)
        .put(SettingsRepository.TIME_DISPLAY, s.timeDisplay.key)
        .put(SettingsRepository.HAPTICS, s.haptics)
        .put(SettingsRepository.CLICK_SOUND, s.clickSound)
        .put(SettingsRepository.SHOW_COUNTER, s.showCounter)

    /** Missing keys keep their default. */
    private fun settingsFromJson(json: JSONObject): AppSettings {
        val d = AppSettings()
        return AppSettings(
            theme = ThemeMode.fromKey(json.optString(SettingsRepository.THEME, d.theme.key)),
            wallpaperColors = json.optBoolean(SettingsRepository.WALLPAPER_COLORS, d.wallpaperColors),
            timeDisplay = TimeDisplay.fromKey(json.optString(SettingsRepository.TIME_DISPLAY, d.timeDisplay.key)),
            haptics = json.optBoolean(SettingsRepository.HAPTICS, d.haptics),
            clickSound = json.optBoolean(SettingsRepository.CLICK_SOUND, d.clickSound),
            showCounter = json.optBoolean(SettingsRepository.SHOW_COUNTER, d.showCounter),
        )
    }

    private companion object {
        const val FORMAT = "time-clicker-backup"
        const val VERSION = 1
        const val JSON_ENTRY = "backup.json"
        const val PHOTOS_DIR = "photos/"
        val SafeName = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]*")
    }
}

private fun GroupEntity.toJson() = JSONObject().put("id", id).put("name", name).put("position", position)

private fun TrackerEntity.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("createdAt", createdAt)
    .put("position", position)
    .put("groupId", groupId ?: JSONObject.NULL)
    .put("color", color)
    .put("icon", icon)
    .put("size", size)
    .put("photo", photo ?: JSONObject.NULL)
    .put("countSince", countSince)

private fun TrackerEventEntity.toJson() = JSONObject().put("id", id).put("trackerId", trackerId).put("doneAt", doneAt)

private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
