package me.jochum.filmqueuer.adapters.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPersonSearchResponse(
    val page: Int,
    val results: List<TmdbPerson>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int,
)

@Serializable
data class TmdbPerson(
    val id: Int,
    val name: String,
    @SerialName("known_for_department")
    val knownForDepartment: String? = null,
    @SerialName("profile_path")
    val profilePath: String? = null,
    val popularity: Double = 0.0,
    @SerialName("known_for")
    val knownFor: List<TmdbKnownFor> = emptyList(),
)

@Serializable
data class TmdbKnownFor(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
)

@Serializable
data class TmdbPersonCreditsResponse(
    val cast: List<TmdbCastCredit> = emptyList(),
    val crew: List<TmdbCrewCredit> = emptyList(),
)

@Serializable
data class TmdbCastCredit(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("original_name")
    val originalName: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    @SerialName("vote_count")
    val voteCount: Int = 0,
    val overview: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    val character: String? = null,
)

@Serializable
data class TmdbCrewCredit(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("original_name")
    val originalName: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    @SerialName("vote_count")
    val voteCount: Int = 0,
    val overview: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    val job: String? = null,
    val department: String? = null,
)

@Serializable
data class TmdbMovieDetails(
    val id: Int,
    val title: String,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("poster_path")
    val posterPath: String? = null,
    val credits: TmdbCredits? = null,
    @SerialName("release_dates")
    val releaseDates: TmdbReleaseDatesResponse? = null,
) {
    // TMDB's top-level `release_date` is an editorially-chosen "primary" date whose accuracy
    // varies per film; it isn't reliably the film's true original release. This instead takes
    // the earliest Theatrical/Theatrical-limited date (type 2 or 3) across every country TMDB
    // has release_dates for - matching how Letterboxd defines a film's "Year" - and excludes
    // festival premieres (type 1) and digital/physical/TV dates (4-6), which don't count as the
    // original release. Falls back to the top-level field if release_dates has nothing usable.
    fun originalReleaseDate(): String? =
        releaseDates?.results
            ?.asSequence()
            ?.flatMap { it.releaseDates.asSequence() }
            ?.filter { it.type == 2 || it.type == 3 }
            ?.mapNotNull { it.releaseDate?.take(10) }
            ?.minOrNull()
            ?: releaseDate
}

@Serializable
data class TmdbReleaseDatesResponse(
    val results: List<TmdbCountryReleaseDates> = emptyList(),
)

@Serializable
data class TmdbCountryReleaseDates(
    @SerialName("iso_3166_1")
    val country: String,
    @SerialName("release_dates")
    val releaseDates: List<TmdbReleaseDateEntry> = emptyList(),
)

@Serializable
data class TmdbReleaseDateEntry(
    val type: Int,
    @SerialName("release_date")
    val releaseDate: String? = null,
)

@Serializable
data class TmdbCredits(
    val crew: List<TmdbCrewMember> = emptyList(),
)

@Serializable
data class TmdbCrewMember(
    val id: Int,
    val name: String,
    val job: String,
    @SerialName("profile_path")
    val profilePath: String? = null,
)

@Serializable
data class TmdbGenre(
    val id: Int,
    val name: String,
)

@Serializable
data class TmdbMovieSearchResponse(
    val page: Int,
    val results: List<TmdbMovie>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int,
)

@Serializable
data class TmdbMovie(
    val id: Int,
    val title: String,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    @SerialName("vote_count")
    val voteCount: Int = 0,
    val overview: String? = null,
)

@Serializable
data class TmdbTvSearchResponse(
    val page: Int,
    val results: List<TmdbTvShow>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int,
)

@Serializable
data class TmdbTvShow(
    val id: Int,
    val name: String,
    @SerialName("original_name")
    val originalName: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    @SerialName("vote_count")
    val voteCount: Int = 0,
    val overview: String? = null,
)

@Serializable
data class TmdbTvDetails(
    val id: Int,
    val name: String,
    @SerialName("original_name")
    val originalName: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    @SerialName("episode_run_time")
    val episodeRunTime: List<Int>? = null,
    @SerialName("number_of_episodes")
    val numberOfEpisodes: Int? = null,
    @SerialName("number_of_seasons")
    val numberOfSeasons: Int? = null,
    val seasons: List<TmdbSeason> = emptyList(),
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("aggregate_credits")
    val aggregateCredits: TmdbAggregateCredits? = null,
) {
    // TV credits don't have a single "Director" job the way a movie's /credits does - each
    // episode has its own crew. aggregate_credits rolls every episode's crew up across the whole
    // series, so this finds everyone ever credited as "Director" and ranks them by how many
    // episodes they directed - the show's most-consistent director (often the only one, for a
    // typical mini-series) leads the list, matching the "primary director" concept used for
    // co-directed movies.
    fun directorCrew(): List<TmdbAggregateCrewMember> =
        aggregateCredits?.crew
            ?.filter { member -> member.jobs.any { it.job == "Director" } }
            ?.sortedByDescending { it.totalEpisodeCount }
            ?: emptyList()
}

@Serializable
data class TmdbAggregateCredits(
    val crew: List<TmdbAggregateCrewMember> = emptyList(),
)

@Serializable
data class TmdbAggregateCrewMember(
    val id: Int,
    val name: String,
    @SerialName("profile_path")
    val profilePath: String? = null,
    val jobs: List<TmdbAggregateCrewJob> = emptyList(),
    @SerialName("total_episode_count")
    val totalEpisodeCount: Int = 0,
)

@Serializable
data class TmdbAggregateCrewJob(
    val job: String,
    @SerialName("episode_count")
    val episodeCount: Int = 0,
)

@Serializable
data class TmdbSeason(
    val id: Int,
    @SerialName("season_number")
    val seasonNumber: Int,
    @SerialName("episode_count")
    val episodeCount: Int,
)

@Serializable
data class TmdbSeasonDetails(
    val id: Int,
    @SerialName("season_number")
    val seasonNumber: Int,
    val episodes: List<TmdbEpisode>,
)

@Serializable
data class TmdbEpisode(
    val id: Int,
    @SerialName("episode_number")
    val episodeNumber: Int,
    val runtime: Int? = null,
)
