package com.skide.installer.core

import com.skide.installer.State
import com.skide.installer.utils.*
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.swing.JOptionPane
import javax.swing.JDialog
import javax.swing.SwingUtilities


class Processor(args: Array<String>) {

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
        binFolder = File(folder, "bin")
        if (!binFolder.exists()) binFolder.mkdir()
    }

    private fun getDialog(msg: String): JDialog {
        val optionPane = JOptionPane(
            msg,
            JOptionPane.INFORMATION_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null,
            arrayOf(),
            null
        )

        val dialog = JDialog()
        dialog.title = "SkIDE Updater..."
        dialog.isModal = true

        dialog.contentPane = optionPane

        dialog.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
        dialog.pack()

        return dialog
    }

    private fun getLocalVersions(): Triple<String, Boolean, Boolean> {
        val file = File(binFolder, "versions")
        if (!file.exists())
            return Triple("", false, true)
        val obj = JSONObject(String(Files.readAllBytes(file.toPath())))
        return Triple(obj.getString("binary"), obj.getBoolean("beta"), obj.getBoolean("update"))
    }

    private fun updateBinary(newVersion: String, beta: Boolean, update: Boolean, cb: () -> Unit) {
        val result =
            JOptionPane.showConfirmDialog(null, "New Sk-IDE Version available, do you want to update? ($newVersion)")
        if (result == 0) {
            val dialog = getDialog("Updating to $newVersion....")
            Thread {
                val t = "https://skide.21xayah.com/?_q=get&component=binary&os=$osNum&ver=$newVersion"
                if (os == OperatingSystemType.WINDOWS) {
                    downloadFile(t, File(binFolder, "ide.exe").absolutePath)
                } else {
                    downloadFile(t, File(binFolder, "ide.jar").absolutePath)
                }
                writeVersionFile(newVersion, beta, update)
                SwingUtilities.invokeLater { dialog.dispose() }
                cb()
            }.start()
            dialog.isVisible = true
        }
        if (result == 1) {
            cb()
        }
        if (result == 2) {
            System.exit(0)
        }
    }

    private fun writeVersionFile(bver: String, beta: Boolean, update: Boolean) {
        val obj = JSONObject()
        obj.put("binary", bver)
        obj.put("beta", beta)
        obj.put("update", update)

        Files.write(
            File(binFolder, "versions").toPath(),
            obj.toString().toByteArray(),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    private fun getRemoteVersions(): Pair<String, String> {
        val versionResult = JSONObject(httpRequest("https://skide.21xayah.com/?_q=version").getBodyStr())

        return Pair(versionResult.getString("latest"), versionResult.getString("beta"))
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
                else -> {
                    val ideFile = File(binFolder, "ide.jar")
                    list.add("jre11/bin/java")
                    list.add("-Dskide.mode=prod")
                    list.add("-jar")
                    list.add(ideFile.absolutePath)
                }
            }
            State.args.forEach {
                list.add(it)
            }
            builder.directory( File("/usr/share/skide/"));
            builder.command(list)
            builder.start()
        }.start()
    }

    fun setup() {
        val os = getOS()
        val localVersions = getLocalVersions()
        val ideFile = when (os) {
            OperatingSystemType.WINDOWS -> File(binFolder, "ide.exe")
            else -> File(binFolder, "ide.jar")
        }
        if (!localVersions.third) {
            if (ideFile.exists()) start()
            return
        }
        try {
            val remoteVersions = getRemoteVersions()
            val installedVersion = localVersions.first
            if (localVersions.second) {
                if (remoteVersions.second == installedVersion) {
                    start()
                } else {
                    updateBinary(remoteVersions.second, localVersions.second, localVersions.third) {
                        if (ideFile.exists()) start()
                    }
                }
            } else {
                if (remoteVersions.first == installedVersion) {
                    start()
                } else {
                    updateBinary(remoteVersions.first, localVersions.second, localVersions.third) {
                        if (ideFile.exists()) start()
                    }
                }
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed to check for updates")
            if (ideFile.exists()) {
                start()
            }
        }
    }
}
