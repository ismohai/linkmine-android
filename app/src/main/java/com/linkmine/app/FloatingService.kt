package com.linkmine.app

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FloatingService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isExpanded = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        const val CHANNEL_ID = "linkmine_floating"
        const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        createFloatingView()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持悬浮窗运行"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LinkMine")
            .setContentText("悬浮窗运行中")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_layout, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        windowManager?.addView(floatingView, params)

        setupFloatingViewListeners(params)
    }

    private fun setupFloatingViewListeners(params: WindowManager.LayoutParams) {
        val btnToggle = floatingView?.findViewById<ImageButton>(R.id.btn_toggle)
        val expandedLayout = floatingView?.findViewById<LinearLayout>(R.id.expanded_layout)
        val etFloatingMessage = floatingView?.findViewById<EditText>(R.id.et_floating_message)
        val btnFloatingSend = floatingView?.findViewById<Button>(R.id.btn_floating_send)
        val btnClose = floatingView?.findViewById<ImageButton>(R.id.btn_close)

        // 切换展开/收起
        btnToggle?.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                expandedLayout?.visibility = View.VISIBLE
                // 允许输入
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                windowManager?.updateViewLayout(floatingView, params)
            } else {
                expandedLayout?.visibility = View.GONE
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager?.updateViewLayout(floatingView, params)
            }
        }

        // 发送消息
        btnFloatingSend?.setOnClickListener {
            val message = etFloatingMessage?.text?.toString()?.trim() ?: ""
            if (message.isNotEmpty()) {
                btnFloatingSend.isEnabled = false
                btnFloatingSend.text = "..."
                
                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        NtfyClient.send(message)
                    }
                    
                    btnFloatingSend.isEnabled = true
                    btnFloatingSend.text = "发送"
                    
                    if (success) {
                        etFloatingMessage?.text?.clear()
                        Toast.makeText(this@FloatingService, "已发送", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@FloatingService, "发送失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 关闭悬浮窗
        btnClose?.setOnClickListener {
            stopSelf()
        }

        // 拖动功能
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        btnToggle?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        floatingView?.let {
            windowManager?.removeView(it)
        }
    }
}
