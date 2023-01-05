package com.ctb.vcardtest_project.vcard

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.Contacts
import android.provider.ContactsContract

class ReadLookupProcessor(private val context:Context, private val lookupUri:Uri) {

    val contactData: Array<String> get() {
            val resolver = context.contentResolver
            val contactUri = ensureIsContactUri(resolver, lookupUri)
            val lastSegment = contactUri.lastPathSegment!!.split(":").toTypedArray()
            val entityUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Entity.CONTENT_DIRECTORY)
            val cursor = resolver.query(
                entityUri, arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE,
                    ContactsContract.Data.DATA1,
                    ContactsContract.Data.DATA2,
                    ContactsContract.Data.DATA3
                ), null, null,
                ContactsContract.Contacts.Entity.RAW_CONTACT_ID
            )
            if (cursor != null && cursor.moveToFirst()) {
                val displayName = cursor.getString(0)
                cursor.close()
                return arrayOf(displayName, lastSegment.size.toString())
            }
            return arrayOf("null", "0")
        }

    @Throws(IllegalArgumentException::class)
    private fun ensureIsContactUri(resolver: ContentResolver, uri: Uri?): Uri {
        requireNotNull(uri) { "uri must not be null" }
        val authority = uri.authority
        // Current Style Uri?
        if (ContactsContract.AUTHORITY == authority) {
            val type = resolver.getType(uri)
            // Contact-Uri? Good, return it
            if (ContactsContract.Contacts.CONTENT_ITEM_TYPE == type) {
                return uri
            }
            // RawContact-Uri? Transform it to ContactUri
            if (ContactsContract.RawContacts.CONTENT_ITEM_TYPE == type) {
                val rawContactId = ContentUris.parseId(uri)
                return ContactsContract.RawContacts.getContactLookupUri(
                    resolver,
                    ContentUris.withAppendedId(
                        ContactsContract.RawContacts.CONTENT_URI,
                        rawContactId
                    )
                )
            }
            throw IllegalArgumentException("uri format is unknown")
        }

        // Legacy Style? Convert to RawContact
        val OBSOLETE_AUTHORITY = Contacts.AUTHORITY
        if (OBSOLETE_AUTHORITY == authority) {
            // Legacy Format. Convert to RawContact-Uri and then lookup the contact
            val rawContactId = ContentUris.parseId(uri)
            return ContactsContract.RawContacts.getContactLookupUri(
                resolver,
                ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId)
            )
        }
        throw IllegalArgumentException("uri authority is unknown")
    }
}