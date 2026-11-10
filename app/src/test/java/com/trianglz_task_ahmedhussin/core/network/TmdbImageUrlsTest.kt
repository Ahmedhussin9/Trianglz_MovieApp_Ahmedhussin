package com.trianglz_task_ahmedhussin.core.network

import com.trianglz_task_ahmedhussin.core.network.TmdbImageUrls.BackdropSize
import com.trianglz_task_ahmedhussin.core.network.TmdbImageUrls.PosterSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbImageUrlsTest {

    @Test
    fun `poster uses the medium size by default`() {
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", TmdbImageUrls.poster("/abc.jpg"))
    }

    @Test
    fun `poster honours the requested size`() {
        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", TmdbImageUrls.poster("/abc.jpg", PosterSize.LARGE))
    }

    @Test
    fun `backdrop builds the full url`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w1280/bg.jpg",
            TmdbImageUrls.backdrop("/bg.jpg", BackdropSize.LARGE),
        )
    }

    @Test
    fun `path without a leading slash is still valid`() {
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", TmdbImageUrls.poster("abc.jpg"))
    }

    @Test
    fun `missing or blank path yields null so the UI can show a placeholder`() {
        assertNull(TmdbImageUrls.poster(null))
        assertNull(TmdbImageUrls.poster(""))
        assertNull(TmdbImageUrls.backdrop("   "))
    }
}
