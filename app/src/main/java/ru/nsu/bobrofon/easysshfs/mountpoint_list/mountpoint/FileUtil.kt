package ru.nsu.bobrofon.easysshfs.mountpoint_list.mountpoint

import java.io.File
import java.lang.reflect.Array

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore

@SuppressLint("NewApi")
object FileUtil {

    private val PRIMARY_VOLUME_NAME = "primary"

    fun getFullPathFromTreeUri(treeUri: Uri?, con: Context): String? {
        if (treeUri == null) {
            return null
        }
        var volumePath: String? = FileUtil.getVolumePath(FileUtil.getVolumeIdFromTreeUri(treeUri), con)
                ?: return File.separator
        if (volumePath!!.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length - 1)
        }

        var documentPath = FileUtil.getDocumentPathFromTreeUri(treeUri)
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length - 1)
        }

        return if (documentPath.length > 0) {
            if (documentPath.startsWith(File.separator)) {
                volumePath + documentPath
            } else {
                volumePath + File.separator + documentPath
            }
        } else {
            volumePath
        }
    }


    private fun getVolumePath(volumeId: String?, con: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null
        }

        try {
            val mStorageManager = con.getSystemService(Context.STORAGE_SERVICE) as StorageManager

            val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")

            val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
            val getUuid = storageVolumeClazz.getMethod("getUuid")
            val getPath = storageVolumeClazz.getMethod("getPath")
            val isPrimary = storageVolumeClazz.getMethod("isPrimary")
            val result = getVolumeList.invoke(mStorageManager)

            val length = Array.getLength(result)
            for (i in 0 until length) {
                val storageVolumeElement = Array.get(result, i)
                val uuid = getUuid.invoke(storageVolumeElement) as String?
                val primary = isPrimary.invoke(storageVolumeElement) as Boolean

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME == volumeId) {
                    return getPath.invoke(storageVolumeElement) as String
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid == volumeId) {
                        return getPath.invoke(storageVolumeElement) as String
                    }
                }
            }

            // not found.
            return null
        } catch (ex: Exception) {
            return null
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getVolumeIdFromTreeUri(treeUri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return if (split.size > 0) {
            split[0]
        } else {
            null
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getDocumentPathFromTreeUri(treeUri: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (split.size >= 2/* && split[1] != null*/) {
            split[1]
        } else {
            File.separator
        }
    }

    fun getPath(context: Context, uri: Uri): String? {

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {

            if (isExternalStorageDocument(uri)) {// ExternalStorageProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                val storageDefinition: String


                if ("primary".equals(type, ignoreCase = true)) {

                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]

                } else {

                    if (Environment.isExternalStorageRemovable()) {
                        storageDefinition = "EXTERNAL_STORAGE"

                    } else {
                        storageDefinition = "SECONDARY_STORAGE"
                    }

                    val storage = System.getenv(storageDefinition) ?: ""
                    return storage + "/" + split[1]
                }

            } else if (isDownloadsDocument(uri)) {// DownloadsProvider

                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))

                return getDataColumn(context, contentUri, null, null)

            } else if (isMediaDocument(uri)) {// MediaProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }

        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {// MediaStore (and general)

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)

        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {// File
            return uri.path
        }

        return null
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: kotlin.Array<String>?): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }


    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }


    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}
