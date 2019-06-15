package com.moonway.gohi_chat_moonway_android.map

import com.moonway.gohi_chat_moonway_android.LifeService
import io.flutter.view.FlutterView
import java.net.Socket

/**
 * @program: NotificationSocketMap
 *
 * @description: 存放所有在线用户通知线程的集合
 *
 * @author: moonway
 *
 * @school: University of Science and Technology Liaoning
 *
 * @create: 2019-01-09 14:27
 **/

class NotificationSocketMap private constructor() {


    val socketMap = mapOf<Long, Socket>().toMutableMap()
    val notificationMap = mapOf<Long,Boolean>().toMutableMap()
    val lifeMap = mapOf<Long,Boolean>().toMutableMap()
    //    val messageMap = mapOf<Long,LoginEntity>().toMutableMap()
    var flutterView:FlutterView? = null
    var userId:Long = 0
    var lifeService:LifeService? = null
    var notificationSocket:Socket? = null
    var lifeThreadNum = 0
    var lifeThread :Thread? = null
    companion object {

        const val TAG = "NotificationSocketMap"
        private var instance: NotificationSocketMap? = null
            get() {
                if (field == null) {
                    field = NotificationSocketMap()
                }
                return field
            }

        fun get(): NotificationSocketMap {
            return instance!!
        }
    }

}