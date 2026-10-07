package dev.cfaz.timeclicker.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Tile photos are copied into the app's private storage, so they keep working offline
 * and survive the original being deleted from the gallery.
 */
class PhotoStore(private val context: Context) {

    private val directory: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(name: String): File = File(directory, name)

    /** Copies the picked image and returns its file name, or null if it could not be read. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        val name = "${UUID.randomUUID()}.img"
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { source -> file(name).outputStream().use { source.copyTo(it) } }
            name
        } catch (e: IOException) {
            file(name).delete()
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Deletes every stored photo not in [keep] (replaced, removed, or picked then discarded). */
    suspend fun retainOnly(keep: Set<String>) = withContext(Dispatchers.IO) {
        directory.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
    }
}
