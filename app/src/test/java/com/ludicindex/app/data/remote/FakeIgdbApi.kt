package com.ludicindex.app.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.HttpException
import retrofit2.Response

/** Records every call and replays a queued outcome, so the client can be tested without HTTP. */
class FakeIgdbApi : IgdbApi {

    data class RecordedCall(
        val url: String,
        val clientId: String,
        val bearer: String,
        val body: String,
    )

    val calls = mutableListOf<RecordedCall>()

    private val outcomes = ArrayDeque<Result<String>>()

    fun enqueueJson(json: String) {
        outcomes += Result.success(json)
    }

    fun enqueueHttpError(code: Int, errorBody: String = "{}") {
        outcomes += Result.failure(httpError(code, errorBody))
    }

    fun enqueueIoFailure(message: String = "boom") {
        outcomes += Result.failure(java.io.IOException(message))
    }

    override suspend fun post(
        url: String,
        clientId: String,
        bearer: String,
        body: RequestBody,
    ): ResponseBody {
        val buffer = Buffer()
        body.writeTo(buffer)
        calls += RecordedCall(url, clientId, bearer, buffer.readUtf8())

        val outcome = outcomes.removeFirstOrNull() ?: Result.success("[]")
        val json = outcome.getOrElse { throw it }
        return json.toResponseBody("application/json".toMediaType())
    }

    private fun httpError(code: Int, errorBody: String): HttpException = HttpException(
        Response.error<ResponseBody>(code, errorBody.toResponseBody("application/json".toMediaType())),
    )
}
