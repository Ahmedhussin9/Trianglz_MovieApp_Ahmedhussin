package com.trianglz_task_ahmedhussin.core.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `rememberNavBackStack` saves routes through kotlinx.serialization; if a route stopped
 * being serializable (or lost a field), the back stack could not be restored after
 * process death.
 */
class RoutesTest {

    @Test
    fun `details route survives a serialization round trip with its movie id`() {
        val route = DetailsRoute(movieId = 862)

        val restored = Json.decodeFromString(DetailsRoute.serializer(), Json.encodeToString(DetailsRoute.serializer(), route))

        assertEquals(route, restored)
    }

    @Test
    fun `movies route survives a serialization round trip`() {
        val restored = Json.decodeFromString(MoviesRoute.serializer(), Json.encodeToString(MoviesRoute.serializer(), MoviesRoute))

        assertEquals(MoviesRoute, restored)
    }
}
