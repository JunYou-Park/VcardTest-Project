package com.ctb.vcardtest_project.util

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.ctb.vcardtest_project.R
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.*

object FileUtils {
    private const val TAG = "FileUtils"
    private const val BUFFER_SIZE = 8 * 1024;
    private val FILE_DIR = (Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOWNLOADS + "/")
    private const val INCREMENT_NUMBER = 2
    private const val COLUMN_CONTENT_TYPE = "ct"

    fun createNewFileName(name: String ,type: String): String{
        val prefix = System.currentTimeMillis().toString() + "_"
        return "$prefix${name}.$type"
    }

    fun getRealPathFromURI(context: Context, contentUri: Uri): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(contentUri, proj, null, null, null)
        var result = "";
        if(cursor!=null && cursor.moveToFirst()){
            val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            result = cursor.getString(column_index)
            cursor.close()
        }
        return result
    }

    fun saveFile(activity: Activity, attachmentName: String, attachmentUri: Uri) {
        var resId: Int = R.string.copy_to_sdcard_fail
        val saveUri: Uri? = saveAttachment(activity, attachmentUri, attachmentName)
        if (saveUri != null) {
            resId = R.string.copy_to_sdcard_success
            activity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, saveUri))
        }
        Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show()
    }


    fun saveAttachment(context: Context, attachmentUri: Uri, attachmentName: String): Uri? {
        var input: InputStream? = null
        var fout: FileOutputStream? = null
        var saveUri: Uri? = null
        try {
            input = context.contentResolver.openInputStream(attachmentUri)
            if (input is FileInputStream) {
                val saveFile: File = createSaveFile(context, attachmentUri, attachmentName)
                val parentFile: File = saveFile.parentFile as File
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    Log.w(TAG, "[MMS] copyPart: mkdirs for " + parentFile.path.toString() + " failed!")
                    return null
                }
                val fin: FileInputStream = input
                Log.d(TAG, "saveAttachment: size=${fin.channel.size()}")
                fout = FileOutputStream(saveFile)
                val buffer = ByteArray(BUFFER_SIZE)
                var size = 0
                while (fin.read(buffer).also { size = it } != -1) {
                    fout.write(buffer, 0, size)
                }
                saveUri = Uri.fromFile(saveFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception caught while save attachment: ", e)
            return null
        } finally {
            if (null != input) {
                try {
                    input.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Exception caught while closing input: ", e)
                }
            }
            if (null != fout) {
                try {
                    fout.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Exception caught while closing output: ", e)
                }
            }
        }
        return saveUri
    }

    private fun createSaveFile(context: Context, attachmentUri: Uri, attachmentName: String): File {
        var fileName: String = File(attachmentName).name
        var extension: String? = ""
        var index: Int
        if (fileName.lastIndexOf('.').also { index = it } != -1) {
            extension = fileName.substring(index + 1, fileName.length)
            fileName = fileName.substring(0, index)
        }
        else {
            val cursor: Cursor? = context.contentResolver.query(attachmentUri, null, null, null, null)
            cursor?.use { cursor1 ->
                if (cursor1.moveToFirst()) {
                    val type: String = cursor1.getString(cursor1.getColumnIndexOrThrow(
                        COLUMN_CONTENT_TYPE
                    ))
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
                }
            }

        }
        fileName = fileName.replace("^\\.".toRegex(), "")
        var file = File("$FILE_DIR$fileName.$extension")
        // Add incrementing number after file name if have existed
        // the file has same name.
        var i: Int = INCREMENT_NUMBER
        while (file.exists()) {
            file = File(FILE_DIR + fileName + "_" + i + "." + extension)
            i++
        }
        return file
    }

    @Throws(IOException::class)
    fun copyTo(context: Context, sourceUri: Uri, fileName: String): Uri? {
        val resolver: ContentResolver = context.contentResolver
        var inputChannel: ReadableByteChannel? = null
        var outputChannel: WritableByteChannel? = null
        var destUri: Uri? = null
        try {
            inputChannel = Channels.newChannel(resolver.openInputStream(sourceUri))
            destUri = Uri.parse(context.getFileStreamPath(fileName).toURI().toString())
            outputChannel = context.openFileOutput(fileName, Context.MODE_PRIVATE).channel
            val buffer: ByteBuffer = ByteBuffer.allocateDirect(8192)
            while (inputChannel.read(buffer) != -1) {
                buffer.flip()
                outputChannel.write(buffer)
                buffer.compact()
            }
            buffer.flip()
            while (buffer.hasRemaining()) {
                outputChannel.write(buffer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy", e)
            return null
        } finally {
            if (inputChannel != null) {
                try {
                    inputChannel.close()
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to close inputChannel.")
                }
            }
            if (outputChannel != null) {
                try {
                    outputChannel.close()
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to close outputChannel")
                }
            }
        }
        return destUri
    }

    fun getFileSize(context: Context, uri: Uri?): Long {
        var size = 0L
        if (uri != null) {
            try {
                val c_size: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                if (c_size != null && c_size.moveToFirst()) {
                    size = c_size.getLong(c_size.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    c_size.close()
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "getFileSize", e)
            }
        }
        return size
    }
}