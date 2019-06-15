package com.moonway.gohi_chat_moonway_android

import android.app.AlertDialog
import android.app.Dialog
import android.app.Service
import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.widget.FrameLayout
import android.widget.Toast
import com.moonway.gohi.server.constant.Constant
import com.moonway.gohi.server.tools.CltToSvrConnection
import com.moonway.gohi.server.tools.NotificationFormat
import com.tencent.cos.xml.CosXmlService
import io.flutter.facade.Flutter
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMethodCodec
import io.flutter.view.FlutterView
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import com.tencent.cos.xml.CosXmlServiceConfig
import com.tencent.cos.xml.exception.CosXmlClientException
import com.tencent.cos.xml.exception.CosXmlServiceException
import com.tencent.cos.xml.listener.CosXmlBooleanListener
import com.tencent.cos.xml.listener.CosXmlResultListener
import com.tencent.cos.xml.model.CosXmlRequest
import com.tencent.cos.xml.model.CosXmlResult
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider
import com.tencent.qcloud.core.auth.QCloudCredentialProvider
import com.tencent.cos.xml.transfer.TransferConfig
import com.tencent.cos.xml.transfer.TransferManager
import com.tencent.cos.xml.transfer.COSXMLUploadTask
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.moonway.gohi.server.entity.*

import com.moonway.gohi_chat_moonway_android.map.NotificationSocketMap
import java.net.Socket
import java.util.*


class MainActivity : AppCompatActivity() {
    var lifeServiceConnection:LifeServiceConnection? = null
    var notificationConnection:NotificationConnection? = null
    var notificationSocket: Socket? = null
    val HANDLER_LOGIN = 1
    val HANDLER_REGIST = 2
    val HANDLER_USERINFO = 3
    val HANDLER_USERINFOUPDATE = 4
    val HANDLER_ADDUSER = 5
    var uid = 0
    var appid = "1254406116"
    var region = "ap-beijing"

    var secretId = "AKID73kDRQcex2tte7CXPEo9E6pKgojbNfuZ"
    var secretKey = "aAYhpldKvIUZucTAWyCmFTSCi1xliR0M"

    var transferManager: TransferManager? = null


    var serviceConfig = CosXmlServiceConfig.Builder()
        .setAppidAndRegion(appid, region)
        .setDebuggable(true)
        .builder()

    var credentialProvider: QCloudCredentialProvider = ShortTimeCredentialProvider(
        secretId,
        secretKey, 300
    )


