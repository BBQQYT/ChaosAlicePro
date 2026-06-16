package chaos.alice.pro.data.network

/**
 * Преобразование «сырых» ссылок GitHub репозитория CA-promt в ссылки
 * зеркала Forgejo (git.yuruyuri.fun). Используется для авто-фолбэка.
 */
object MirrorUrls {

    // Покрывает обе формы GitHub: /<branch>/ и /refs/heads/<branch>/
    private val GITHUB_RAW = Regex(
        """^https://raw\.githubusercontent\.com/BBQQYT/CA-promt/(?:refs/heads/)?([^/]+)/(.*)$"""
    )

    /**
     * Возвращает URL зеркала или null, если ссылка не относится
     * к CA-promt на GitHub (внешние ссылки вроде imgur не трогаем).
     */
    fun toMirror(url: String): String? {
        val m = GITHUB_RAW.matchEntire(url) ?: return null
        val branch = m.groupValues[1]
        val path = m.groupValues[2]
        return "https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/$branch/$path"
    }
}
