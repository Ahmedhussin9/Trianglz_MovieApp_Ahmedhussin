package com.trianglz_task_ahmedhussin.core.common.locale

import java.util.Locale

/**
 * Supplies the locale the app is currently displayed in.
 *
 * It is read on every call rather than cached, so a language change takes effect
 * on the next request without restarting anything.
 */
fun interface LocaleProvider {
    fun current(): Locale
}
