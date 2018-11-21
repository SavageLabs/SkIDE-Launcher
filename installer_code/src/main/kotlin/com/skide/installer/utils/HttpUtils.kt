package com.skide.installer.utils

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

fun downloadFile(target: String, path: String) {
    val url = URL(target)
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

class HttpResponse(val code: Int, val responseMessage: String, val headers: Map<String, String>, val body: ByteArray) {

    fun getBodyStr(): String {
        return String(body)
    }
}

fun httpRequest(target: String, method: String = "GET", headers: Map<String, String> = HashMap(), bdy: ByteArray? = null): HttpResponse {
    val connection = if (target.startsWith("https"))
        URL(target).openConnection() as HttpsURLConnection
    else
        URL(target).openConnection() as HttpURLConnection

    connection.requestMethod = method
    connection.instanceFollowRedirects = true
    connection.doInput = true
    connection.doOutput = true
    for ((k, v) in headers) connection.setRequestProperty(k, v)
    if (method != "GET" && bdy != null) {
        connection.outputStream.write(bdy)
        connection.outputStream.flush()
    }
    val code = connection.responseCode
    val msg = connection.responseMessage
    val headers = connection.headerFields
    val inStream = if (code == 200)
        connection.inputStream
    else
        connection.errorStream
    val headersMap = HashMap<String, String>()
    val byteStream = ByteArrayOutputStream()
    inStream.copyTo(byteStream)

    headers.forEach { key ->
        key.value.forEach {

            headersMap[if (key.key == null) "Baseline_req" else key.key] = it
        }
    }
    return HttpResponse(code, msg, headersMap, byteStream.toByteArray())
}