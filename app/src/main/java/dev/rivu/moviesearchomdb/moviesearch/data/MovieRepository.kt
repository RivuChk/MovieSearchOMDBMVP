package dev.rivu.moviesearchomdb.moviesearch.data

import androidx.room.EmptyResultSetException
import dev.rivu.moviesearchomdb.moviesearch.data.model.Movie
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

class MovieRepository(
    private val localDataStore: MovieDataStore,
    private val remoteDataStore: MovieDataStore
) : IMovieRepository {
    override fun getMovies(searchQuery: String): Flowable<List<Movie>> {
        return localDataStore.getMovies(searchQuery)
            .map {
                if (it.isNullOrEmpty()) {
                    throw EmptyResultSetException("Movies matching the query isn't in db")
                } else {
                    it
                }
            }
            .mergeWith {
                syncMovieSearchResult(searchQuery)
            }
            .onErrorResumeNext {
                syncMovieSearchResult(searchQuery)
                    .toFlowable()
            }
    }

    override fun syncMovieSearchResult(searchQuery: String): Single<List<Movie>> {
        return remoteDataStore.getMovies(searchQuery)
            .flatMap { movies ->
                if (movies.isNotEmpty()) {
                    addMovies(movies)
                        .andThen(
                            Single.just(movies)
                        )
                } else {
                    Single.error(EmptyResultSetException("No data found in Remote"))
                }
            }
    }

    override fun addMovies(movieList: List<Movie>): Completable {
        return localDataStore.addMovies(movieList)
    }

    override fun getSearchSuggestion(textEntered: String): Single<List<String>> {
        return localDataStore.getSearchSuggestion(textEntered)
    }
}