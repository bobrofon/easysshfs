// SPDX-License-Identifier: MIT
package ru.nsu.bobrofon.easysshfs

import android.util.Log
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

private const val TAG = "IpResolver"

object IpResolver {
    private val cache = HashMap<String, String>()

    fun resolve(host: String, refreshCache: Boolean = true): String {
        val cachedIp = cache[host]

        if (cachedIp != null && !refreshCache) {
            return cachedIp
        }

        val resolvedIp = tryResolveOnce(host)
        if (resolvedIp != null) {
            cache[host] = resolvedIp
            return resolvedIp
        }

        if (cachedIp != null) {
            return cachedIp
        }

        return host
    }

    private fun tryResolveOnce(host: String): String? {
        try {
            val address = InetAddress.getByName(host)
            if (address is Inet6Address) {
                address.hostAddress?.let { hostAddress ->
                    if (!hostAddress.startsWith("[")) {
                        return "[$hostAddress]"
                    }
                }
            }
            return address.hostAddress

        } catch (e: UnknownHostException) {
            Log.w(TAG, e.message.orEmpty())
            return null
        }
    }
}