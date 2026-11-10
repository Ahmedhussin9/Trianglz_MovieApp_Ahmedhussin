package com.trianglz_task_ahmedhussin.core.ui.error

import androidx.annotation.StringRes
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError

/** The localized message shown to the user for each [DomainError]. */
@StringRes
fun DomainError.messageRes(): Int = when (this) {
    DomainError.NoInternet -> R.string.error_no_internet
    DomainError.Timeout -> R.string.error_timeout
    DomainError.Unauthorized -> R.string.error_unauthorized
    DomainError.NotFound -> R.string.error_not_found
    DomainError.RateLimited -> R.string.error_rate_limited
    DomainError.Server -> R.string.error_server
    DomainError.Unknown -> R.string.error_unknown
}
