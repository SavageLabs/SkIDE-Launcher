package com.skide.installer


import com.skide.installer.core.Processor

object State {

    lateinit var args:Array<String>
    lateinit var prc: Processor

}

fun main(args:Array<String>) {
    State.args = args
    Processor(args).setup()

}
