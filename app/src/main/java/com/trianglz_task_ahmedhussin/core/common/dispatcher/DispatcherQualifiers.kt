package com.trianglz_task_ahmedhussin.core.common.dispatcher

import javax.inject.Qualifier

// Classes receive their CoroutineDispatcher through the constructor instead of referencing
// Dispatchers.IO directly, so tests can replace it with a TestDispatcher.

/** Dispatcher for blocking I/O: network calls, database access, file reads. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Dispatcher for CPU-bound work such as mapping or sorting large lists. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
