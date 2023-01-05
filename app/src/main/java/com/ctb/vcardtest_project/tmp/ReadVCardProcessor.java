package com.ctb.vcardtest_project.tmp;

import static com.android.contacts.util.FileUtils.copyTo;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardVersionException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadVCardProcessor implements VCardEntryHandler {
    private static final String TAG = "VCardUtils";

    public final static int VCARD_VERSION_AUTO_DETECT = 0;
    public final static int VCARD_VERSION_V21 = 1;
    public final static int VCARD_VERSION_V30 = 2;

    private final Context mContext;
    private final Handler mHandler;
    private Uri mSourceUri = null;
    private String mSourceDisplayName = "";
    private final ArrayList<VCardEntry> mVCardEntries = new ArrayList<>();
    private VCardEntryConstructor mConstructor;

    public ReadVCardProcessor(Context mContext, Handler mHandler) {
        this.mContext = mContext;
        this.mHandler = mHandler;
    }

    public void initialize(Uri sourceUri){
        Log.d(TAG, "initialize: sourceUri=" + sourceUri);
        mVCardEntries.clear();
        if (sourceUri != null) {
            mSourceDisplayName = getDisplayName(sourceUri);
            Log.d(TAG, "initialize: mSourceDisplayName=" + mSourceDisplayName);
            mSourceUri = readUriToLocalFile(sourceUri);
            mConstructor = new VCardEntryConstructor(0, new Account("null", "null"), null);
            mConstructor.addEntryHandler(this);
        }
    }

    private Uri readUriToLocalFile(Uri sourceUri) {
        String localFilename = "import_" + System.currentTimeMillis() + "_" + mSourceDisplayName;
        try {
            return copyTo(mContext, sourceUri, localFilename);
        } catch (IOException |SecurityException e) {
            Log.e(TAG, "readUriToLocalFile: ", e);
            return null;
        }
    }

    private String getDisplayName(Uri sourceUri) {
        if (sourceUri == null) {
            return null;
        }
        final ContentResolver resolver = mContext.getContentResolver();
        String displayName = null;
        Cursor cursor = null;
        // Try to get a display name from the given Uri. If it fails, we just
        // pick up the last part of the Uri.
        try {
            cursor = resolver.query(sourceUri,
                    new String[] { OpenableColumns.DISPLAY_NAME },
                    null, null, null);
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                if (cursor.getCount() > 1) {
                    Log.w(TAG, "Unexpected multiple rows: "
                            + cursor.getCount());
                }
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    displayName = cursor.getString(index);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (TextUtils.isEmpty(displayName)){
            displayName = sourceUri.getLastPathSegment();
        }
        return displayName;
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onEntryCreated(VCardEntry entry) {
        mVCardEntries.add(entry);
    }

    @Override
    public void onEnd() {
        if(mHandler!=null) {
            Message message = Message.obtain(mHandler, 0, mVCardEntries);
            message.sendToTarget();
        }
    }


    public void readVCard() {
        readVCard(new Uri[] {mSourceUri}, new String[] {mSourceDisplayName});
    }

    public void readVCard(final Uri[] arrUris, final String[] arrSourceDisplayNames) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                for (Uri sourceUri : arrUris) {
                    String sourceDisplayName = arrSourceDisplayNames[i++];
                    try {
                        constructImportRequest(mContext, sourceUri, sourceDisplayName);
                    } catch (VCardException e) {
                        Log.e(TAG, "VCardException: ", e);
                    } catch (IOException e) {
                        Log.e(TAG, "IOException: ", e);
                    }
                }
            }
        });
    }


    private void constructImportRequest(Context context, final Uri localDataUri, final String displayName)
            throws IOException, VCardException {
        final ContentResolver resolver = context.getContentResolver();
        VCardParser vCardParser = null;
        int vcardVersion = VCARD_VERSION_V21;
        try {
            boolean shouldUseV30 = false;
            InputStream is = resolver.openInputStream(localDataUri);
            vCardParser = new VCardParser_V21();
            try {;
                vCardParser.parse(is, mConstructor);
            } catch (VCardVersionException e1) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "constructImportRequest: ", e);
                }

                shouldUseV30 = true;
                is = resolver.openInputStream(localDataUri);
                vCardParser = new VCardParser_V30();
                try {
                    vCardParser.parse(is, mConstructor);
                } catch (VCardVersionException e2) {
                    throw new VCardException("vCard with unsupported version.");
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "constructImportRequest: ", e);
                    }
                }
            }

            vcardVersion = shouldUseV30 ? VCARD_VERSION_V30 : VCARD_VERSION_V21;
        } catch (VCardNestedException e) {
            Log.w(TAG, "Nested Exception is found (it may be false-positive).");
            // Go through without throwing the Exception, as we may be able to detect the
            // version before it
        }
        catch(IOException e){
            Log.e(TAG,"constructImportRequest: e", e);
        }
    }
}
