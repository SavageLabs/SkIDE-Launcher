package com.skide.installer.core

import com.skide.installer.State
import com.skide.installer.utils.downloadFile
import com.skide.installer.utils.getOS
import com.skide.installer.utils.httpRequest
import com.skide.installer.utils.osToNumber
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.URLClassLoader
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

    private fun getLocalVersions(): Pair<String, String> {
        val file = File(configFolder, "versions")
        if (!file.exists()) {
            return Pair("undefined", "undefined")
        }
        val obj = JSONObject(String(Files.readAllBytes(file.toPath())))
        val binaryVersion = obj.getString("binary")
        val libraryVersion = obj.getString("library")

        return Pair(binaryVersion, libraryVersion)
    }

    private fun updateLibrary(newVersion: String, currentBinary: String) {
        JOptionPane.showMessageDialog(null, "SK-IDE is updating its core libraries..")
        val t = "https://skide.21xayah.com/?_q=get&component=library&os=$osNum&ver=$newVersion"
        downloadFile(t, File(binFolder, "libs.jar").absolutePath)
        writeVersionFile(currentBinary, newVersion)

        Runtime.getRuntime().exec(File(folder, "Sk-IDE.exe").absolutePath)
        System.exit(0)
    }

    private fun updateBinary(newVersion: String, currentLibrary: String) {
        JOptionPane.showMessageDialog(null, "SK-IDE is updating...")
        val t = "https://skide.21xayah.com/?_q=get&component=binary&os=$osNum&ver=$newVersion"
        downloadFile(t, File(binFolder, "ide.jar").absolutePath)
        writeVersionFile(newVersion, currentLibrary)
    }

    private fun writeVersionFile(bver: String, lVer: String) {
        val obj = JSONObject()
        obj.put("binary", bver)
        obj.put("library", lVer)

        Files.write(
            File(configFolder, "versions").toPath(),
            obj.toString().toByteArray(),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    private fun getRemoteVersions(): Pair<String, String> {
        val versionResult = JSONObject(httpRequest("https://skide.21xayah.com/?_q=version").getBodyStr())
        val binaryVersion = versionResult.getString("binary")
        val libraryVersion = versionResult.getString("library")

        return Pair(binaryVersion, libraryVersion)
    }

    fun start() {
        Thread {
            val ideFile = File(binFolder, "ide.jar")
            val child = URLClassLoader(
                arrayOf(URL(ideFile.toURI().toURL().toString())), Processor::class.java.classLoader
            )
            val coreManager = Class.forName("com.skide.CoreManager", true, child)
            val instance = coreManager.newInstance()
            coreManager.getDeclaredMethod("bootstrap", Array<String>::class.java, ClassLoader::class.java)
                .invoke(instance, arrayOf(""), child)

        }.start()
    }

    fun setup(): Processor {
        val remoteVersions = getRemoteVersions()
        val localVersions = getLocalVersions()

        val ideFile = File(binFolder, "ide.jar")
        val libFile = File(binFolder, "libs.jar")
        if (remoteVersions.second != localVersions.second || !libFile.exists()) updateLibrary(
            remoteVersions.second,
            localVersions.first
        )
        if (remoteVersions.first != localVersions.first || !ideFile.exists()) updateBinary(
            remoteVersions.first,
            localVersions.second
        )

        return this
    }

}