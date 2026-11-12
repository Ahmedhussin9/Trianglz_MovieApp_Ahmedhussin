package com.trianglz_task_ahmedhussin.feature.details.data.repository

import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.feature.details.data.local.MovieDetailsDao
import com.trianglz_task_ahmedhussin.feature.details.data.mapper.toDomain
import com.trianglz_task_ahmedhussin.feature.details.data.mapper.toEntity
import com.trianglz_task_ahmedhussin.feature.details.data.remote.MovieDetailsRemoteDataSource
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.repository.MovieDetailsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cache-then-network movie details (R7, R38, requirements Q5), following the emission
 * contract of [MovieDetailsRepository.movieDetails].
 *
 * Room's suspend DAO functions and Retrofit's suspend calls are main-safe on their own, so
 * the flow needs no dispatcher switch.
 */
class MovieDetailsRepositoryImpl @Inject constructor(
    private val remoteDataSource: MovieDetailsRemoteDataSource,
    private val dao: MovieDetailsDao,
) : MovieDetailsRepository {

    override fun movieDetails(id: Int): Flow<AppResult<MovieDetails>> = flow {
        val cached = readCache(id)
        if (cached != null) {
            Timber.d("Details of movie %d: cache hit", id)
            emit(AppResult.Success(cached))
        } else {
            Timber.d("Details of movie %d: cache miss", id)
        }

        val fresh = remoteDataSource.movieDetails(id)
        if (fresh is AppResult.Success) {
            writeCache(fresh.data)
            Timber.d("Details of movie %d: refreshed", id)
        }
        emit(fresh)
    }

    /** A cache that cannot be read is treated as empty: the network can still serve the screen. */
    private suspend fun readCache(id: Int): MovieDetails? = try {
        dao.get(id)?.toDomain()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Reading cached details of movie %d failed", id)
        null
    }

    /** Fresh details are still shown when saving them fails; only offline use is lost. */
    private suspend fun writeCache(details: MovieDetails) {
        try {
            dao.upsert(details.toEntity())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Caching details of movie %d failed", details.id)
        }
    }
}
