package com.crossberry.noxs.runtime

import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/** Starts and stops the Kali KeX VNC server inside the app-private proot rootfs. */
class KaliKexManager(private val launcher: ProotLauncher) {

    data class Result(val success: Boolean, val output: String)

    fun start(): Result = runGuestCommand(
        listOf(
            "/bin/bash", "-lc",
            "command -v kex >/dev/null 2>&1 || { echo 'KeX command is not installed'; exit 127; }; " +
                "nohup kex >/tmp/noxs-kex.log 2>&1 </dev/null &"
        )
    )

    fun stop(): Result = runGuestCommand(listOf("/bin/bash", "-lc", "kex stop"))

    fun isReady(): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", VNC_PORT), 350) }
        true
    }.getOrDefault(false)

    fun waitForReady(timeoutMs: Long = 15_000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isReady()) return true
            try { Thread.sleep(250) } catch (_: InterruptedException) { return false }
        }
        return isReady()
    }

    private fun runGuestCommand(command: List<String>): Result {
        return try {
            val process = ProcessBuilder(launcher.oneShotArgv(command, asRoot = false))
                .redirectErrorStream(true)
                .also { launcher.applyEnvTo(it) }
                .start()
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return Result(false, "Kali KeX command timed out")
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }.take(8_192)
            Result(process.exitValue() == 0, output)
        } catch (e: Exception) {
            Result(false, e.message ?: e.javaClass.simpleName)
        }
    }

    companion object {
        const val VNC_HOST = "127.0.0.1"
        const val VNC_PORT = 5901
    }
}