    var handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                HANDLER_LOGIN -> {
                    var handlerEntity = msg.obj as HandlerEntity
                    println("native:${handlerEntity.message as String}")
                    var message_content = NotificationFormat.ReleaseString(handlerEntity.message.toString())
                    println(message_content)
                    if (message_content.equals(Constant.LOGIN_FLAG_SUCCESS.toString())) {
                        //返回的结果是登录成功的flag 启动心跳线程 并提示登录成功
//                        startService(Intent().setClass(this@MainActivity,NotificationService(handlerEntity.socket!!).javaClass))
                        selectUser(handlerEntity.result!!, uid).start()
//                       var intent = Intent(this@MainActivity,LifeService.javaClass)
//                        var bl : Boolean = bindService(intent,LifeServiceConnection(), Context.BIND_AUTO_CREATE)
//                        println("服务启动"+bl)

                        var lifeIntent = Intent()
                        lifeIntent.setClass(this@MainActivity,LifeService::class.java)
                        lifeServiceConnection = LifeServiceConnection()
                       bindService(lifeIntent,lifeServiceConnection!!,Service.BIND_AUTO_CREATE)

                        var notificationIntent = Intent()
                        notificationIntent.setClass(this@MainActivity,NotificationService::class.java)
                        notificationConnection = NotificationConnection(notificationSocket!!)
                        bindService(notificationIntent,notificationConnection,Service.BIND_AUTO_CREATE)

                    } else if (message_content.equals(Constant.LOGIN_FLAG_ERROR.toString())) {
                        //返回的结果是登录失败的flag 无法登陆成功
                        println("login error")
                        handlerEntity.result!!.error("登录失败", "error", "错误")
                    }

                }
                HANDLER_REGIST -> {
                    var handlerEntity = msg.obj as HandlerEntity
                    var registerEntity = handlerEntity.message as RegisterEntity
                    if (registerEntity != null && registerEntity!!.flag!! >= 1) {
                        //TODO 注册成功
                        AlertDialog.Builder(this@MainActivity).setTitle("提示").setMessage("您的账号：${registerEntity.uid}")
                            .setPositiveButton("已牢记", object : DialogInterface.OnClickListener {
                                override fun onClick(p0: DialogInterface?, p1: Int) {
                                    handlerEntity.result!!.success("注册成功")
                                }
                            }).show()
                    } else {
                        //TODO 注册失败

                        AlertDialog.Builder(this@MainActivity).setTitle("提示").setMessage("注册失败")
                            .setPositiveButton("了解", object : DialogInterface.OnClickListener {
                                override fun onClick(p0: DialogInterface?, p1: Int) {
                                    handlerEntity.result!!.error("注册失败", "error", "错误")
                                }
                            }).show()


                    }
                }
                HANDLER_USERINFO -> {
                    var handlerEntity = msg.obj as HandlerEntity
                    var userInfo = handlerEntity.message as UserEntity
                    if (userInfo != null && userInfo.phone != "") {
                        var json = Gson().toJson(userInfo)
                        println(json)
                        handlerEntity.result!!.success(json)
                    } else {

                    }
                }
                HANDLER_USERINFOUPDATE ->{
                    var handlerEntity = msg.obj as HandlerEntity
                    println("native:${handlerEntity.message as String}")
                    var message_content = NotificationFormat.ReleaseString(handlerEntity.message.toString())
                    println(message_content)
                    if (message_content.equals(Constant.SEARCH_FLAG_HAVE.toString())) {

                        handlerEntity.result!!.success("修改成功")
//
                    } else if (message_content.equals(Constant.LOGIN_FLAG_ERROR.toString())) {

                        println("change error")
                        handlerEntity.result!!.error("失败", "error", "错误")
                    }else{
                        println("未知错误")
                        handlerEntity.result!!.error("失败", "error", "错误")
                    }
                }
                HANDLER_ADDUSER ->{
                    var handlerEntity = msg.obj as HandlerEntity
                    println("native:${handlerEntity.message as String}")
                    var message_content = NotificationFormat.ReleaseString(handlerEntity.message.toString())
                    if(message_content.equals(Constant.SEARCH_FLAG_HAVE.toString())){
                        handlerEntity.result!!.success("请等待对方同意")
                    }else{
                        println("未知错误")
                        handlerEntity.result!!.error("登录失败", "error", "错误")
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        print("----------------------")
        val cosXmlService = CosXmlService(this, serviceConfig, credentialProvider)


            val transferConfig = TransferConfig.Builder().build()
            transferManager = TransferManager(cosXmlService, transferConfig)



        var flutter = Flutter.createView(this, lifecycle, "")
        var layoutParams =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        addContentView(flutter, layoutParams)

        addFlutterMethodListener(flutter)
    }


    class LifeServiceConnection :ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            //To change body of created functions use File | Settings | File Templates.
            var lifeBinder:LifeService.LifeBinder = p1 as LifeService.LifeBinder
            var lifeService = lifeBinder.getLifeService()
            NotificationSocketMap.get().lifeService = lifeService
        }

