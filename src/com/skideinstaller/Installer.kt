package com.skideinstaller

import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.stage.Stage
import java.io.*
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection


fun downloadFile(target: String, path: String) {
    val url = URL(target);
    val bis = BufferedInputStream(url.openStream())
    val fis = FileOutputStream(File(path))
    val buffer = ByteArray(1024)
    var count: Int
    while (true) {
        count = bis.read(buffer, 0, 1024)
        if (count == -1) break
        fis.write(buffer, 0, count)
    }
    fis.close()
    bis.close()
}

fun request(path: String, method: String = "GET", headers: Map<String, String> = HashMap(), body: String = ""): Triple<Int, MutableMap<String, MutableList<String>>, InputStream> {

    val connection = {
        val url = URL(path)

        if (path.startsWith("https://")) {
            url.openConnection() as HttpsURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }

    }.invoke()
    connection.requestMethod = method
    connection.instanceFollowRedirects = true
    headers.forEach {
        connection.addRequestProperty(it.key, it.value)
    }
    connection.doInput = true
    connection.doOutput = true
    if (method == "POST") {
        connection.outputStream.write(body.toByteArray())
        connection.outputStream.flush()
    }
    connection.connect()
    return Triple(connection.responseCode, connection.headerFields, connection.inputStream)
}

object State {
    var args = arrayOf("")
}

fun main(args: Array<String>) {
    State.args = args
    Installer.start()

}

enum class OperatingSystemType {
    MAC_OS,
    WINDOWS,
    LINUX,
    OTHER
}

fun getOS(): OperatingSystemType {

    val sys = System.getProperty("os.name")

    if (sys.contains("Windows", true)) return OperatingSystemType.WINDOWS
    if (sys.contains("Linux", true) || sys.contains("Unix", true)) return OperatingSystemType.LINUX
    if (sys.contains("Darwin", true) || sys.contains("OSX", true) || sys.contains("macos", true)) return OperatingSystemType.MAC_OS

    return OperatingSystemType.OTHER
}

class Controller {

    @FXML
    lateinit var label: Label
    @FXML
    lateinit var progessBar: ProgressBar
    @FXML
    lateinit var cancelBtn: Button
}

class Installer : Application() {

