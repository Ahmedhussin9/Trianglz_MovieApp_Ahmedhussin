package com.trianglz_task_ahmedhussin.core.network

/**
 * Builds full image URLs from the relative paths TMDb returns (for example `/abc123.jpg`).
 *
 * Sizes are chosen per use instead of `original` to keep downloads and the image cache small.
 */
object TmdbImageUrls {

    enum class PosterSize(val path: String) { SMALL("w185"), MEDIUM("w342"), LARGE("w500") }

    enum class BackdropSize(val path: String) { MEDIUM("w780"), LARGE("w1280") }

    /** @return the poster URL, or `null` when TMDb has no poster for the movie. */
    fun poster(path: String?, size: PosterSize = PosterSize.MEDIUM): String? = build(path, size.path)

    /** @return the backdrop URL, or `null` when TMDb has no backdrop for the movie. */
    fun backdrop(path: String?, size: BackdropSize = BackdropSize.MEDIUM): String? = build(path, size.path)

    private fun build(path: String?, size: String): String? {
        val trimmed = path?.trim()
        if (trimmed.isNullOrEmpty()) return null
        return TmdbConfig.IMAGE_BASE_URL + size + "/" + trimmed.removePrefix("/")
    }
}
