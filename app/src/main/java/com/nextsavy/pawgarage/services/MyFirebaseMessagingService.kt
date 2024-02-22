package com.nextsavy.pawgarage.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.Timestamp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.fragments.AnimalProfileFragmentArgs
import com.nextsavy.pawgarage.fragments.NewProfileFragmentArgs
import com.nextsavy.pawgarage.models.ProfileLeadDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionNotifications

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e("NOTIFICATION", "token-$token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // This method called when app is in foreground and notification received.
        // If app is in background, this method will not called and notification will handled by intent in MainActivity.
        Log.e("NOTIFICATION", "In onMessageReceived - message-${message.data["animal_doc_id"]}")
        Log.e("NOTIFICATION", "In onMessageReceived - message-${message.data["reminder_doc_id"]}")
        Log.e("NOTIFICATION", "In onMessageReceived - message-${message.data["animal_name"]}")
        Log.e("NOTIFICATION", "In onMessageReceived - message-${message.data["animal_image_url"]}")
        Log.e("NOTIFICATION", "In onMessageReceived - message-${message.data["location"]}")
        Log.e("NOTIFICATION", "In onMessageReceived - message-${message.data["latitude"]}")
        Log.e("NOTIFICATION", "In onMessageReceived - message-${message.data["longitude"]}")

        if (message.notification != null) {
            val notificationType: String? = message.data["notification_type"]
            val notificationVersion: String? = message.data["notification_version"]
            val pendingIntent = if (notificationType == CollectionNotifications.PROFILE_LEADS && notificationVersion == "v2") {
                buildPendingIntent2(message)
            } else {
                buildPendingIntent(message)
            }

            val channelId = getString(R.string.paw_garage_notification_channel_id)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_paw_transparent)
                .setColor(ContextCompat.getColor(AppDelegate.applicationContext()!!, R.color.primary_color))
                .setContentTitle(message.notification!!.title)
                .setContentText(message.notification!!.body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(1 /* ID of notification */, notificationBuilder.build())
        }
    }

    private fun buildPendingIntent(message: RemoteMessage): PendingIntent? {
        val animalDocId = message.data["animal_doc_id"]
        val pendingIntent: PendingIntent?
        if (animalDocId == "") {
            val profileLeadDTO = ProfileLeadDTO(
                id = "",
                animalId = "",
                name = message.data["animal_name"] as String,
                downloadUrl = message.data["animal_image_url"] as String,
                address = message.data["location"] as String,
                latitude = (message.data["latitude"] as String).toDoubleOrNull() ?: 0.0,
                longitude = (message.data["longitude"] as String).toDoubleOrNull() ?: 0.0,
                isArchive = false,
                createdAt = Timestamp.now(),
                createdBy = ""
            )
            pendingIntent = AppDelegate.applicationContext()?.let {
                NavDeepLinkBuilder(it)
                    .setGraph(R.navigation.nav_graph)
                    .setDestination(R.id.newProfileFragment)
                    .setArguments(NewProfileFragmentArgs(profileLead = profileLeadDTO).toBundle())
                    .createPendingIntent()
            }
        } else {
            pendingIntent = AppDelegate.applicationContext()?.let {
                NavDeepLinkBuilder(it)
                    .setGraph(R.navigation.nav_graph)
                    .setDestination(R.id.animalProfileFragment)
                    .setArguments(AnimalProfileFragmentArgs(animalDocId).toBundle())
                    .createPendingIntent()
            }
        }

        return pendingIntent
    }

    private fun buildPendingIntent2(message: RemoteMessage): PendingIntent? {
        // v2
        val profileLeadDTO = ProfileLeadDTO(
            id = message.data["profile_lead_doc_id"] as String,
            animalId = "",
            name = message.data["animal_name"] as String,
            downloadUrl = message.data["animal_image_url"] as String,
            address = message.data["location"] as String,
            latitude = (message.data["latitude"] as String).toDoubleOrNull() ?: 0.0,
            longitude = (message.data["longitude"] as String).toDoubleOrNull() ?: 0.0,
            isArchive = false,
            createdAt = Timestamp.now(),
            createdBy = ""
        )

        val pendingIntent = AppDelegate.applicationContext()?.let {
            NavDeepLinkBuilder(it)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.newProfileFragment)
                .setArguments(NewProfileFragmentArgs(profileLead = profileLeadDTO).toBundle())
                .createPendingIntent()
        }

        return pendingIntent
    }

}