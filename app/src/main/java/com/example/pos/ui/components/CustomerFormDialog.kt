package com.example.pos.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class ImportedContactData(
    val id: String = "",
    val name: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val email: String = "",
    val address: String = ""
) {
    val phone: String get() = phoneNumbers.firstOrNull().orEmpty()
}

private fun extractContactDetails(context: Context, contactUri: Uri): ImportedContactData {
    var name = ""
    val phoneList = mutableListOf<String>()
    var email = ""
    var address = ""
    var contactId: String? = null

    try {
        context.contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                if (idIndex != -1) contactId = cursor.getString(idIndex)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: ""
            }
        }

        if (contactId != null) {
            // 1. All Phone numbers
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { pCursor ->
                val numIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (pCursor.moveToNext()) {
                    if (numIdx != -1) {
                        val num = pCursor.getString(numIdx) ?: ""
                        val clean = num.trim()
                        if (clean.isNotBlank() && !phoneList.contains(clean)) {
                            phoneList.add(clean)
                        }
                    }
                }
            }

            // 2. Email
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { eCursor ->
                if (eCursor.moveToFirst()) {
                    val emailIdx = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                    if (emailIdx != -1) email = eCursor.getString(emailIdx) ?: ""
                }
            }

            // 3. Postal Address
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS),
                "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { addressCursor ->
                if (addressCursor.moveToFirst()) {
                    val addressIndex = addressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                    if (addressIndex != -1) address = addressCursor.getString(addressIndex) ?: ""
                }
            }
        }
    } catch (_: Exception) {
    }

    return ImportedContactData(
        id = contactId.orEmpty(),
        name = name,
        phoneNumbers = phoneList,
        email = email,
        address = address
    )
}

private fun searchDeviceContacts(context: Context, query: String): List<ImportedContactData> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()

    val hasPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasPerm) return emptyList()

    val contactsMap = linkedMapOf<String, Pair<String, MutableList<String>>>()

    try {
        val filterUri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(trimmed)
        )
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(
            filterUri,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext() && contactsMap.size < 12) {
                val id = if (idIdx != -1) cursor.getString(idIdx) ?: "" else ""
                val contactName = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                val rawNum = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                val clean = rawNum.trim()

                if (contactName.isNotBlank()) {
                    val key = id.ifBlank { contactName }
                    val entry = contactsMap.getOrPut(key) { Pair(contactName, mutableListOf()) }
                    if (clean.isNotBlank() && !entry.second.contains(clean)) {
                        entry.second.add(clean)
                    }
                }
            }
        }
    } catch (_: Exception) {}

    if (contactsMap.isEmpty()) {
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$trimmed%")
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext() && contactsMap.size < 12) {
                    val id = if (idIdx != -1) cursor.getString(idIdx) ?: "" else ""
                    val contactName = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                    val rawNum = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                    val clean = rawNum.trim()

                    if (contactName.isNotBlank()) {
                        val key = id.ifBlank { contactName }
                        val entry = contactsMap.getOrPut(key) { Pair(contactName, mutableListOf()) }
                        if (clean.isNotBlank() && !entry.second.contains(clean)) {
                            entry.second.add(clean)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    return contactsMap.map { (id, pair) ->
        ImportedContactData(
            id = id,
            name = pair.first,
            phoneNumbers = pair.second
        )
    }
}

private fun fetchAdditionalContactDetails(context: Context, contactId: String): Pair<String, String> {
    var email = ""
    var address = ""
    if (contactId.isBlank()) return Pair("", "")
    try {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { eCursor ->
            if (eCursor.moveToFirst()) {
                val emailIdx = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                if (emailIdx != -1) email = eCursor.getString(emailIdx) ?: ""
            }
        }
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS),
            "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { addressCursor ->
            if (addressCursor.moveToFirst()) {
                val addressIndex = addressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                if (addressIndex != -1) address = addressCursor.getString(addressIndex) ?: ""
            }
        }
    } catch (_: Exception) {}
    return Pair(email, address)
}

