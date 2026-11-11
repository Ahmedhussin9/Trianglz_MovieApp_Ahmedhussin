package com.trianglz_task_ahmedhussin.testing

import timber.log.Timber

/** A Timber tree that records every log call so tests can assert on what was logged. */
class RecordingTree : Timber.Tree() {

    data class Entry(val priority: Int, val message: String, val throwable: Throwable?)

    val entries = mutableListOf<Entry>()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        entries += Entry(priority, message, t)
    }
}
