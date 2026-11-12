package com.trianglz_task_ahmedhussin.core.domain.error

/**
 * Every failure the app can show to the user, independent of how it happened.
 *
 * The data layer converts exceptions (I/O, HTTP, parsing) into one of these, so
 * ViewModels and UI never deal with Retrofit, OkHttp or Room types (R21).
 */
sealed interface DomainError {

    /** The device could not reach the server: offline, DNS failure, connection refused. */
    data object NoInternet : DomainError

    /** The server was reached but did not answer in time. */
    data object Timeout : DomainError

    /** The API rejected our credentials (HTTP 401/403), typically a missing or invalid token. */
    data object Unauthorized : DomainError

    /** The requested resource does not exist (HTTP 404), for example a removed movie. */
    data object NotFound : DomainError

    /** Too many requests in a short time (HTTP 429). */
    data object RateLimited : DomainError

    /** The server failed (HTTP 5xx). */
    data object Server : DomainError

    /** Anything else, including malformed responses. */
    data object Unknown : DomainError
}
