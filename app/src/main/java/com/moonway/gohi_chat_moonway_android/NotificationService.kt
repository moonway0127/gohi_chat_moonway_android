package com.moonway.gohi_chat_moonway_android

import android.app.Service
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import com.moonway.gohi.server.constant.Constant
import com.moonway.gohi.server.entity.LifeEntity
import com.moonway.gohi.server.entity.MsgEntity
import com.moonway.gohi.server.tools.CltToSvrConnection
import com.moonway.gohi.server.tools.NotificationFormat
import com.moonway.gohi_chat_moonway_android.map.NotificationSocketMap
import java.io.ObjectInput
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.Socket
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*

class NotificationService: Service() {



    var socket: Socket? = null

    init {
        this.socket = NotificationSocketMap.get().notificationSocket
    }


    var thread: Thread? = null
    override fun onCreate() {
        super.onCreate()
        println("通知线程")
        thread = object : Thread() {
            override fun run() {
                super.run()
                try {
                    while (true) {
                        var message: String = ObjectInputStream(socket!!.getInputStream()).readUTF()
//                        println("接收轮询监听开始:${SimpleDateFormat("HH:mm:ss").format(Date())}")
                        println(message)
                        if(message.equals("getNewMeaasge"))
                        NotificationSocketMap.get().lifeThreadNum = 9
                    }
                }catch (e:Exception){
                    socket!!.close()
                }
            }
        }
        thread!!.start()

    }

    override fun onBind(p0: Intent?): IBinder {
        return NotificationBinder()
    }

    class NotificationBinder : Binder() {
        fun getNotificationService(): NotificationService {
            return NotificationService()
        }
    }

    override fun unbindService(conn: ServiceConnection?) {
        super.unbindService(conn)
    }


}