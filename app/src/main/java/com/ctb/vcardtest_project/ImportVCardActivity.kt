package com.ctb.vcardtest_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class ImportVCardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_vcard)
        val file = Intent(Intent.ACTION_PICK)
        startActivityForResult(file, 0)
    }
}