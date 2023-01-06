package com.ctb.vcardtest_project

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.OpenableColumns
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.vcard.VCardEntry
import com.ctb.vcardtest_project.view.ContactAvatarDrawable.Companion.TYPE_NORMAL
import com.ctb.vcardtest_project.util.FileUtils.saveFile
import com.ctb.vcardtest_project.util.GraphicUtils.getCircularBitmap
import com.ctb.vcardtest_project.util.PermissionUtils.PERMISSIONS_REQUEST_CODE
import com.ctb.vcardtest_project.util.PermissionUtils.REQUIRED_PERMISSIONS
import com.ctb.vcardtest_project.util.PermissionUtils.hasRequiredPermission
import com.ctb.vcardtest_project.databinding.ActivityImportVcardBinding
import com.ctb.vcardtest_project.databinding.ItemVcardContactBinding
import com.ctb.vcardtest_project.view.ContactAvatarDrawable
import java.util.*


class ImportVCardActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "ImportVCardActivity"
        private const val PHONE_TYPE = 1
        private const val EMAIL_TYPE = 2
        private const val MULTI_TYPE = 3
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
        val fileUri = it.data?.data ?: return@ActivityResultCallback

        readVcard(fileUri)
    })

    private val callbackHandler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if(msg.obj !is ArrayList<*>) return
            val entries = msg.obj as ArrayList<VCardEntry>
            if(entries.size == 0){
               return
            }
            adapter.update(entries)
            binding.btnImportVcardSave.isEnabled = true
        }
    }

    private val binding : ActivityImportVcardBinding by lazy { ActivityImportVcardBinding.inflate(layoutInflater) }
    private val adapter = VCardContactListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if(!hasRequiredPermission(this)){
            requestPermissions(REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
        else{
            initView()
            openDocument()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var bGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    bGranted = false
                    break
                }
            }
            if (bGranted) {
                initView()
                openDocument()
            } else {
                finish()
            }
        }


    }

    private fun initView(){
        binding.rvImportVcardList.adapter = adapter
    }

    private fun openDocument(){
        val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
        fileIntent.type = "text/x-vcard"
        fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        openDocumentLauncher.launch(fileIntent)
    }

    private fun readVcard(uri: Uri){
        var displayName = ""
        val cursor: Cursor? = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        if(cursor!=null && cursor.count > 0 && cursor.moveToFirst()){
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if(index>0) displayName = cursor.getString(index)
            cursor.close()
        }
        if(displayName.isEmpty()){
            displayName = uri.lastPathSegment.toString()
        }
        val readVCardProcessor = ReadVCardProcessor(this, callbackHandler)
        readVCardProcessor.initialize(uri)
        readVCardProcessor.readVCard()

        binding.btnImportVcardSave.setOnClickListener {
            saveFile(this, displayName, uri)
        }
    }

    inner class VCardContactListAdapter : RecyclerView.Adapter<VCardContactListHolder>() {
        private val entries = ArrayList<VCardEntry>()
        fun update(newEntries: ArrayList<VCardEntry>){
            entries.clear()
            entries.addAll(newEntries)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VCardContactListHolder {
            return VCardContactListHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_vcard_contact, parent, false))
        }

        override fun onBindViewHolder(holder: VCardContactListHolder, position: Int) {
            holder.initDataBinding(entries[position])
        }

        override fun getItemCount(): Int {
            return entries.size
        }
    }

    inner class VCardContactListHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val binding = ItemVcardContactBinding.bind(itemView)
        private var type = PHONE_TYPE
        fun initDataBinding(entry: VCardEntry){
            val displayName = entry.displayName
            val phoneList = entry.phoneList
            val emailList = entry.emailList
            binding.tvVcardItemName.text = if (displayName == null || displayName.isEmpty()) itemView.context.getString(R.string.unknown_name) else displayName
            val isEmptyData = displayName == null || phoneList == null && emailList == null
            val isFullData = phoneList != null && emailList != null
            setAvatar(entry, displayName, entry.hashCode())
            if (!isEmptyData) {
                var content = ""
                if (isFullData) {
                    type = MULTI_TYPE
                    content += PhoneNumberUtils.formatNumber(phoneList!![0].number, Locale.getDefault().country) + " " + itemView.context.getString(R.string.contact_content_summary, phoneList.size + emailList!!.size - 1)
                } else if (phoneList != null) {
                    val phoneNumber = phoneList[0].number
                    content = if (phoneList.size > 1) { (PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().country) + " " + itemView.context.getString(
                            R.string.contact_content_summary,
                            phoneList.size - 1
                        )) }
                    else { PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().country) + " (" + phoneList[0].getPhoneLabel(Locale.getDefault()) + ")" }
                    type = PHONE_TYPE
                } else {
                    binding.tvVcardItemName.text = displayName
                    val email = emailList!![0].address
                    content = if (emailList.size > 1) {
                        email + itemView.context.getString(
                            R.string.contact_content_summary,
                            emailList.size - 1
                        )
                    } else {
                        email + " (" + emailList[0].getEmailLabel(Locale.getDefault()) + ")"
                    }
                    type = EMAIL_TYPE
                }
                binding.tvVcardItemMore.text = content
            } else {
                binding.tvVcardItemMore.visibility = View.GONE
            }
        }

        private fun setAvatar(entry: VCardEntry, displayName: String, hashCode: Int) {
            if (entry.photoList != null) {
                val bitmap = BitmapFactory.decodeByteArray(entry.photoList[0].bytes, 0, entry.photoList[0].bytes.size)
                binding.ivVcardContactAvatar.setImageBitmap(getCircularBitmap(bitmap))
            } else {
                val drawable = ContactAvatarDrawable(itemView.context, displayName, TYPE_NORMAL, hashCode, false)
                binding.ivVcardContactAvatar.setImageDrawable(drawable)
            }
        }
    }
}