package com.skide.installer.core

import com.skide.installer.State
import com.skide.installer.utils.downloadFile
import com.skide.installer.utils.getOS
import com.skide.installer.utils.httpRequest
import com.skide.installer.utils.osToNumber
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.swing.JOptionPane


class Processor(args: Array<String>) {

    var configFolder: File
    var binFolder: File
    val os = getOS()
    val osNum = osToNumber(os)
    var folder = File(File(".").canonicalPath)

    init {
        State.prc = this
        for ((index, arg) in args.withIndex()) {
            if (arg == "-folder") {
                val target = File(args[index + 1])
                if (target.exists() && target.isDirectory)
                    folder = target
                else
                    error("${target.absolutePath} is not a valid directory")
            }
        }
        configFolder = File(folder, "conf")
        binFolder = File(folder, "bin")
        if (!configFolder.exists()) configFolder.mkdir()
        if (!binFolder.exists()) binFolder.mkdir()
    }

    private fun getLocalVersions(): String? {
        val file = File(configFolder, "versions")
        if (!file.exists())
            return "undefined"
        val obj = JSONObject(String(Files.readAllBytes(file.toPath())))
        return obj.getString("binary")
    }

    private fun updateBinary(newVersion: String, cb: () -> Unit) {
        Thread {
            val t = "https://skide.21xayah.com/?_q=get&component=binary&os=$osNum&ver=$newVersion"
            downloadFile(t, File(binFolder, "ide.jar").absolutePath)
            writeVersionFile(newVersion)
            cb()
        }.start()
        JOptionPane.showMessageDialog(null, "SK-IDE is updating...")
    }

    private fun writeVersionFile(bver: String) {
        val obj = JSONObject()
        obj.put("binary", bver)

        Files.write(
            File(configFolder, "versions").toPath(),
            obj.toString().toByteArray(),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    private fun getRemoteVersions(): String {
        val versionResult = JSONObject(httpRequest("https://skide.21xayah.com/?_q=version").getBodyStr())

        return versionResult.getString("binary")
    }

    fun start() {
        Thread {
            val ideFile = File(binFolder, "ide.jar")
            val builder = ProcessBuilder()
            val list = ArrayList<String>()
            list.add(File("jre11/bin/javaw.exe").absolutePath)
            list.add("-jar")
            list.add(ideFile.absolutePath)
            State.args.forEach {
                list.add(it)
            }
            builder.command(list)
            val proc = builder.start()
            Runtime.getRuntime().addShutdownHook(Thread {
                proc.destroy()
            })
            while (proc.isAlive) {
                Thread.sleep(10)
            }
        }.start()
    }
    fun setup() {
        if(State.args.isNotEmpty()) {
            start()
            return
        }
        val remoteVersions = getRemoteVersions()
        val localVersions = getLocalVersions()
        val ideFile = File(binFolder, "ide.jar")
        if (remoteVersions != localVersions || !ideFile.exists())
            updateBinary(remoteVersions) {
                start()
            }
        else
            start()
    }
}