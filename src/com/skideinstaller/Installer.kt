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
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardOpenOption

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

    lateinit var stage:Stage
    override fun start(primaryStage: Stage) {

        stage = primaryStage
        primaryStage.title = "Sk-IDE Launcher"
        
        updating = "Updating Sk-IDE..."

        val loader = FXMLLoader()
        val parent = loader.load<Parent>(javaClass.getResourceAsStream("Gui.fxml"))
        val controller = loader.getController<Controller>()
        primaryStage.scene = Scene(parent)
        primaryStage.centerOnScreen()
        primaryStage.isResizable = false
        primaryStage.sizeToScene()
        primaryStage.show()


        val task = object : Task<Void>() {
            @Throws(Exception::class)
            override fun call(): Void? {
                updateProgress(0.0, 100.0)
                updateMessage("Checking files...")

                val version = checkVersion()
                println(version)
                val folder = File(System.getProperty("user.home"), ".skide")
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
                    binFolder.mkdir()
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    update()
                    if (!versionFile.exists()) versionFile.createNewFile()

                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    primaryStage.close()
                    return null
                }
                val ideFile = File(binFolder, "Sk-IDE.jar")
                if (!ideFile.exists()) {
                    update()
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    if (!versionFile.exists()) versionFile.createNewFile()

                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    primaryStage.close()

                    return null
                }
                if (!versionFile.exists()) {
                    update()
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    if (!versionFile.exists()) versionFile.createNewFile()
                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    primaryStage.close()

                    return null
                }
                val oldVersion = String(Files.readAllBytes(versionFile.toPath()))

                if (oldVersion != version) {
                    update()
                    updateProgress(25.0, 100.0)
                    updateMessage(updating)
                    Files.write(versionFile.toPath(), version.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
                    primaryStage.close()

                    return null
                } else {
                    updateProgress(100.0, 100.0)
                    updateMessage("Starting Sk-IDE")
                    Thread.sleep(2500)
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

       if(getOS() == OperatingSystemType.MAC_OS) {
           Platform.runLater {

               stage.close()
               val folder = File(System.getProperty("user.home"), ".skide")
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

               coreManager.getDeclaredMethod("bootstrap", Array < String >::class.java).invoke(instance, State.args)


           }
       } else {
           val folder = File(System.getProperty("user.home"), ".skide")
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
        val folder = File(System.getProperty("user.home"), ".skide")
        val binFolder = File(folder, "bin")
        val ideFile = File(binFolder, "Sk-IDE.jar")

        println("Updating")
        downloadFile("https://liz3.net/sk/depot/SkIde.jar", ideFile.absolutePath)
        println("Updated")
        start()
    }

    companion object {
        fun start() {
            launch(Installer::class.java)
        }
    }
}
