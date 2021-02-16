package ru.nsu.bobrofon.easysshfs

import android.util.Log
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

private const val TAG = "IpResolver"
private const val RETRIES = 5
private const val HOT_CACHE_MILLISECONDS = 10000

object IpResolver {
    private val cache = HashMap<String, String>()
    private var lastCheck = System.currentTimeMillis()

    fun resolve(host: String): String {
        val now = System.currentTimeMillis()
        val elapsed = now - lastCheck

        val cachedIp = cache.getOrElse(host, {null})

        if (cachedIp != null && elapsed < HOT_CACHE_MILLISECONDS) {
            return cachedIp
        }

        val resolvedIp = tryResolve(host)
        if (resolvedIp != null) {
            cache[host] = resolvedIp
            lastCheck = now
            return resolvedIp
        }

        if (cachedIp != null) {
            return cachedIp
        }

        return host
    }

    private fun tryResolve(host: String): String? {
        for (i in 0 until RETRIES) {
            val ip = tryResolveOnce(host)
            if (ip != null) {
                return ip
            }
        }
        return null
    }

    private fun tryResolveOnce(host: String): String? {
        try {
            val address = InetAddress.getByName(host)
            if (address is Inet6Address) {
                if (!address.getHostAddress().startsWith("[")) {
                    return "[" + address.getHostAddress() + "]"
                }
            }
            return address.hostAddress

        } catch (e: UnknownHostException) {
            Log.w(TAG, e.message.orEmpty())
            return null
        }
    }
}