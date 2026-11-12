package com.trianglz_task_ahmedhussin.core.domain.error

/**
 * Carries a [DomainError] where an API only accepts a [Throwable].
 *
 * Paging reports failures as `LoadState.Error(throwable)`; wrapping the mapped error
 * lets the UI read `(throwable as DomainException).error` instead of inspecting
 * network exceptions itself.
 */
class DomainException(val error: DomainError, cause: Throwable? = null) : Exception(error.toString(), cause)
