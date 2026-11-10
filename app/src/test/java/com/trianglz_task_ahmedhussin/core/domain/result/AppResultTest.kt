package com.trianglz_task_ahmedhussin.core.domain.result

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import org.junit.Assert.assertEquals
import org.junit.Test

class AppResultTest {

    @Test
    fun `map transforms a success value`() {
        val result: AppResult<Int> = AppResult.Success(2)

        assertEquals(AppResult.Success("4"), result.map { (it * 2).toString() })
    }

    @Test
    fun `map passes a failure through without calling transform`() {
        val result: AppResult<Int> = AppResult.Failure(DomainError.Timeout)

        val mapped = result.map<Int, String> { error("must not be called") }

        assertEquals(AppResult.Failure(DomainError.Timeout), mapped)
    }
}
