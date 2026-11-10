package com.trianglz_task_ahmedhussin.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/*
 * Navigation destinations. Routes are @Serializable so `rememberNavBackStack` can save the
 * whole back stack across configuration changes and process death.
 */

/** Start destination: the movie list with its search bar (R1, R11). */
@Serializable
data object MoviesRoute : NavKey

/** Details of one movie (R5, R6). Only the id is passed; the screen loads everything else. */
@Serializable
data class DetailsRoute(val movieId: Int) : NavKey
