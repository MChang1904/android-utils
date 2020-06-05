package com.labs1904.network.cookies

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

interface MutableCookieJar : CookieJar {
	fun clear()
	fun clearSession()
}

class PersistentCookieJar(
	private val sessionCookieCache: CookieDataSource,
	private val persistentCookieCache: CookieDataSource
) : MutableCookieJar {

	private var loaded = false

	@Synchronized
	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		if (!loaded) fillCacheWithPersistedCookies()

		cookies.map { PersistableCookie(it) }.forEach {
			sessionCookieCache.insert(it)
			if (it.isPersistent()) persistentCookieCache.insert(it)
		}
	}

	@Synchronized
	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		if (!loaded) fillCacheWithPersistedCookies()

		val expired = mutableListOf<PersistableCookie>()
		val valid = mutableListOf<PersistableCookie>()

		sessionCookieCache.load().forEach {
			if (it.isExpired()) expired.add(it)
			else if (it.toCookie().matches(url)) valid.add(it)
		}

		sessionCookieCache.removeAll(expired)
		persistentCookieCache.removeAll(expired)

		return valid.map { it.toCookie() }
	}

	@Synchronized
	override fun clear() {
		sessionCookieCache.clear()
		persistentCookieCache.clear()
	}

	@Synchronized
	override fun clearSession() {
		sessionCookieCache.clear()
		fillCacheWithPersistedCookies()
	}

	private fun fillCacheWithPersistedCookies() {
		sessionCookieCache.insertAll(persistentCookieCache.load())
		loaded = true
	}
}