@Composable
private fun SelectPhoneNumberDialog(
    contact: ImportedContactData,
    onNumberSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isNepali) "फोन नम्बर छान्नुहोस्" else "Select Phone Number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (isNepali) "${contact.name} (${contact.phoneNumbers.size} वटा नम्बर भेटियो)" else "${contact.name} (${contact.phoneNumbers.size} numbers found)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00B67A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isNepali) "यस ग्राहकको प्रोफाइलसँग जोड्न चाहेको नम्बर छान्नुहोस्:" else "Please choose which number to link with this customer profile:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    contact.phoneNumbers.forEachIndexed { index, number ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNumberSelected(number) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0F2FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = number,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (isNepali) "फोन #${index + 1}" else "Phone #${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF00B67A)
                                ) {
                                    Text(
                                        text = if (isNepali) "छान्नुहोस्" else "Pick",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerFormDialog(
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        phone: String,
        email: String,
        creditLimitInCents: Long,
        paymentTermsDays: Int,
        gracePeriodDays: Int,
        notes: String,
        openingBalanceInCents: Long,
        openingBalanceType: String,
        asOfDate: String
    ) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    val todayFormatted = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Form fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var customerType by remember { mutableStateOf("INDIVIDUAL") } // "INDIVIDUAL" or "BUSINESS"
    var taxId by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    // Customer Profile Photo state (Gallery picker)
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val photoBitmap = remember(selectedPhotoUri) {
        selectedPhotoUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Log.e("CustomerFormDialog", "Failed to decode customer photo", e)
                null
            }
        }
    }

    // Opening Balance & Accounting fields
    var openingBalanceText by remember { mutableStateOf("") }
    var openingBalanceType by remember { mutableStateOf("TO_RECEIVE") } // "TO_RECEIVE" or "TO_GIVE"
    var asOfDate by remember { mutableStateOf(todayFormatted) }

    // Picked / suggested contact tracking
    var pickedContact by remember { mutableStateOf<ImportedContactData?>(null) }
    var pendingContactForNumberSelection by remember { mutableStateOf<ImportedContactData?>(null) }
    var isContactConfirmed by remember { mutableStateOf(false) }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { contactUri ->
            val imported = extractContactDetails(context, contactUri)
            if (imported.name.isNotBlank()) {
                name = imported.name
                nameError = null
            }
            if (imported.email.isNotBlank()) email = imported.email
            if (imported.address.isNotBlank()) address = imported.address

            if (imported.phoneNumbers.size > 1) {
                // DO NOT automatically insert the first number! Let user pick!
                pendingContactForNumberSelection = imported
            } else if (imported.phoneNumbers.size == 1) {
                phone = imported.phoneNumbers[0]
                pickedContact = imported
                isContactConfirmed = true
            } else {
                pickedContact = imported
                isContactConfirmed = true
            }
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        if (isGranted) {
            contactPickerLauncher.launch(null)
        }
    }

    val autoSearchPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    val onPickContact: () -> Unit = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        hasContactsPermission = hasPerm
        if (hasPerm) {
            contactPickerLauncher.launch(null)
        } else {
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val matchingContacts = remember(name, hasContactsPermission, isContactConfirmed) {
        if (!isContactConfirmed && name.trim().length >= 1 && hasContactsPermission) {
            searchDeviceContacts(context, name)
        } else {
            emptyList()
        }
    }

    // Date Picker Dialog setup
    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                asOfDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Credit terms
    var creditEnabled by remember { mutableStateOf(true) }
    var creditLimitText by remember { mutableStateOf("500.00") }
    var termsDaysText by remember { mutableStateOf("30") }
    var gracePeriodText by remember { mutableStateOf("7") }
    var notesText by remember { mutableStateOf("") }

    // Active Section Tab (0: Credit Info, 1: Additional Details)
    var selectedSectionTab by remember { mutableIntStateOf(0) }

    val initials = remember(name, pickedContact) {
        val raw = name.ifBlank { pickedContact?.name.orEmpty() }
        if (raw.isBlank()) "ST"
        else {
            val parts = raw.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            } else {
                raw.take(2).uppercase()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 680.dp)
                .heightIn(max = 720.dp)
                .testTag("customer_form_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Top Sticky Header with Close Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNepali) "नयाँ ग्राहक थप्नुहोस्" else "Add Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.cancel,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // 1. Profile Avatar with Gallery Photo Picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clickable { photoPickerLauncher.launch("image/*") }
                            ) {
                                // Large Circular Avatar
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155))
                                        .border(2.dp, Color(0xFF00B67A), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (photoBitmap != null) {
                                        Image(
                                            bitmap = photoBitmap.asImageBitmap(),
                                            contentDescription = if (isNepali) "ग्राहकको फोटो" else "Customer Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = if (isNepali) "ग्राहक अवतार" else "Customer Avatar",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(52.dp)
                                        )
                                    }
                                }

                                // Camera / Gallery Picker Badge at Bottom-Right
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00B67A))
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = if (isNepali) "ग्यालरीबाट फोटो छान्नुहोस्" else "Choose Photo from Gallery",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (selectedPhotoUri != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isNepali) "फोटो बदल्नुहोस्" else "Change Photo",
                                        color = Color(0xFF00B67A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable { photoPickerLauncher.launch("image/*") }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        text = "•",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                    Text(
                                        text = if (isNepali) "हटाउनुहोस्" else "Remove",
                                        color = PosRose,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable { selectedPhotoUri = null }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isNepali) "ग्यालरीबाट फोटो छान्न ट्याप गर्नुहोस्" else "Tap to choose photo from gallery",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Party Name Outlined Field (Matching Screenshot)
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = null
                            isContactConfirmed = false
                            if (pickedContact != null && it != pickedContact?.name) {
                                pickedContact = null
                            }
                        },
                        label = { Text(if (isNepali) "पार्टी / ग्राहकको नाम" else "Party Name", color = Color(0xFF00B67A), fontWeight = FontWeight.Medium) },
                        placeholder = { Text(if (isNepali) "जस्तै: सोम थेहित" else "e.g. Som Thehit") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        trailingIcon = {
                            IconButton(onClick = onPickContact) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = if (isNepali) "सम्पर्क सूचीबाट ल्याउनुहोस्" else "Import from Contacts",
                                    tint = Color(0xFF00B67A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_field")
                    )

                    // 3. Contact Suggestion / Auto-Search Results or Selected Card
                    if (matchingContacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isNepali) "मिल्दाजुल्दा सम्पर्कहरू (${matchingContacts.size})" else "Matching Contacts (${matchingContacts.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00B67A)
                                    )
                                    Text(
                                        text = if (isNepali) "छान्न सम्पर्क वा नम्बर थिच्नुहोस्" else "Tap contact or number to pick",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    matchingContacts.take(4).forEach { contact ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (contact.phoneNumbers.size > 1) {
                                                        // DO NOT insert first automatically! Let user choose!
                                                        pendingContactForNumberSelection = contact
                                                    } else if (contact.phoneNumbers.size == 1) {
                                                        name = contact.name
                                                        phone = contact.phoneNumbers[0]
                                                        pickedContact = contact
                                                        isContactConfirmed = true
                                                        val (extraEmail, extraAddress) = fetchAdditionalContactDetails(context, contact.id)
                                                        if (extraEmail.isNotBlank()) email = extraEmail
                                                        if (extraAddress.isNotBlank()) address = extraAddress
                                                    } else {
                                                        name = contact.name
                                                        pickedContact = contact
                                                        isContactConfirmed = true
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Initials
                                                val contactInitials = contact.name.trim().split("\\s+".toRegex()).let { parts ->
                                                    if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                                                    else contact.name.take(2).uppercase().ifBlank { "CN" }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFE2E8F0)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(contactInitials, fontWeight = FontWeight.Bold, color = Color(0xFF334155), fontSize = 13.sp)
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = contact.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )

                                                    if (contact.phoneNumbers.isEmpty()) {
                                                        Text(if (isNepali) "कुनै फोन नम्बर फेला परेन" else "No phone number attached", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                                    } else if (contact.phoneNumbers.size == 1) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF00B67A), modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = contact.phoneNumbers[0],
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFF00B67A),
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    } else {
                                                        // Multiple numbers: list each as a chip so user can tap directly
                                                        Text(
                                                            text = if (isNepali) "${contact.phoneNumbers.size} नम्बरहरू (छान्न ट्याप गर्नुहोस्):" else "${contact.phoneNumbers.size} numbers (Tap one to select):",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier
                                                                .horizontalScroll(rememberScrollState())
                                                                .padding(top = 3.dp)
                                                        ) {
                                                            contact.phoneNumbers.forEach { num ->
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = Color(0xFFF0FDF4),
                                                                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                                                    modifier = Modifier.clickable {
                                                                        name = contact.name
                                                                        phone = num
                                                                        pickedContact = contact
                                                                        isContactConfirmed = true
                                                                        val (extraEmail, extraAddress) = fetchAdditionalContactDetails(context, contact.id)
                                                                        if (extraEmail.isNotBlank()) email = extraEmail
                                                                        if (extraAddress.isNotBlank()) address = extraAddress
                                                                    }
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF00B67A), modifier = Modifier.size(11.dp))
                                                                        Spacer(modifier = Modifier.width(3.dp))
                                                                        Text(num, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                if (contact.phoneNumbers.size > 1) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFF1F5F9),
                                                        modifier = Modifier.clickable { pendingContactForNumberSelection = contact }
                                                    ) {
                                                        Text(
                                                            text = if (isNepali) "छान्नुहोस् ▾" else "Pick ▾",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0284C7),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!hasContactsPermission && name.trim().length >= 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { autoSearchPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContactPhone, contentDescription = null, tint = Color(0xFF00B67A), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isNepali) "फोन नम्बर स्वचालित खोज्न सम्पर्क अनुमति दिनुहोस्" else "Allow contacts to auto-match phone number", fontSize = 11.sp, color = Color(0xFF166534))
                                }
                                Text(if (isNepali) "अनुमति दिनुहोस्" else "Allow", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B67A))
                            }
                        }
                    } else if (name.isNotBlank() || pickedContact != null || phone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    // Avatar circle with initials
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155),
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name.ifBlank { pickedContact?.name ?: (if (isNepali) "ग्राहक" else "Customer") },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (phone.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF00B67A), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = phone,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF00B67A),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (pickedContact != null && pickedContact!!.phoneNumbers.size > 1) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFE0F2FE),
                                                        modifier = Modifier.clickable { pendingContactForNumberSelection = pickedContact }
                                                    ) {
                                                        Text(
                                                            text = if (isNepali) "बदल्नुहोस् (${pickedContact!!.phoneNumbers.size})" else "Change (${pickedContact!!.phoneNumbers.size})",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF0284C7),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = if (isNepali) "फोन जोडिएको छैन • थप्न थप विवरणमा जानुहोस्" else "No phone attached • Tap Additional Details to enter",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                if (pickedContact != null || isContactConfirmed) {
                                    IconButton(
                                        onClick = {
                                            pickedContact = null
                                            isContactConfirmed = false
                                            phone = ""
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = if (isNepali) "सम्पर्क हटाउनुहोस्" else "Clear Contact",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Tab Row: "Credit Info" & "Additional Details" (Matching Screenshot)
                    TabRow(
                        selectedTabIndex = selectedSectionTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF00B67A),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSectionTab]),
                                color = Color(0xFF00B67A),
                                height = 3.dp
                            )
                        },
                        divider = {
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedSectionTab == 0,
                            onClick = { selectedSectionTab = 0 },
                            text = {
                                Text(
                                    text = if (isNepali) "ऋण तथा खाता विवरण" else "Credit Info",
                                    fontWeight = if (selectedSectionTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSectionTab == 0) Color(0xFF00B67A) else Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedSectionTab == 1,
                            onClick = { selectedSectionTab = 1 },
                            text = {
                                Text(
                                    text = if (isNepali) "थप विवरण" else "Additional Details",
                                    fontWeight = if (selectedSectionTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSectionTab == 1) Color(0xFF00B67A) else Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Tab 0: Credit Info (Opening Balance, As of Date, To Receive / To Give, Credit Terms)
                    if (selectedSectionTab == 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Row: Opening Balance + As of Date (Matching Screenshot)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Opening Balance Field
                                OutlinedTextField(
                                    value = openingBalanceText,
                                    onValueChange = { openingBalanceText = it },
                                    label = { Text(if (isNepali) "सुरुवाती मौज्दात (Opening Balance)" else "Opening Balance") },
                                    placeholder = { Text("0.00") },
                                    leadingIcon = {
                                        Text(
                                            text = currencySymbol,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00B67A),
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("opening_balance_field")
                                )

                                // As of Date Field (with Calendar Picker)
                                OutlinedTextField(
                                    value = asOfDate,
                                    onValueChange = { asOfDate = it },
                                    label = { Text(if (isNepali) "मितिसम्म (As of Date)" else "As of Date") },
                                    trailingIcon = {
                                        IconButton(onClick = { datePickerDialog.show() }) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = if (isNepali) "मिति छान्नुहोस्" else "Pick Date",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("as_of_date_field")
                                )
                            }

                            // To Receive & To Give Mutually Exclusive Pill Buttons (Matching Screenshot)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // To Receive Pill
                                Surface(
                                    onClick = { openingBalanceType = "TO_RECEIVE" },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (openingBalanceType == "TO_RECEIVE") Color(0xFF00B67A) else Color(0xFFF1F5F9),
                                    modifier = Modifier.testTag("to_receive_button")
                                ) {
                                    Text(
                                        text = if (isNepali) "लिन बाँकी (To Receive)" else "To Receive",
                                        color = if (openingBalanceType == "TO_RECEIVE") Color.White else Color(0xFF334155),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp)
                                    )
                                }

                                // To Give Pill
                                Surface(
                                    onClick = { openingBalanceType = "TO_GIVE" },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (openingBalanceType == "TO_GIVE") Color(0xFF00B67A) else Color(0xFFF1F5F9),
                                    modifier = Modifier.testTag("to_give_button")
                                ) {
                                    Text(
                                        text = if (isNepali) "दिन बाँकी (To Give)" else "To Give",
                                        color = if (openingBalanceType == "TO_GIVE") Color.White else Color(0xFF334155),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp)
                                    )
                                }
                            }

                            // Accounting Explanation Banner (In Nepali & English)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (openingBalanceType == "TO_RECEIVE") Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (openingBalanceType == "TO_RECEIVE") Color(0xFF00B67A).copy(alpha = 0.35f) else Color(0xFF3B82F6).copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (openingBalanceType == "TO_RECEIVE") Color(0xFF00B67A) else Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (openingBalanceType == "TO_RECEIVE") {
                                                if (isNepali) "पैसा लिन बाँकी (Accounts Receivable / सम्पत्ति)" else "To Receive — पैसा लिन बाँकी (Accounts Receivable / Asset)"
                                            } else {
                                                if (isNepali) "पैसा दिन बाँकी (Customer Advance / दायित्व)" else "To Give — पैसा दिन बाँकी (Customer Advance / Liability)"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (openingBalanceType == "TO_RECEIVE") Color(0xFF065F46) else Color(0xFF1E40AF)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (openingBalanceType == "TO_RECEIVE") {
                                                if (isNepali) "ग्राहकले तपाईंलाई पैसा तिर्न बाँकी छ। सुरुवाती बाँकी रकम ग्राहकको लेजरमा प्राप्त गर्न बाँकी (Due) का रूपमा डेबिट हुनेछ।" else "Customer owes you money. Opening balance will be debited to customer's ledger as receivable."
                                            } else {
                                                if (isNepali) "तपाईंले ग्राहकलाई पैसा फिर्ता वा सामान दिन बाँकी छ। सुरुवाती रकम ग्राहकको advance/payable खातामा क्रेडिट हुनेछ।" else "You owe customer refund/goods. Opening balance will be credited as customer advance."
                                            },
                                            fontSize = 11.sp,
                                            color = if (openingBalanceType == "TO_RECEIVE") Color(0xFF047857) else Color(0xFF1D4ED8),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                            // Store Credit Allow Toggle
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isNepali) "उधारो बिक्री अनुमति दिनुहोस्" else "Allow Store Credit Sales",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isNepali) "उधारो सुविधा र ग्राहकको अधिकतम ऋण सीमा सक्रिय गर्नुहोस्" else "Enable buy-now-pay-later and customer credit limit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Switch(
                                        checked = creditEnabled,
                                        onCheckedChange = { creditEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00B67A))
                                    )
                                }
                            }

                            // Credit Limit & Payment Terms (if credit is enabled)
                            if (creditEnabled) {
                                OutlinedTextField(
                                    value = creditLimitText,
                                    onValueChange = { creditLimitText = it },
                                    label = { Text(if (isNepali) "ऋण सीमा (Credit Limit)" else "Credit Limit") },
                                    leadingIcon = {
                                        Text(
                                            text = currencySymbol,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00B67A),
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = termsDaysText,
                                        onValueChange = { termsDaysText = it },
                                        label = { Text(if (isNepali) "भुक्तानी म्याद" else "Payment Terms") },
                                        suffix = { Text(if (isNepali) "दिन" else "days", fontSize = 12.sp, color = Color(0xFF64748B)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = gracePeriodText,
                                        onValueChange = { gracePeriodText = it },
                                        label = { Text(if (isNepali) "थप छुट म्याद (Grace Period)" else "Grace Period") },
                                        suffix = { Text(if (isNepali) "दिन" else "days", fontSize = 12.sp, color = Color(0xFF64748B)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 6. Tab 1: Additional Details (Phone, Email, Address, Business Classification, Notes)
                    if (selectedSectionTab == 1) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Phone Field with Contact Picker
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(if (isNepali) "फोन नम्बर" else "Phone Number") },
                                placeholder = { Text("+977 98XXXXXXXX") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = onPickContact) {
                                        Icon(Icons.Default.ContactPhone, contentDescription = if (isNepali) "सम्पर्क छान्नुहोस्" else "Pick Contact", tint = Color(0xFF00B67A))
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_phone_field")
                            )

                            // Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text(if (isNepali) "इमेल ठेगाना" else "Email Address") },
                                placeholder = { Text("customer@example.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_email_field")
                            )

                            // Postal / Billing Address
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text(if (isNepali) "बिलिङ / डेलिभरी ठेगाना" else "Billing / Delivery Address") },
                                placeholder = { Text(if (isNepali) "टोल, वडा नं, सहर, नेपाल" else "Street, Ward, City, Nepal") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = false,
                                maxLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Account Type: Individual vs Business
                            Text(
                                text = if (isNepali) "ग्राहकको वर्गीकरण" else "Account Classification",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    onClick = { customerType = "INDIVIDUAL" },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (customerType == "INDIVIDUAL") Color(0xFF00B67A).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (customerType == "INDIVIDUAL") Color(0xFF00B67A) else Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (customerType == "INDIVIDUAL") Color(0xFF00B67A) else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isNepali) "व्यक्तिगत" else "Individual",
                                            fontWeight = FontWeight.Bold,
                                            color = if (customerType == "INDIVIDUAL") Color(0xFF00B67A) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { customerType = "BUSINESS" },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (customerType == "BUSINESS") Color(0xFF00B67A).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (customerType == "BUSINESS") Color(0xFF00B67A) else Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = if (customerType == "BUSINESS") Color(0xFF00B67A) else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isNepali) "व्यावसायिक (फर्म/कम्पनी)" else "Business",
                                            fontWeight = FontWeight.Bold,
                                            color = if (customerType == "BUSINESS") Color(0xFF00B67A) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Tax ID / PAN if Business
                            if (customerType == "BUSINESS") {
                                OutlinedTextField(
                                    value = taxId,
                                    onValueChange = { taxId = it },
                                    label = { Text(if (isNepali) "स्थायी लेखा नम्बर (PAN / VAT)" else "Tax ID / PAN Number") },
                                    placeholder = { Text("e.g. 601234567") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Notes
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                label = { Text(if (isNepali) "थप कैफियत / टिप्पणी" else "Additional Notes") },
                                placeholder = { Text(if (isNepali) "जमानी विवरण, भुक्तानी निर्देशन, टिप्पणी" else "Guarantor info, payment instructions, remarks") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Sticky Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedSectionTab == 0) {
                                PosOutlineButton(
                                    text = if (isNepali) "थप विवरण" else "Details",
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    height = 40.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    onClick = { selectedSectionTab = 1 }
                                )
                            } else {
                                PosOutlineButton(
                                    text = if (isNepali) "ऋण विवरण" else "Credit Info",
                                    height = 40.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    onClick = { selectedSectionTab = 0 }
                                )
                            }

                            PosCompactPillButton(
                                text = if (isNepali) "ग्राहक सुरक्षित गर्नुहोस्" else "Save Customer",
                                icon = Icons.Default.Check,
                                gradient = PosButtonGradients.Emerald,
                                height = 40.dp,
                                fontSize = 13.sp,
                                modifier = Modifier.testTag("save_customer_button"),
                                onClick = {
                                    var valid = true
                                    if (name.isBlank()) {
                                        nameError = if (isNepali) "ग्राहकको नाम अनिवार्य छ" else "Party name is required"
                                        valid = false
                                    }
                                    if (valid) {
                                        val limitCents = if (creditEnabled) {
                                            ((creditLimitText.toDoubleOrNull() ?: 500.0) * 100).toLong().coerceAtLeast(0L)
                                        } else {
                                            0L
                                        }
                                        val termsDays = termsDaysText.toIntOrNull() ?: 30
                                        val graceDays = gracePeriodText.toIntOrNull() ?: 7

                                        val openingBalCents = ((openingBalanceText.toDoubleOrNull() ?: 0.0) * 100).toLong().coerceAtLeast(0L)

                                        val combinedNotes = buildString {
                                            if (customerType == "BUSINESS" && taxId.isNotBlank()) append("Tax ID: $taxId; ")
                                            if (address.isNotBlank()) append("Address: $address; ")
                                            if (notesText.isNotBlank()) append("Notes: $notesText; ")
                                        }

                                        onSubmit(
                                            name.trim(),
                                            phone.trim(),
                                            email.trim(),
                                            limitCents,
                                            termsDays,
                                            graceDays,
                                            combinedNotes,
                                            openingBalCents,
                                            openingBalanceType,
                                            asOfDate.trim()
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingContactForNumberSelection?.let { contact ->
        SelectPhoneNumberDialog(
            contact = contact,
            onNumberSelected = { chosenNumber ->
                name = contact.name
                phone = chosenNumber
                pickedContact = contact
                isContactConfirmed = true
                val (extraEmail, extraAddress) = fetchAdditionalContactDetails(context, contact.id)
                if (extraEmail.isNotBlank()) email = extraEmail
                if (extraAddress.isNotBlank()) address = extraAddress
                pendingContactForNumberSelection = null
            },
            onDismiss = {
                pendingContactForNumberSelection = null
            }
        )
    }
}
