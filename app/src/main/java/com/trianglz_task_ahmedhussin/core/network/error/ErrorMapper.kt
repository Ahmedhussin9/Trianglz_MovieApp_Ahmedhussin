package com.trianglz_task_ahmedhussin.core.network.error

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.io.InterruptedIOException

/**
 * The single place where technical exceptions become [DomainError]s (R21).
 *
 * Order matters: [java.net.SocketTimeoutException] and OkHttp's call timeout are both
 * [InterruptedIOException]s, which are themselves [IOException]s, so timeouts are
 * matched before the general "no connection" case. Malformed JSON
 * (`kotlinx.serialization.SerializationException`) and anything unexpected become
 * [DomainError.Unknown].
 */
fun Throwable.toDomainError(): DomainError = when (this) {
    is DomainException -> error
    is HttpException -> code().toDomainError()
    is InterruptedIOException -> DomainError.Timeout
    is IOException -> DomainError.NoInternet
    else -> DomainError.Unknown
}

/** Maps an HTTP status code to a [DomainError]. */
internal fun Int.toDomainError(): DomainError = when (this) {
    401, 403 -> DomainError.Unauthorized
    404 -> DomainError.NotFound
    429 -> DomainError.RateLimited
    in 500..599 -> DomainError.Server
    else -> DomainError.Unknown
}

/**
 * Wraps any throwable as a [DomainException] carrying its mapped error, for Paging load results.
 *
 * The original exception is logged once here (debug builds); an existing [DomainException]
 * was already logged when it was created, so it is returned unchanged without a second log.
 */
fun Throwable.toDomainException(): DomainException {
    if (this is DomainException) return this
    val error = toDomainError()
    Timber.w(this, "Load failed → %s", error)
    return DomainException(error, cause = this)
}
