// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs.mountpointlist.mountpoint

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.net.toUri
import java.io.File
import java.lang.reflect.Array as JArray

// Some strange code from stackoverflow
object FileUtil {
    fun getFullPathFromTreeUri(treeUri: Uri, con: Context): String {
        var (volumeId, documentPath) = splitTreeUri(treeUri)
        var volumePath = volumeId?.let { getVolumePath(volumeId, con) } ?: return File.separator

        volumePath = volumePath.trimEnd(File.separatorChar)
        documentPath = documentPath.trim(File.separatorChar)

        return if (documentPath.isNotEmpty()) volumePath + File.separator + documentPath else volumePath
    }

    private fun getVolumePath(volumeId: String, context: Context): String? {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val storageManager =
                    context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

                for (storageVolume in storageManager.storageVolumes) {
                    val directory = storageVolume.directory
                    if (storageVolume.isPrimary && PRIMARY_VOLUME_NAME == volumeId) {
                        return directory?.absolutePath
                    } else if (storageVolume.uuid != null) {
                        if (storageVolume.uuid.equals(volumeId)) {
                            return directory?.absolutePath
                        }
                    }
                }

                // not found.
                return null
            }

            else -> {
                try {
                    val mStorageManager =
                        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

                    val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")

                    val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
                    val getUuid = storageVolumeClazz.getMethod("getUuid")
                    val getPath = storageVolumeClazz.getMethod("getPath")
                    val isPrimary = storageVolumeClazz.getMethod("isPrimary")
                    val result = getVolumeList.invoke(mStorageManager) ?: return null

                    val length = JArray.getLength(result)
                    for (i in 0 until length) {
                        val storageVolumeElement = JArray.get(result, i)
                        val uuid = getUuid.invoke(storageVolumeElement) as String?
                        val primary = isPrimary.invoke(storageVolumeElement) as Boolean

                        // primary volume
                        if (primary && volumeId == "primary") {
                            return getPath.invoke(storageVolumeElement) as String
                        }

                        // other volumes
                        if (uuid == volumeId) {
                            return getPath.invoke(storageVolumeElement) as String
                        }
                    }

                    // not found
                    return null
                } catch (_: Exception) {
                    return null
                }
            }
        }
    }

    private fun splitTreeUri(treeUri: Uri): Pair<String?, String> {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = splitId(treeDocumentId)
        val volumeId = split.getOrNull(0)
        val path = split.getOrElse(1) { "/" }

        return Pair(volumeId, path)
    }

    private fun splitId(id: String): Array<String> =
        id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    fun getPath(uri: Uri, context: Context): String? = when {
        DocumentsContract.isDocumentUri(context, uri) -> getDocumentProviderPath(context, uri)
        isMediaStoreUri(uri) -> getRemoteAddress(context, uri)
        isFileUri(uri) -> uri.path
        else -> null
    }

    private fun isMediaStoreUri(uri: Uri): Boolean = "content".equals(uri.scheme, ignoreCase = true)

    private fun isFileUri(uri: Uri): Boolean = "file".equals(uri.scheme, ignoreCase = true)

    private fun getDocumentProviderPath(context: Context, uri: Uri): String? = when {
        isExternalStorageDocument(uri) -> getExternalStorageProviderPath(uri)
        isDownloadsDocument(uri) -> getDownloadsProviderPath(context, uri)
        isMediaDocument(uri) -> getMediaProviderPath(context, uri)
        else -> null
    }

    private fun getRemoteAddress(context: Context, uri: Uri): String? =
        if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri)

    private fun isGooglePhotosUri(uri: Uri): Boolean =
        uri.authority == "com.google.android.apps.photos.content"

    private fun isExternalStorageDocument(uri: Uri): Boolean =
        uri.authority == "com.android.externalstorage.documents"

    private fun isDownloadsDocument(uri: Uri): Boolean =
        uri.authority == "com.android.providers.downloads.documents"

    private fun isMediaDocument(uri: Uri): Boolean =
        uri.authority == "com.android.providers.media.documents"

    private fun getExternalStorageProviderPath(uri: Uri): String {
        val (uriType, uriValue) = splitUri(uri)

        return if ("primary".equals(uriType, ignoreCase = true)) {
            Environment.getExternalStorageDirectory().path + "/" + uriValue
        } else {
            val storageDefinition =
                if (Environment.isExternalStorageRemovable()) "EXTERNAL_STORAGE" else "SECONDARY_STORAGE"

            val storage = System.getenv(storageDefinition) ?: ""
            "$storage/$uriValue"
        }
    }

    private fun getDownloadsProviderPath(context: Context, uri: Uri): String? {
        val id = DocumentsContract.getDocumentId(uri)
        val contentUri = try {
            ContentUris.withAppendedId(
                "content://downloads/public_downloads".toUri(), java.lang.Long.valueOf(id)
            )
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            return null
        }

        return getDataColumn(context, contentUri)
    }

    private fun getMediaProviderPath(context: Context, uri: Uri): String? {
        val (uriType, uriValue) = splitUri(uri)
        val contentUri: Uri? = when (uriType) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> null
        }
        val selection = "_id=?"
        val selectionArgs = arrayOf(uriValue)

        return contentUri?.let { getDataColumn(context, contentUri, selection, selectionArgs) }
    }

    private fun splitUri(uri: Uri): Pair<String, String> {
        val documentId = DocumentsContract.getDocumentId(uri)
        val splitUri = splitId(documentId)

        return Pair(splitUri[0], splitUri[1])
    }

    private fun getDataColumn(
        context: Context, uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null
    ): String? {
        val projection = arrayOf(DATA_COLUMN_NAME)

        return context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            .use { cursor -> cursor?.let { getDataColumn(cursor) } }
    }

    private fun getDataColumn(cursor: Cursor): String? = if (cursor.moveToFirst()) {
        val columnIndex = cursor.getColumnIndexOrThrow(DATA_COLUMN_NAME)
        cursor.getString(columnIndex)
    } else null

    private const val DATA_COLUMN_NAME = "_data"

    /**
     * The name of the primary volume (LOLLIPOP).
     */
    private const val PRIMARY_VOLUME_NAME = "primary"
}
