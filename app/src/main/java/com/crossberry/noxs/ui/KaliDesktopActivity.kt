package com.crossberry.noxs.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.crossberry.noxs.NoxsApplication
import com.crossberry.noxs.R
import com.crossberry.noxs.emulator.VncClient
import com.crossberry.noxs.runtime.KaliKexManager
import com.crossberry.noxs.runtime.NoxsResources
import com.crossberry.noxs.runtime.NoxsRuntimeFactory
import kotlin.math.min

/** Local-only viewport for a Kali NetHunter KeX desktop session. */
class KaliDesktopActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var password: EditText
    private lateinit var desktop: ImageView
    private lateinit var textInput: EditText
    private var bitmap: Bitmap? = null
    private var client: VncClient? = null
    private var manager: KaliKexManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kali_desktop)

        status = findViewById(R.id.kali_status)
        password = findViewById(R.id.kali_password)
        desktop = findViewById(R.id.kali_desktop_view)
        textInput = findViewById(R.id.kali_text_input)

        findViewById<Button>(R.id.kali_connect).setOnClickListener { connect() }
        findViewById<Button>(R.id.kali_stop).setOnClickListener { stopDesktop() }
        findViewById<Button>(R.id.kali_send).setOnClickListener { sendText() }
        desktop.setOnTouchListener(::onDesktopTouch)

        status.text = getString(R.string.kali_desktop_hint)
    }

    private fun createManager(): KaliKexManager? {
        val app = application as NoxsApplication
        val paths = app.environments.activePaths()
        if (!paths.isInstalled()) return null
        val launcher = NoxsRuntimeFactory.launcher(paths, NoxsResources(paths))
        return KaliKexManager(launcher)
    }

    private fun connect() {
        val entered = password.text
        if (entered.isNullOrBlank()) {
            status.text = getString(R.string.kali_desktop_password_hint)
            return
        }
        val secret = CharArray(entered.length) { index -> entered[index] }
        entered.clear()
        status.text = getString(R.string.kali_desktop_starting)
        Thread {
            val kex = createManager()
            if (kex == null) {
                secret.fill('\u0000')
                runOnUiThread { status.text = getString(R.string.kali_desktop_not_installed) }
                return@Thread
            }
            manager = kex
            val result = kex.start()
            if (!result.success) {
                secret.fill('\u0000')
                runOnUiThread {
                    status.text = result.output.ifBlank { getString(R.string.kali_desktop_start_failed) }
                }
                return@Thread
            }
            if (!kex.waitForReady()) {
                secret.fill('\u0000')
                runOnUiThread { status.text = getString(R.string.kali_desktop_port_timeout) }
                return@Thread
            }
            val next = VncClient(
                KaliKexManager.VNC_HOST,
                KaliKexManager.VNC_PORT,
                vncListener,
                secret
            )
            secret.fill('\u0000')
            runOnUiThread {
                client?.close()
                client = next
                password.text?.clear()
                status.text = getString(R.string.kali_desktop_connecting)
                next.start()
            }
        }.start()
    }

    private val vncListener = object : VncClient.Listener {
        override fun onConnecting() {
            runOnUiThread { status.text = getString(R.string.kali_desktop_connecting) }
        }

        override fun onConnected(width: Int, height: Int, name: String) {
            runOnUiThread {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                desktop.setImageBitmap(bitmap)
                status.text = getString(R.string.kali_desktop_connected, width, height)
            }
        }

        override fun onResized(width: Int, height: Int) {
            runOnUiThread {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                desktop.setImageBitmap(bitmap)
            }
        }

        override fun onRect(x: Int, y: Int, w: Int, h: Int, pixels: IntArray) {
            runOnUiThread {
                val frame = bitmap ?: return@runOnUiThread
                if (x >= 0 && y >= 0 && w > 0 && h > 0 &&
                    x + w <= frame.width && y + h <= frame.height &&
                    pixels.size >= w * h
                ) {
                    frame.setPixels(pixels, 0, w, x, y, w, h)
                    desktop.invalidate()
                }
            }
        }

        override fun onClosed(error: String?) {
            runOnUiThread {
                if (!isFinishing) {
                    status.text = error ?: getString(R.string.kali_desktop_disconnected)
                }
            }
        }
    }

    private fun onDesktopTouch(view: View, event: MotionEvent): Boolean {
        val frame = bitmap ?: return false
        val scale = min(
            view.width.toFloat() / frame.width.toFloat(),
            view.height.toFloat() / frame.height.toFloat()
        )
        if (scale <= 0f) return false
        val left = (view.width - frame.width * scale) / 2f
        val top = (view.height - frame.height * scale) / 2f
        val x = ((event.x - left) / scale).toInt().coerceIn(0, frame.width - 1)
        val y = ((event.y - top) / scale).toInt().coerceIn(0, frame.height - 1)
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                client?.sendPointer(x, y, 1)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                client?.sendPointer(x, y, 0)
            else -> return false
        }
        return true
    }

    private fun sendText() {
        val value = textInput.text?.toString().orEmpty()
        value.forEach { char ->
            client?.sendChar(char, true)
            client?.sendChar(char, false)
        }
        textInput.text?.clear()
    }

    private fun stopDesktop() {
        client?.close()
        client = null
        val kex = manager ?: createManager()
        status.text = getString(R.string.kali_desktop_stopping)
        Thread {
            val result = kex?.stop()
            runOnUiThread {
                status.text = if (result?.success == true) {
                    getString(R.string.kali_desktop_stopped)
                } else {
                    result?.output?.ifBlank { getString(R.string.kali_desktop_stop_failed) }
                        ?: getString(R.string.kali_desktop_not_installed)
                }
            }
        }.start()
    }

    override fun onDestroy() {
        client?.close()
        client = null
        super.onDestroy()
    }
}
