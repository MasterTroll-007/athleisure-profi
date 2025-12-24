package com.fitness.app.data.repository

/**
 * A sealed class representing the result of an operation.
 * Used across all repositories for consistent error handling.
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed operation with an error message.
     * @param message User-friendly error message
     * @param exception Optional underlying exception for debugging
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : Result<Nothing>()

    /**
     * Represents an operation in progress.
     */
    data object Loading : Result<Nothing>()

    /**
     * Returns true if this is a Success result.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if this is an Error result.
     */
    val isError: Boolean get() = this is Error

    /**
     * Returns the data if this is a Success, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the data if this is a Success, or the default value otherwise.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * Maps the data if this is a Success.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    /**
     * Executes the action if this is a Success.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes the action if this is an Error.
     */
    inline fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, exception)
        return this
    }
}
