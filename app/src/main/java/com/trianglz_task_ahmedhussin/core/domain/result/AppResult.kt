package com.trianglz_task_ahmedhussin.core.domain.result

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError

/**
 * Outcome of an operation that can fail with a [DomainError].
 *
 * Used instead of [kotlin.Result] because its failure type is a closed set of domain
 * errors rather than any [Throwable], so `when` over it is exhaustive in the UI layer.
 */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    data class Failure(val error: DomainError) : AppResult<Nothing>
}

/** Transforms the success value, passing failures through unchanged. */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}
