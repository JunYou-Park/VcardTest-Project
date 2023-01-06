package com.ctb.vcardtest_project

import android.accounts.Account
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import com.android.vcard.*
import com.android.vcard.exception.VCardException
import com.android.vcard.exception.VCardNestedException
import com.android.vcard.exception.VCardVersionException
import com.ctb.vcardtest_project.util.FileUtils.copyTo
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors


class ReadVCardProcessor(private val mContext: Context, private val mHandler: Handler?) :
    VCardEntryHandler {
    private var mSourceUri: Uri? = null
    private var mSourceDisplayName: String? = ""
    private val mVCardEntries: ArrayList<VCardEntry> = ArrayList()
    private var mConstructor: VCardEntryConstructor? = null
    fun initialize(sourceUri: Uri?) {
        Log.d(TAG, "initialize: sourceUri=$sourceUri")
        mVCardEntries.clear()
        if (sourceUri != null) {
            mSourceDisplayName = getDisplayName(sourceUri)
            Log.d(
                TAG,
                "initialize: mSourceDisplayName=$mSourceDisplayName"
            )
            mSourceUri = readUriToLocalFile(sourceUri)
            mConstructor = VCardEntryConstructor(0, Account("null", "null"), null)
            mConstructor!!.addEntryHandler(this)
        }
    }

    private fun readUriToLocalFile(sourceUri: Uri): Uri? {
        val localFilename = "import_" + System.currentTimeMillis() + "_" + mSourceDisplayName
        return try {
            copyTo(mContext, sourceUri, localFilename)
        } catch (e: IOException) {
            Log.e(TAG, "readUriToLocalFile: ", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "readUriToLocalFile: ", e)
            null
        }
    }

    private fun getDisplayName(sourceUri: Uri?): String? {
        if (sourceUri == null) {
            return null
        }
        val resolver = mContext.contentResolver
        var displayName: String? = null
        var cursor: Cursor? = null
        // Try to get a display name from the given Uri. If it fails, we just
        // pick up the last part of the Uri.
        try {
            cursor = resolver.query(
                sourceUri, arrayOf(OpenableColumns.DISPLAY_NAME),
                null, null, null
            )
            if (cursor != null && cursor.count > 0 && cursor.moveToFirst()) {
                if (cursor.count > 1) {
                    Log.w(
                        TAG, "Unexpected multiple rows: "
                                + cursor.count
                    )
                }
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    displayName = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
        if (TextUtils.isEmpty(displayName)) {
            displayName = sourceUri.lastPathSegment
        }
        return displayName
    }

    override fun onStart() {}
    override fun onEntryCreated(entry: VCardEntry) {
        mVCardEntries.add(entry)
    }

    override fun onEnd() {
        if (mHandler != null) {
            val message = Message.obtain(mHandler, 0, mVCardEntries)
            message.sendToTarget()
        }
    }

    @JvmOverloads
    fun readVCard(
        arrUris: Array<Uri?> = arrayOf(mSourceUri),
        arrSourceDisplayNames: Array<String?> = arrayOf(mSourceDisplayName)
    ) {
        val service = Executors.newSingleThreadExecutor()
        service.execute {
            var i = 0
            for (sourceUri in arrUris) {
                val sourceDisplayName = arrSourceDisplayNames[i++]
                try {
                    constructImportRequest(mContext, sourceUri, sourceDisplayName)
                } catch (e: VCardException) {
                    Log.e(TAG, "VCardException: ", e)
                } catch (e: IOException) {
                    Log.e(TAG, "IOException: ", e)
                }
            }
        }
    }

    @Throws(IOException::class, VCardException::class)
    private fun constructImportRequest(context: Context, localDataUri: Uri?, displayName: String?) {
        val resolver = context.contentResolver
        var vCardParser: VCardParser? = null
        var vcardVersion = VCARD_VERSION_V21
        try {
            var shouldUseV30 = false
            var stream = resolver.openInputStream(localDataUri!!)
            vCardParser = VCardParser_V21()
            try {
                vCardParser.parse(stream, mConstructor)
            } catch (e1: VCardVersionException) {
                try {
                    stream!!.close()
                } catch (e: IOException) {
                    Log.e(TAG, "constructImportRequest: ", e)
                }
                shouldUseV30 = true
                stream = resolver.openInputStream(localDataUri)
                vCardParser = VCardParser_V30()
                try {
                    vCardParser.parse(stream, mConstructor)
                } catch (e2: VCardVersionException) {
                    throw VCardException("vCard with unsupported version.")
                }
            } finally {
                if (stream != null) {
                    try {
                        stream.close()
                    } catch (e: IOException) {
                        Log.e(TAG, "constructImportRequest: ", e)
                    }
                }
            }
            vcardVersion = if (shouldUseV30) VCARD_VERSION_V30 else VCARD_VERSION_V21
        } catch (e: VCardNestedException) {
            Log.w(TAG, "Nested Exception is found (it may be false-positive).")
            // Go through without throwing the Exception, as we may be able to detect the
            // version before it
        } catch (e: IOException) {
            Log.e(TAG, "constructImportRequest: e", e)
        }
    }

    companion object {
        private const val TAG = "VCardUtils"
        const val VCARD_VERSION_AUTO_DETECT = 0
        const val VCARD_VERSION_V21 = 1
        const val VCARD_VERSION_V30 = 2
    }
}