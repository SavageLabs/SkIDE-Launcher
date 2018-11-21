package com.skide.installer.utils

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
fun osToNumber(os:OperatingSystemType): Int {

    if(os == OperatingSystemType.WINDOWS) return 0
    if(os == OperatingSystemType.MAC_OS) return 1
    if(os == OperatingSystemType.LINUX) return 2
    if(os == OperatingSystemType.OTHER) return 3

    return -1
}