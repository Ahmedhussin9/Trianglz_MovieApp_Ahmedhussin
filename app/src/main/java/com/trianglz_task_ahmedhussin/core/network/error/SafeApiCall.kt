package com.trianglz_task_ahmedhussin.core.network.error

import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and returns its value as [AppResult.Success], or the mapped
 * [com.trianglz_task_ahmedhussin.core.domain.error.DomainError] as [AppResult.Failure].
 *
 * The original exception is logged (debug builds) before it is reduced to a domain error,
 * so the root cause stays visible in Logcat even though the UI only sees the domain error.
 *
 * [CancellationException] is always rethrown, unlogged: swallowing it would break structured
 * concurrency (a cancelled screen would keep working and report a fake error).
 */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    val error = e.toDomainError()
    Timber.w(e, "API call failed → %s", error)
    AppResult.Failure(error)
}
