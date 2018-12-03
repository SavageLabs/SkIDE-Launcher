package com.skide.installer.core

import com.skide.installer.State
import com.skide.installer.utils.*
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.swing.JOptionPane


class Processor(args: Array<String>) {

    var configFolder: File
    var binFolder: File
    private val os = getOS()
    private val osNum = osToNumber(os)
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
            if (os == OperatingSystemType.WINDOWS) {
                downloadFile(t, File(binFolder, "ide.exe").absolutePath)
            } else if (os == OperatingSystemType.MAC_OS) {
                downloadFile(t, File("Sk-IDE.app/Contents/MacOS/ide.jar").absolutePath)

            } else {
                downloadFile(t, File(binFolder, "ide.jar").absolutePath)
            }
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
            val os = getOS()
            val builder = ProcessBuilder()
            val list = ArrayList<String>()
            when (os) {
                OperatingSystemType.WINDOWS -> {
                    val ideFile = File(binFolder, "ide.exe")
                    list.add(ideFile.absolutePath)
                }
                OperatingSystemType.MAC_OS -> {
                    list.add("open")
                    list.add("-a")
                    list.add("Sk-IDE.app")
                }
                else -> {
                    val ideFile = File(binFolder, "ide.jar")
                    list.add(ideFile.absolutePath)
                }
            }
            State.args.forEach {
                list.add(it)
            }
            builder.command(list)
            builder.start()
        }.start()
    }

    fun setup() {
        if (State.args.isNotEmpty()) {
            start()
            return
        }
        val os = getOS()
        val remoteVersions = getRemoteVersions()
        val localVersions = getLocalVersions()
        val ideFile = when (os) {
            OperatingSystemType.WINDOWS -> File(binFolder, "ide.exe")
            OperatingSystemType.MAC_OS -> File("Sk-IDE.app/Contents/MacOS/ide.jar")
            else -> File(binFolder, "ide.jar")
        }
        if (remoteVersions != localVersions || !ideFile.exists())
            updateBinary(remoteVersions) {
                start()
            }
        else
            start()
    }
}