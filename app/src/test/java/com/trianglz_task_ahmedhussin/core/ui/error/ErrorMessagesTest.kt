package com.trianglz_task_ahmedhussin.core.ui.error

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMessagesTest {

    /** `messageRes()` is an exhaustive `when`, so a new error without a message fails to compile. */
    @Test
    fun `every domain error has its own message`() {
        val errors = listOf(
            DomainError.NoInternet,
            DomainError.Timeout,
            DomainError.Unauthorized,
            DomainError.NotFound,
            DomainError.RateLimited,
            DomainError.Server,
            DomainError.Unknown,
        )

        val messages = errors.map { it.messageRes() }

        assertEquals("each error needs a distinct message", errors.size, messages.toSet().size)
    }
}
