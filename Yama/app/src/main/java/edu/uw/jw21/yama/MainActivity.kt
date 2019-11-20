package edu.uw.jw21.yama

import android.Manifest.permission.READ_SMS
import android.content.Intent
import android.os.Bundle
import android.Manifest.permission.RECEIVE_SMS
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import android.content.pm.PackageManager
import android.provider.Telephony.Sms.Intents.getMessagesFromIntent
import android.telephony.SmsMessage
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import androidx.recyclerview.widget.LinearLayoutManager
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private lateinit var smsRecyclerView: RecyclerView
    private val REQUEST_RECEIVE_SMS = 1
    private val REQUEST_READ_SMS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (ActivityCompat.checkSelfPermission(
                this,
                RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(RECEIVE_SMS), REQUEST_RECEIVE_SMS)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(READ_SMS), REQUEST_READ_SMS)
        }

        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, ComposeActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.DATE_SENT),
            // It's probably Telephony.SMS.Inbox.PERSON instead of ADDRESS but PERSON is null on the emulator.
            null,
            null,
            Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
        )

        val messages = mutableListOf<SMS>()

        while (cursor!!.moveToNext()) {
            var temp = SMS(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getLong(2)
            )
            messages.add(temp)
        }

        setUpSMSRecyclerView(messages, this)
    }

    private fun setUpSMSRecyclerView(messages: List<SMS>, context: Context) {
        smsRecyclerView = findViewById(R.id.sms_recycler_view)

        val recyclerAdapter = SMSAdapter(messages, this)
        smsRecyclerView.adapter = recyclerAdapter

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        smsRecyclerView.layoutManager = layoutManager
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SMS(val message: String, val author: String, val time: Long)

    class SMSAdapter(private val smsList: List<SMS>, private val context: Context) :
        RecyclerView.Adapter<SMSAdapter.SMSViewHolder>() {

        var sdf = SimpleDateFormat("M/d/yyyy h:mm a")

        override fun onBindViewHolder(smsViewHolder: SMSViewHolder, index: Int) {
            smsViewHolder.message.text = smsList[index].message
            smsViewHolder.sender.text = smsList[index].author
            smsViewHolder.time.text = sdf.format(Date(smsList[index].time))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMSViewHolder {
            return SMSViewHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.message_item,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return smsList.size
        }

        inner class SMSViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val message: TextView = view.findViewById(R.id.msg_content)
            val sender: TextView = view.findViewById(R.id.sender)
            val time: TextView = view.findViewById(R.id.time_sent)
        }
    }

    class SmsReceiver : BroadcastReceiver() {
        var messages = arrayOf<SmsMessage>()


        override fun onReceive(context: Context, intent: Intent) {

            val viewIntent = Intent(context, MainActivity::class.java)
            val viewPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(viewIntent)
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            val replyIntent = Intent(context, ComposeActivity::class.java)
            val replyPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(replyIntent)
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                messages = getMessagesFromIntent(intent)
                val builder = NotificationCompat.Builder(context, "my_channel_id")
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setContentTitle(messages[0].displayOriginatingAddress)
                    .setAutoCancel(true)
                    .setContentIntent(viewPendingIntent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(messages[0].displayMessageBody))
                    .addAction(android.R.drawable.ic_menu_view, "View", viewPendingIntent)
                    .addAction(android.R.drawable.ic_menu_revert, "Reply", replyPendingIntent)

                NotificationManagerCompat.from(context).notify(messages[0].timestampMillis.toInt(), builder.build())
            }
        }
    }
}

