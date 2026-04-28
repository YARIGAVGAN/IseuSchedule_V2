package com.example.scheduleiseu.data.remote.cookie

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class MemoryCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        val current = store.getOrPut(key) { mutableListOf() }

        for (newCookie in cookies) {
            current.removeAll { old ->
                old.name == newCookie.name &&
                    old.domain == newCookie.domain &&
                    old.path == newCookie.path
            }
            current.add(newCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store[url.host]
            ?.filter { it.matches(url) }
            ?: emptyList()
    }

    fun clear() {
        store.clear()
    }
}