    lateinit var stage: Stage
    override fun start(primaryStage: Stage) {

        stage = primaryStage
        primaryStage.title = "Sk-IDE Launcher"


        val loader = FXMLLoader()
        val parent = loader.load<Parent>(ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<?import javafx.scene.control.Button?>\n" +
                "<?import javafx.scene.control.Label?>\n" +
                "<?import javafx.scene.control.ProgressBar?>\n" +
                "<?import javafx.scene.layout.Pane?>\n" +
                "\n" +
                "<Pane maxHeight=\"-Infinity\" maxWidth=\"-Infinity\" minHeight=\"-Infinity\" minWidth=\"-Infinity\" prefHeight=\"85.0\"\n" +
                "      prefWidth=\"595.0\" xmlns=\"http://javafx.com/javafx/8.0.141\" xmlns:fx=\"http://javafx.com/fxml/1\"\n" +
                "      fx:controller=\"com.skideinstaller.Controller\">\n" +
                "    <Label fx:id=\"label\" layoutX=\"15.0\" layoutY=\"14.0\" prefHeight=\"17.0\" prefWidth=\"207.0\" text=\"Label\"/>\n" +
                "    <ProgressBar fx:id=\"progessBar\" layoutX=\"15.0\" layoutY=\"31.0\" prefHeight=\"18.0\" prefWidth=\"571.0\" progress=\"0.0\"/>\n" +
                "    <Button fx:id=\"cancelBtn\" layoutX=\"534.0\" layoutY=\"53.0\" mnemonicParsing=\"false\" text=\"Cancel\"/>\n" +
                "</Pane>\n").toByteArray()))
        val controller = loader.getController<Controller>()
        primaryStage.scene = Scene(parent)
        primaryStage.centerOnScreen()
        primaryStage.isResizable = false
        primaryStage.sizeToScene()


        val task = object : Task<Void>() {
            @Throws(Exception::class)
            override fun call(): Void? {
                updateProgress(0.0, 100.0)
                updateMessage("Checking files...")

                val version = checkVersion()
                println(version)
                val updating = "Updating Sk-IDE to $version..."
                val folder = File(System.getProperty("user.home"), ".SK-IDE")
                val binFolder = File(folder, "bin")
                val versionFile = File(binFolder, "version.txt")
                if (!folder.exists()) {
                    folder.mkdir()
                    if (!binFolder.exists()) binFolder.mkdir()
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    update()
                    if (!versionFile.exists()) versionFile.createNewFile()
                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    return null
                }
                if (!binFolder.exists()) {
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    binFolder.mkdir()
                    update()
                    if (!versionFile.exists()) versionFile.createNewFile()
                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    primaryStage.close()
                    return null
                }
                val ideFile = File(binFolder, "Sk-IDE.jar")
                if (!ideFile.exists()) {
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    update()
                    if (!versionFile.exists()) versionFile.createNewFile()
                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    primaryStage.close()
                    return null
                }
                if (!versionFile.exists()) {
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    update()
                    if (!versionFile.exists()) versionFile.createNewFile()
                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    primaryStage.close()
                    return null
                }
                val oldVersion = String(Files.readAllBytes(versionFile.toPath()))

                if (oldVersion != version) {
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    update()

                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    primaryStage.close()

                    return null
                } else {
                    start()
                }


                return null
            }
        }
        controller.label.textProperty().bind(task.messageProperty())
        controller.progessBar.progressProperty().bind(task.progressProperty())
        val thread = Thread(task)
        thread.isDaemon = true

        controller.cancelBtn.setOnAction {
            task.cancel()
            System.exit(0)
        }
        thread.start()
    }

    fun checkVersion(): String {
        var str = ""
        val response = request("https://liz3.net/sk/depot/").third

        while (true) {
            val r = response.read()
            if (r == -1) break
            str += r.toChar()
        }
        return str
    }

    fun start() {
        if (getOS() == OperatingSystemType.MAC_OS) {
            Platform.runLater {
                stage.close()
                val folder = File(System.getProperty("user.home"), ".SK-IDE")
                val binFolder = File(folder, "bin")
                val ideFile = File(binFolder, "Sk-IDE.jar")
                val classLoader = ClassLoader.getSystemClassLoader() as URLClassLoader
                val method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
                method.isAccessible = true
                method.invoke(classLoader, ideFile.toURI().toURL())
                val debugger = Class.forName("com.skide.core.debugger.Debugger")
                debugger.newInstance()
                val coreManager = Class.forName("com.skide.CoreManager")
                val instance = coreManager.newInstance()
                coreManager.getDeclaredMethod("bootstrap", Array<String>::class.java).invoke(instance, State.args)
            }
        } else {
            val folder = File(System.getProperty("user.home"), ".SK-IDE")
            val binFolder = File(folder, "bin")
            val ideFile = File(binFolder, "Sk-IDE.jar")
            Thread {
                val java = File(File(System.getProperty("java.home"), "bin"), "java").absolutePath
                println(java)
                val args = arrayListOf<String>(java, "-jar", ideFile.absolutePath)
                val pb = ProcessBuilder()
                args += State.args
                pb.command(args)
                pb.start()
                System.exit(0)
            }.start()
        }
    }

    fun update() {
        Platform.runLater {
            stage.show()
        }
        val folder = File(System.getProperty("user.home"), ".SK-IDE")
        val binFolder = File(folder, "bin")
        val ideFile = File(binFolder, "Sk-IDE.jar")
        downloadFile("https://liz3.net/sk/depot/SkIde.jar", ideFile.absolutePath)
        start()
    }

    companion object {
        fun start() {
            launch(Installer::class.java)
        }
    }
}
