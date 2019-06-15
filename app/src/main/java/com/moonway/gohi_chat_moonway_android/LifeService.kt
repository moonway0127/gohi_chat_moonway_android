package com.moonway.gohi_chat_moonway_android

import android.app.Service
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.moonway.gohi.server.constant.Constant
import com.moonway.gohi.server.entity.HeadRequestBackEntity
import com.moonway.gohi.server.entity.LifeEntity
import com.moonway.gohi.server.entity.MsgEntity
import com.moonway.gohi.server.tools.CltToSvrConnection
import com.moonway.gohi.server.tools.NotificationFormat
import com.moonway.gohi_chat_moonway_android.map.NotificationSocketMap
import io.flutter.plugin.common.BasicMessageChannel
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.StringCodec
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.xml.datatype.DatatypeConstants.SECONDS

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser

class LifeService : Service() {





    var thread: Thread? = null
    override fun onCreate() {
        super.onCreate()
        println("心跳线程")
        val basicMessageChannel = BasicMessageChannel<String>(NotificationSocketMap.get().flutterView, "com.moonway.gohi_chat.android/heart",
            StringCodec.INSTANCE)

        thread = object : Thread() {



            override fun run() {
                super.run()

                while (true) {

//                    println("向服务器发送心跳 bilibili！~")
                    try {
                        var socket = CltToSvrConnection.Connection()
                        var bfw = ObjectOutputStream(socket.getOutputStream())
                        bfw.writeObject(
                            MsgEntity(
                                Constant.MSG_FLAG_HEARTBEAT,
                                LifeEntity(NotificationSocketMap.get().userId) as Object
                            )
                        )
                        bfw.flush()
//                        println("发送完成等待回应 dididididi!~")
                        //设置5秒超时
                        socket.soTimeout = 5000
                        var message = ObjectInputStream(socket.getInputStream()).readObject() as HeadRequestBackEntity
//                        println("接收到消息 bibibibi！~")


                        if (Constant.MSG_FLAG_HEARTBEAT == message.msgFlag) {
                            var json = Gson().toJson(message)
                            println(json)
                            basicMessageChannel.send(json)
                        } else {
                            println("心跳回复错误，预计停止线程")
                        }
                    } catch (e: ConnectException) {
                        println("连接网络失败，进入离线状态")
                    } catch (e: SocketTimeoutException) {
                        println("连接网络超时，进入离线状态")
                    }

                    try {
                        //10秒心跳
                        while (NotificationSocketMap.get().lifeThreadNum in 0..9) {

                            Thread.sleep(1000)
                            NotificationSocketMap.get().lifeThreadNum++
                            println(NotificationSocketMap.get().lifeThreadNum)

                        }
                        NotificationSocketMap.get().lifeThreadNum = 0
                    }catch (e:Exception){
                        break
                    }



                }
            }
        }


        thread!!.start()
//        EventChannel(NotificationSocketMap.get().flutterView, "com.moonway.gohi_android/testToast")
//            .setStreamHandler(object : EventChannel.StreamHandler {
//                override fun onListen(o: Any?, eventSink: EventChannel.EventSink) {
//                    eventSink.success("11111111")
//                }
//
//                override fun onCancel(o: Any?) {
//                    println("取消")
//                }
//            })



    }

    override fun onBind(p0: Intent?): IBinder {
        return LifeBinder()
    }

    class LifeBinder : Binder() {
        fun getLifeService(): LifeService {
            return LifeService()
        }
    }

    override fun unbindService(conn: ServiceConnection?) {
        super.unbindService(conn)
    }

    override fun onDestroy() {
        super.onDestroy()
        println("销毁服务")
        thread!!.interrupt()
    }
}