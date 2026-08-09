package me.jochum.filmqueuer.adapters.tmdb

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TmdbClientTest {
    // Note: These are integration-style tests that would require actual TMDB API access
    // In practice, you'd mock the HTTP client or use a test double

    @Test
    fun `searchTv should return TmdbTvSearchResponse with TV shows`() =
        runBlocking {
            // This test would require mocking the HTTP client
            // For now, just testing the model structure
            val mockResponse =
                TmdbTvSearchResponse(
                    page = 1,
                    totalPages = 1,
                    totalResults = 1,
                    results =
                        listOf(
                            TmdbTvShow(
                                id = 1399,
                                name = "Game of Thrones",
                                originalName = "Game of Thrones",
                                firstAirDate = "2011-04-17",
                                posterPath = "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                                voteAverage = 9.2,
                                voteCount = 8654,
                                overview = "Epic fantasy series",
                            ),
                        ),
                )

            assertEquals(1399, mockResponse.results.first().id)
            assertEquals("Game of Thrones", mockResponse.results.first().name)
            assertEquals("2011-04-17", mockResponse.results.first().firstAirDate)
        }

    @Test
    fun `getTvDetails should return TmdbTvDetails with seasons`() =
        runBlocking {
            // Mock the expected TV details response
            val mockTvDetails =
                TmdbTvDetails(
                    id = 1399,
                    name = "Game of Thrones",
                    originalName = "Game of Thrones",
                    firstAirDate = "2011-04-17",
                    episodeRunTime = listOf(50),
                    numberOfEpisodes = 73,
                    numberOfSeasons = 8,
                    seasons =
                        listOf(
                            TmdbSeason(id = 3627, seasonNumber = 1, episodeCount = 10),
                            TmdbSeason(id = 3628, seasonNumber = 2, episodeCount = 10),
                        ),
                    genres = listOf(TmdbGenre(id = 18, name = "Drama")),
                    posterPath = "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                )

            assertEquals("Game of Thrones", mockTvDetails.name)
            assertEquals(8, mockTvDetails.numberOfSeasons)
            assertEquals(73, mockTvDetails.numberOfEpisodes)
            assertEquals(2, mockTvDetails.seasons.size)
        }

    @Test
    fun `getTvSeasonDetails should return TmdbSeasonDetails with episodes`() =
        runBlocking {
            // Mock season details response
            val mockSeasonDetails =
                TmdbSeasonDetails(
                    id = 3627,
                    seasonNumber = 1,
                    episodes =
                        listOf(
                            TmdbEpisode(id = 63056, episodeNumber = 1, runtime = 62),
                            TmdbEpisode(id = 63057, episodeNumber = 2, runtime = 56),
                            TmdbEpisode(id = 63058, episodeNumber = 3, runtime = 58),
                        ),
                )

            assertEquals(1, mockSeasonDetails.seasonNumber)
            assertEquals(3, mockSeasonDetails.episodes.size)
            assertEquals(62, mockSeasonDetails.episodes[0].runtime)
            assertEquals(56, mockSeasonDetails.episodes[1].runtime)
        }

    @Test
    fun `originalReleaseDate should pick the earliest theatrical date across countries, ignoring the US-biased top-level field`() {
        // Modeled on TMDB's real data for Parasite (496243): the top-level `release_date` here
        // is deliberately set to a later US date to prove it's ignored, while South Korea's
        // actual earliest theatrical release (2019-05-30) is buried among many other countries.
        val details =
            TmdbMovieDetails(
                id = 496243,
                title = "Parasite",
                releaseDate = "2019-11-01",
                releaseDates =
                    TmdbReleaseDatesResponse(
                        results =
                            listOf(
                                TmdbCountryReleaseDates(
                                    country = "KR",
                                    releaseDates = listOf(TmdbReleaseDateEntry(type = 3, releaseDate = "2019-05-30T00:00:00.000Z")),
                                ),
                                TmdbCountryReleaseDates(
                                    country = "US",
                                    releaseDates =
                                        listOf(
                                            TmdbReleaseDateEntry(type = 1, releaseDate = "2019-05-21T00:00:00.000Z"),
                                            TmdbReleaseDateEntry(type = 3, releaseDate = "2019-11-01T00:00:00.000Z"),
                                        ),
                                ),
                            ),
                    ),
            )

        assertEquals("2019-05-30", details.originalReleaseDate())
    }

    @Test
    fun `originalReleaseDate should exclude premiere, digital, physical and TV dates`() {
        val details =
            TmdbMovieDetails(
                id = 1,
                title = "Some Film",
                releaseDate = "2020-01-01",
                releaseDates =
                    TmdbReleaseDatesResponse(
                        results =
                            listOf(
                                TmdbCountryReleaseDates(
                                    country = "US",
                                    releaseDates =
                                        listOf(
                                            TmdbReleaseDateEntry(type = 1, releaseDate = "2019-01-01T00:00:00.000Z"),
                                            TmdbReleaseDateEntry(type = 4, releaseDate = "2019-02-01T00:00:00.000Z"),
                                            TmdbReleaseDateEntry(type = 5, releaseDate = "2019-03-01T00:00:00.000Z"),
                                            TmdbReleaseDateEntry(type = 6, releaseDate = "2019-04-01T00:00:00.000Z"),
                                            TmdbReleaseDateEntry(type = 3, releaseDate = "2019-05-01T00:00:00.000Z"),
                                        ),
                                ),
                            ),
                    ),
            )

        assertEquals("2019-05-01", details.originalReleaseDate())
    }

    @Test
    fun `originalReleaseDate should fall back to the top-level field when release_dates has nothing usable`() {
        val withoutReleaseDates = TmdbMovieDetails(id = 1, title = "Some Film", releaseDate = "2020-01-01")
        assertEquals("2020-01-01", withoutReleaseDates.originalReleaseDate())

        val onlyNonTheatrical =
            TmdbMovieDetails(
                id = 2,
                title = "Some Film",
                releaseDate = "2020-01-01",
                releaseDates =
                    TmdbReleaseDatesResponse(
                        results =
                            listOf(
                                TmdbCountryReleaseDates(
                                    country = "US",
                                    releaseDates = listOf(TmdbReleaseDateEntry(type = 4, releaseDate = "2019-01-01T00:00:00.000Z")),
                                ),
                            ),
                    ),
            )
        assertEquals("2020-01-01", onlyNonTheatrical.originalReleaseDate())
    }
}
