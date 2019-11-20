package edu.uw.jw21.yama

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import android.content.pm.PackageManager
import android.Manifest.permission.SEND_SMS
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.compose_message.*

class ComposeActivity : AppCompatActivity() {
    private val REQUEST_SEND_SMS = 1
    private val REQUEST_PHONE_NUMBER = 1
    private lateinit var contactUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_message)
        contacts_btn.setOnClickListener {
            findContact()
        }
        send_btn.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(SEND_SMS), REQUEST_SEND_SMS)
            } else {
                sendSMSMessage()
            }
        }
    }

    private fun findContact() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        if (intent.resolveActivity(packageManager) != null)
            startActivityForResult(intent, REQUEST_PHONE_NUMBER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PHONE_NUMBER && resultCode == Activity.RESULT_OK) {
            contactUri = data!!.data!!
            getPhoneNumber()
        }
    }

    private fun getPhoneNumber() {
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val cursor = contentResolver.query(contactUri, projection, null, null, null)

        if ((cursor!!.moveToFirst())) {
            val phoneNumber =
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            number_edit.setText(phoneNumber)
        }
    }

    private fun sendSMSMessage() {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(
            number_edit.text.toString(),
            null,
            text_edit.text.toString(),
            null,
            null
        )
        text_edit.setText("")
        Toast.makeText(applicationContext, "SMS sent.", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_SEND_SMS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSMSMessage()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Send failed, please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}