package com.skideinstaller

object State {

    var args = arrayOf("")
}

fun main(args:Array<String>) {
    State.args = args
    Installer.start()

}