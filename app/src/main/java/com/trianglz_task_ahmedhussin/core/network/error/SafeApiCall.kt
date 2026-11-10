package com.trianglz_task_ahmedhussin.core.network.error

import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and returns its value as [AppResult.Success], or the mapped
 * [com.trianglz_task_ahmedhussin.core.domain.error.DomainError] as [AppResult.Failure].
 *
 * [CancellationException] is always rethrown: swallowing it would break structured
 * concurrency (a cancelled screen would keep working and report a fake error).
 */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    AppResult.Failure(e.toDomainError())
}