        override fun onServiceDisconnected(p0: ComponentName?) {

        }
    }


    class NotificationConnection(socket: Socket):ServiceConnection{
        var socket:Socket? = null
        init {
            this.socket = socket
        }
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        var notificationBinder:NotificationService.NotificationBinder = p1 as NotificationService.NotificationBinder
        var notificationService = notificationBinder.getNotificationService()

        }

        override fun onServiceDisconnected(p0: ComponentName?) {

        }
    }


    fun addFlutterMethodListener(flutterView: FlutterView) {
        NotificationSocketMap.get().flutterView = flutterView
        MethodChannel(
            flutterView, "com.moonway.gohi_android/method",
            StandardMethodCodec.INSTANCE
        )
            .setMethodCallHandler { methodCall, callResult ->
                when (methodCall.method) {
                    "login" -> {

                        val userId = methodCall.argument<Int>("userId")
                        val userPwd = methodCall.argument<String>("userPwd")
                        uid = userId!!
                        println("native:1")
                        loginThread(callResult, userId!!, userPwd!!).start()
                    }

                    "uploadHead" -> {
                        val path = methodCall.argument<String>("path")
                        val pwd = methodCall!!.argument<String>("pwd")
                        val nickname = methodCall.argument<String>("nikename")
                        val school = methodCall.argument<String>("school")
                        val address = methodCall.argument<String>("address")
                        val email = methodCall.argument<String>("email")
                        val phone = methodCall.argument<String>("phone")
                        val sex = methodCall.argument<String>("sex")
                        val year = methodCall.argument<Int>("year")
                        val month = methodCall.argument<Int>("month")
                        val date = methodCall.argument<Int>("date")

                        println("native:$path")
                        val bucket = "gohi-chat-1254406116"
                        val cosPath = "user_head/$phone${Date().time}.jpg" //即存储到 COS 上的绝对路径, 格式如 cosPath = "test.txt";
                        val uploadId: String? = null //若存在初始化分片上传的 UploadId，则赋值对应uploadId值用于续传，否则，赋值null。
                        val cosxmlUploadTask = transferManager!!.upload(bucket, cosPath, path, uploadId)

                        cosxmlUploadTask.setCosXmlResultListener(object : CosXmlResultListener {
                            override fun onSuccess(request: CosXmlRequest?, result: CosXmlResult?) {
                                println(result!!.accessUrl)
                                registThread(
                                    callResult, result!!.accessUrl, pwd!!, nickname!!, school!!,
                                    address!!, email!!, phone!!,
                                    sex!!, year!!, month!!,
                                    date!!
                                ).start()
                            }
                            override fun onFail(
                                request: CosXmlRequest?,
                                exception: CosXmlClientException?,
                                serviceException: CosXmlServiceException?
                            ) {
                                //TODO error
                            }
                        })
                    }

                    "SelectUserById" -> {
                        val uid = methodCall.argument<Int>("userId")
                        selectUser(callResult, uid!!).start()
                    }
                    "userInfoUpdate" -> {
                        val uid = methodCall.argument<Int>("uid")
                        val path = methodCall.argument<String>("path")
                        val nickname = methodCall.argument<String>("nikename")
                        val school = methodCall.argument<String>("school")
                        val address = methodCall.argument<String>("address")
                        val email = methodCall.argument<String>("email")
                        val phone = methodCall.argument<String>("phone")
                        val sex = methodCall.argument<Int>("sex")
                        val year = methodCall.argument<Int>("year")
                        val month = methodCall.argument<Int>("month")
                        val date = methodCall.argument<Int>("date")

                        userInfoUpdate(uid!!,callResult,path!!,nickname!!,school!!,address!!,email!!,phone!!,sex!!,year!!,month!!,date!!).start()
                    }
                    "userHeadImageChange" ->{
                        val path = methodCall.argument<String>("path")
                        val uid = methodCall.argument<Int>("uid")
                        val bucket = "gohi-chat-1254406116"
                        val cosPath = "user_head/$uid${Date().time}.jpg" //即存储到 COS 上的绝对路径, 格式如 cosPath = "test.txt";
                        val uploadId: String? = null //若存在初始化分片上传的 UploadId，则赋值对应uploadId值用于续传，否则，赋值null。
                        val cosxmlUploadTask = transferManager!!.upload(bucket, cosPath, path, uploadId)

                        cosxmlUploadTask.setCosXmlResultListener(object : CosXmlResultListener {
                            override fun onSuccess(request: CosXmlRequest?, result: CosXmlResult?) {

                                callResult.success(result!!.accessUrl)
                            }
                            override fun onFail(
                                request: CosXmlRequest?,
                                exception: CosXmlClientException?,
                                serviceException: CosXmlServiceException?
                            ) {
                                //TODO error
                                callResult.error("请求失败","","")
                            }
                        })

                    }
                    "friendAdd" ->{
                        val uid_from = methodCall.argument<Int>("uid_from")
                        val uid_to = methodCall.argument<Int>("uid_to")
                        addFriend(callResult,uid_from!!,uid_to!!).start()
                    }
                    "friendAcceptAndRefuse" ->{

                        val uid_friend = methodCall.argument<Int>("uid_friend")
                        val uid = methodCall.argument<Int>("uid")
                        val accpetOrRefuseFlag = methodCall.argument<Int>("accpetOrRefuseFlag")
                        val notificationId = methodCall.argument<String>("notificationId")
                        acceptOrRefuse(callResult,uid!!,uid_friend!!,accpetOrRefuseFlag!!,notificationId!!).start()


                    }
                    "sendMessage" ->{
                        val uid_friend = methodCall.argument<Int>("uid_friend")
                        val uid = methodCall.argument<Int>("uid")
                        val content = methodCall.argument<String>("content")
                        sendMessage(uid!!,uid_friend!!,content!!).start()
                        callResult.success("发送成功")
                    }
                    "clearMessage" ->{
                        val uid = methodCall.argument<Int>("uid")
                        val uid_friend = methodCall.argument<Int>("uid_friend")
                        clearMessage(uid!!,uid_friend!!).start()

                    }
                    "deleteFriend"->{
                        val uid = methodCall.argument<Int>("uid")
                        val uid_friend = methodCall.argument<Int>("uid_friend")
                        deleteFriend(uid!!,uid_friend!!).start()
                    }
                    "logout"->{

                        unbindService(lifeServiceConnection)
                        unbindService(notificationConnection)

                    }
                    else -> {
                        // ...
                    }
                }
            }
    }

    fun loginThread(result: MethodChannel.Result, uid: Int, pwd: String): Thread {
        val thread = object : Thread() {
            override fun run() {
                println("loginThread start")
                super.run()
                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_LOGIN_AND_NOTIFICATION,
                        LoginEntity(uid.toLong(), pwd) as Object
                    )
                )
                bfw.flush()
                var message: String = ObjectInputStream(socket.getInputStream()).readUTF()
                notificationSocket = socket
                NotificationSocketMap.get().notificationSocket = notificationSocket
                NotificationSocketMap.get().userId = uid.toLong()
                var msg = Message()
                msg.what = HANDLER_LOGIN
                msg.obj = HandlerEntity(result, message as Object,socket)
                handler.sendMessage(msg)
            }
        }
        return thread
    }


    fun selectUser(result: MethodChannel.Result, uid: Int): Thread {
        val thread = object : Thread() {
            override fun run() {
                println("registThread start")
                super.run()
                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_SELECTUSERBYID,
                        uid.toLong() as Object
                    )
                )
                bfw.flush()
                var message = ObjectInputStream(socket.getInputStream()).readObject() as UserEntity
                println("-----${message.uid}")
                var msg = Message()
                msg.what = HANDLER_USERINFO
                msg.obj = HandlerEntity(result, message as Object)
                handler.sendMessage(msg)
                bfw.close()
            }
        }
        return thread
    }



    fun sendMessage( uid: Int,friendId:Int,content:String): Thread {
        val thread = object : Thread() {
            override fun run() {
                println("registThread start")
                super.run()
                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                var chatContentEntity = ChatContentEntity()
                var chatContent = ChatContent()
                chatContent.content = content
                chatContent.sender = uid.toLong()
                chatContent.time = Date().time.toString()
                chatContentEntity.friendId = friendId.toLong()
                chatContentEntity.chatContentList.add(chatContent)

                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_SEND_CHAT,
                        chatContentEntity as Object
                    )
                )
                bfw.flush()
                var message:String = ObjectInputStream(socket.getInputStream()).readUTF()
                bfw.close()
            }
        }
        return thread
    }


    fun deleteFriend( uid: Int,friendId:Int): Thread {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                var chatContentEntity = ChatContentEntity()
                var chatContent = ChatContent()

                chatContent.sender = uid.toLong()
                chatContent.time = Date().time.toString()
                chatContentEntity.friendId = friendId.toLong()
                chatContentEntity.chatContentList.add(chatContent)

                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_DELETE_FRIEND,
                        chatContentEntity as Object
                    )
                )
                bfw.flush()
                var message:String = ObjectInputStream(socket.getInputStream()).readUTF()
                bfw.close()
            }
        }
        return thread
    }




    fun clearMessage( uid: Int,friendId:Int): Thread {
        val thread = object : Thread() {
            override fun run() {
                println("registThread start")
                super.run()
                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                var chatContentEntity = ChatContentEntity()
                var chatContent = ChatContent()

                chatContent.sender = uid.toLong()
                chatContent.time = Date().time.toString()
                chatContentEntity.friendId = friendId.toLong()
                chatContentEntity.chatContentList.add(chatContent)

                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_CLEAR_CHAT,
                        chatContentEntity as Object
                    )
                )
                bfw.flush()
                var message:String = ObjectInputStream(socket.getInputStream()).readUTF()
                bfw.close()
            }
        }
        return thread
    }


    fun addFriend(result: MethodChannel.Result,uid_from: Int,uid_to:Int):Thread{
        var thread = object:Thread(){
            override fun run() {
                super.run()

                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                var userNotificationEntity = UserNotificationEntity()
                userNotificationEntity.uid_from = uid_from.toLong()
                userNotificationEntity.uid_to = uid_to.toLong()
                userNotificationEntity.time = Date().time.toString()
                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_ADDUSER,
                        userNotificationEntity as Object
                    )
                )
                bfw.flush()
                var message:String = ObjectInputStream(socket.getInputStream()).readUTF()
                println("message:$message")
                var msg = Message()
                msg.what = HANDLER_ADDUSER
                msg.obj = HandlerEntity(result, message as Object)
                handler.sendMessage(msg)
                bfw.close()
            }
        }
        return thread
    }



    fun acceptOrRefuse(result: MethodChannel.Result,uid: Int,uid_friend:Int,flag:Int,notificationId:String):Thread{
        var thread = object:Thread(){
            override fun run() {
                super.run()

                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                var acceptOrRefuceEntity = AcceptOrRefuceEntity()
                acceptOrRefuceEntity.flag = flag
                acceptOrRefuceEntity.time = Date().time.toString()
                acceptOrRefuceEntity.uid = uid.toLong()
                acceptOrRefuceEntity.uid_friend = uid_friend.toLong()
                acceptOrRefuceEntity.notificationId = notificationId
                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_ACCEPTORREFUSE_FRIEND,
                        acceptOrRefuceEntity as Object
                    )
                )
                bfw.flush()
                var message:String = ObjectInputStream(socket.getInputStream()).readUTF()
                println("message:$message")
                result.success("完成")
//                var msg = Message()
//                msg.what = HANDLER_ADDUSER
//                msg.obj = HandlerEntity(result, message as Object)
//                handler.sendMessage(msg)
                bfw.close()
            }
        }
        return thread
    }



    fun userInfoUpdate(
        uid: Int,
        result: MethodChannel.Result,
        headImg: String,
        nickname: String,
        school: String,
        address: String,
        email: String,
        phone: String,
        sex: Int,
        year: Int,
        month: Int,
        date: Int
    ): Thread {
        var thread = object : Thread() {
            override fun run() {
                super.run()
                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_CHANGEINFO,
                        UserEntity(
                            nickname!!,
                            sex!!,
                            headImg!!,
                            school!!,
                            year!!,
                            month!!,
                            date!!,
                            phone!!,
                            email!!,
                            "yyyyyyyyyy",
                            address!!, uid!!.toLong()
                        ) as Object
                    )
                )
                bfw.flush()
                var message: String = ObjectInputStream(socket.getInputStream()).readUTF()
                var msg = Message()
                msg.what = HANDLER_USERINFOUPDATE
                msg.obj = HandlerEntity(result, message as Object)
                handler.sendMessage(msg)
                bfw.close()
            }
        }
        return thread
    }


    fun registThread(
        result: MethodChannel.Result,
        headImg: String,
        password: String,
        nickname: String,
        school: String,
        address: String,
        email: String,
        phone: String,
        sex: String,
        year: Int,
        month: Int,
        date: Int
    ): Thread {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                var socket = CltToSvrConnection.Connection()
                var bfw = ObjectOutputStream(socket.getOutputStream())
                bfw.writeObject(
                    MsgEntity(
                        Constant.MSG_FLAG_REGISTER,
                        UserEntity(
                            password,
                            nickname,
                            if (sex == "m") 0 else 1,
                            headImg,
                            school,
                            year,
                            month,
                            date,
                            phone,
                            email,
                            "",
                            address
                        ) as Object
                    )
                )
                bfw.flush()

                //TODO 超时设定
                var message = ObjectInputStream(socket.getInputStream()).readObject() as RegisterEntity
                println(message.flag)
                println(
                    message.uid
                )

                var msg = Message()
                msg.what = HANDLER_REGIST
                msg.obj = HandlerEntity(result, message as Object)
                handler.sendMessage(msg)
                bfw.close()
            }
        }
        return thread
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(lifeServiceConnection)
        unbindService(notificationConnection)
    }

}


class HandlerEntity(result: MethodChannel.Result, message: Object) {
    var result: MethodChannel.Result? = null
    var message: Object? = null
    var socket:Socket? =null
    init {
        this.result = result
        this.message = message
    }

    constructor(result:MethodChannel.Result,message: Object,socket:Socket):this(result,message){
        this.socket = socket
    }



}