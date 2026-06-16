package chaos.alice.pro.data.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Если запрос к raw.githubusercontent.com (репозиторий CA-promt) не удался
 * — сетевая ошибка (типично для блокировки/DNS) или неуспешный HTTP-код —
 * повторяет тот же запрос на зеркало Forgejo (git.yuruyuri.fun).
 */
class GithubMirrorFallbackInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mirrorUrl = MirrorUrls.toMirror(request.url.toString())

        // Не наш CA-promt/GitHub — работаем как обычно.
        if (mirrorUrl == null) return chain.proceed(request)

        // 1) Пробуем GitHub.
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            // DNS/таймаут/reset — типичные симптомы блокировки → сразу на зеркало.
            Log.w("MirrorFallback", "GitHub недоступен, пробуем зеркало: $mirrorUrl", e)
            return proceedMirror(chain, request, mirrorUrl)
        }

        // 2) GitHub ответил. 304 Not Modified и 2xx — это успех (в т.ч. условные
        //    запросы кэша Coil), фолбэк НЕ нужен. Уходим на зеркало только при явных
        //    признаках блокировки (403/451) или серверной ошибке GitHub (5xx).
        if (response.code == 403 || response.code == 451 || response.code in 500..599) {
            Log.w("MirrorFallback", "GitHub вернул ${response.code}, пробуем зеркало: $mirrorUrl")
            response.close()
            return proceedMirror(chain, request, mirrorUrl)
        }

        return response
    }

    private fun proceedMirror(
        chain: Interceptor.Chain,
        original: Request,
        mirrorUrl: String
    ): Response {
        val mirrorRequest = original.newBuilder().url(mirrorUrl).build()
        return chain.proceed(mirrorRequest)
    }
}
