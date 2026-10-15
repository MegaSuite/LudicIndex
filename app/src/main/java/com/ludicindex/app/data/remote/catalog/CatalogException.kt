package com.ludicindex.app.data.remote.catalog

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Failures from the platform-catalog clients, each with a message meant for the UI. */
sealed class CatalogException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class Network(cause: Throwable) :
        CatalogException("网络连接失败，无法更新游戏库。", cause)

    class Server(val code: Int) :
        CatalogException("平台目录服务暂时不可用（HTTP $code）。")

    class Parse(cause: Throwable) :
        CatalogException("目录数据无法解析。", cause)
}

/** Runs a catalog request and turns transport/parse failures into [CatalogException]. */
internal suspend fun <T> catalogCall(block: suspend () -> T): T = try {
    block()
} catch (e: HttpException) {
    throw CatalogException.Server(e.code())
} catch (e: IOException) {
    throw CatalogException.Network(e)
} catch (e: SerializationException) {
    throw CatalogException.Parse(e)
}

/** Parses the ISO-8601 timestamps the catalogs use ("2025-11-04T16:00:00Z") into epoch seconds. */
internal fun parseIsoEpochSeconds(raw: String?): Long? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    val instant = runCatching { Instant.parse(value) }.getOrNull()
    if (instant != null) return instant.epochSecond
    return runCatching {
        LocalDate.parse(value.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().epochSecond
    }.getOrNull()
}
