package com.symphonysimper.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data

class Contact(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val phone: String?,
    val whatsAppDataId: Long?,
)

fun Context.hasContactsPermission() =
    checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

private const val MAX_CONTACTS = 4

fun Context.searchContacts(query: String): List<Contact> {
    if (query.isBlank() || !hasContactsPermission()) return emptyList()
    data class Base(val id: Long, val lookup: String, val name: String)

    val bases = mutableListOf<Base>()
    contentResolver.query(
        Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(query.trim())),
        arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY),
        null, null, null,
    )?.use { c ->
        while (bases.size < MAX_CONTACTS && c.moveToNext()) {
            bases += Base(c.getLong(0), c.getString(1) ?: continue, c.getString(2) ?: continue)
        }
    }
    if (bases.isEmpty()) return emptyList()

    val phones = HashMap<Long, String>()
    val primary = HashSet<Long>()
    val whatsApp = HashMap<Long, Long>()
    val ids = bases.joinToString(",") { it.id.toString() }
    contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data.CONTACT_ID, Data.MIMETYPE, Data._ID, Phone.NUMBER, Data.IS_SUPER_PRIMARY),
        "${Data.CONTACT_ID} IN ($ids) AND ${Data.MIMETYPE} IN (?, ?)",
        arrayOf(Phone.CONTENT_ITEM_TYPE, WHATSAPP_MIME),
        null,
    )?.use { c ->
        while (c.moveToNext()) {
            val contactId = c.getLong(0)
            if (c.getString(1) == WHATSAPP_MIME) {
                whatsApp.putIfAbsent(contactId, c.getLong(2))
                continue
            }
            val number = c.getString(3) ?: continue
            val isPrimary = c.getInt(4) != 0
            if (contactId in phones && (!isPrimary || contactId in primary)) continue
            phones[contactId] = number
            if (isPrimary) primary += contactId
        }
    }
    return bases.map { b ->
        Contact(b.id, b.lookup, b.name, phones[b.id], whatsApp[b.id])
    }
}
