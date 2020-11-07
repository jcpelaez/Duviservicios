package com.example.duviservicios.Services

import com.example.duviservicios.Commun.Commun
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MyFCMServices : FirebaseMessagingService() {
    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Commun.updateToken(this,p0)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val dataRecv = remoteMessage.data
        if(dataRecv != null)
        {
            Commun.showNotification(this, Random().nextInt(),
                dataRecv[Commun.NOTI_TITLE],
                dataRecv[Commun.NOTI_CONTENT],
                null)
        }
    }
}