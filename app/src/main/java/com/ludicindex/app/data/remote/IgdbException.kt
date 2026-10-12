package com.ludicindex.app.data.remote

/** Every failure the IGDB layer can surface, each with a message meant for the UI. */
sealed class IgdbException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class MissingCredentials :
        IgdbException("尚未配置 IGDB 凭据。请到「设置」填写 Client ID 与 Client Secret。")

    class InvalidCredentials(cause: Throwable? = null) :
        IgdbException("凭据无效或已失效，请检查 Client ID 与 Client Secret。", cause)

    class RateLimited :
        IgdbException("请求过于频繁，已触发 IGDB 限流（4 次/秒），请稍后再试。")

    class BadRequest(detail: String) :
        IgdbException("IGDB 拒绝了本次查询：$detail")

    class ServerError(val code: Int) :
        IgdbException("IGDB 服务暂时不可用（HTTP $code），请稍后重试。")

    class Network(cause: Throwable) :
        IgdbException("网络连接失败，请检查网络后重试。", cause)
}
