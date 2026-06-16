# Зеркало git для персонажей: GitHub → git.yuruyuri.fun (Forgejo) с авто-фолбэком

Документ-план. Код не меняется — здесь описан подход, точные замены URL и готовые сниппеты, которые можно применить.

## TL;DR

Приложение тянет персонажей с `raw.githubusercontent.com/BBQQYT/CA-promt`. Чтобы добавить зеркало `git.yuruyuri.fun/BBQQ_YT/CA-promt` с **авто-фолбэком** (GitHub основной, зеркало — при сбое/блокировке), нужно:

1. Создать на Forgejo **pull-mirror** репозитория из GitHub (репозиторий-зеркало синхронизируется сам).
2. В приложении добавить **один OkHttp-перехватчик** (`Interceptor`), который при ошибке запроса к GitHub автоматически повторяет тот же запрос на зеркало, переписывая URL по правилам ниже.
3. Подключить этот перехватчик к Retrofit-клиентам (JSON + промпты) и к Coil ImageLoader (иконки). Больше ничего трогать не нужно — JSON-файлы и UI-код остаются как есть.

Ключевое преимущество подхода: **единый источник правды**. JSON в зеркале не редактируется (внутри остаются ссылки на GitHub), всё переписывание происходит в рантайме только при фактическом сбое.

---

## 1. Что сейчас зависит от GitHub

Приложение загружает список персонажей и сам контент так:

| Что грузится | Откуда | Где в коде |
|---|---|---|
| `pers.json` (офиц. персонажи) | `raw.githubusercontent.com/BBQQYT/CA-promt/main/pers.json` | `data/network/PersonaApiService.kt:7` |
| `cus_pers.json` (кастомные) | `raw.githubusercontent.com/BBQQYT/CA-promt/main/cus_pers.json` | `data/network/PersonaApiService.kt:10` |
| `prompt_url` каждого персонажа | ссылка внутри JSON, напр. `.../refs/heads/main/personas/prompt_alisa.txt` | `data/PersonaRepository.kt:53` (`getPrompt`) |
| `icon_url` каждого персонажа | ссылка внутри JSON (часть — `i.imgur.com`, внешние) | Coil `AsyncImage(model = persona.icon_url)` в 7 экранах |
| baseUrl Retrofit (Direct) | `https://raw.githubusercontent.com/` | `di/NetworkModule.kt:130` |

Важные нюансы:

- Внутри `pers.json` ссылки имеют форму `.../CA-promt/refs/heads/main/...`, а захардкоженные — форму `.../CA-promt/main/...`. Правило преобразования должно покрывать **обе** формы.
- Часть `icon_url` указывает на `i.imgur.com` — это внешние ссылки, их трогать **нельзя**.
- `icon_url` грузится Coil-ом в 7 местах (`ChatScreen`, `ChatListScreen`, `ChatGPTContainerScreen`, `TelegramChatScreen`, `YouTubeChatListScreen`). Поэтому фолбэк для картинок удобнее делать централизованно на сетевом слое, а не в каждом экране.

---

## 2. Правила преобразования URL (GitHub raw → Forgejo raw)

Forgejo/Gitea отдают «сырые» файлы по другому пути, чем GitHub. Отличий три: **хост**, **владелец** и **путь**.

| Элемент | GitHub | Forgejo (git.yuruyuri.fun) |
|---|---|---|
| Хост | `raw.githubusercontent.com` | `git.yuruyuri.fun` |
| Владелец | `BBQQYT` | `BBQQ_YT` (с подчёркиванием!) |
| Репозиторий | `CA-promt` | `CA-promt` (без изменений) |
| Путь к ветке | `/<branch>/...` или `/refs/heads/<branch>/...` | `/raw/branch/<branch>/...` |

Итоговый шаблон Forgejo:

```
https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/<branch>/<path>
```

### Проверенные примеры (на реальных URL из текущего pers.json)

```
https://raw.githubusercontent.com/BBQQYT/CA-promt/main/pers.json
  → https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/main/pers.json

https://raw.githubusercontent.com/BBQQYT/CA-promt/main/cus_pers.json
  → https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/main/cus_pers.json

https://raw.githubusercontent.com/BBQQYT/CA-promt/refs/heads/main/img/alice.png
  → https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/main/img/alice.png

https://raw.githubusercontent.com/BBQQYT/CA-promt/refs/heads/main/personas/prompt_alisa.txt
  → https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/main/personas/prompt_alisa.txt

https://i.imgur.com/BFEyIeI.png
  → (не меняется — внешняя ссылка)
```

### Регулярное выражение

```
^https://raw\.githubusercontent\.com/BBQQYT/CA-promt/(?:refs/heads/)?([^/]+)/(.*)$
```

Замена: `https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/$1/$2`

Группа `$1` — имя ветки (работает и для `main`, и для любой другой), `$2` — остальной путь. Любой URL, не подходящий под шаблон (imgur, чужие репозитории), остаётся без изменений.

---

## 3. Шаг 1 — создать зеркало на Forgejo

Зеркало должно само синхронизироваться с GitHub, чтобы персонажей не нужно было заливать вручную.

1. На `git.yuruyuri.fun` под аккаунтом `BBQQ_YT`: **+ → New Migration → GitHub**.
2. Source/Clone URL: `https://github.com/BBQQYT/CA-promt`.
3. Включить галочку **«This repository will be a mirror»** (pull-mirror). Owner: `BBQQ_YT`, имя репозитория: `CA-promt`.
4. Запустить миграцию. Дальше Forgejo сам периодически подтягивает изменения из GitHub.
5. (Опц.) В **Settings → Mirror Settings** уменьшить интервал синхронизации (по умолчанию 8h) до приемлемого, напр. `1h`, чтобы новые персонажи быстрее появлялись на зеркале.

Проверки после создания:

- Имя ветки на зеркале — `main` (как на GitHub), иначе пути `raw/branch/main/...` не сработают.
- Открыть в браузере: `https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/main/pers.json` — должен отдаться JSON.
- Проверить картинку: `https://git.yuruyuri.fun/BBQQ_YT/CA-promt/raw/branch/main/img/alice.png`.

> Если Forgejo для бинарных файлов отдаёт их через `/media/branch/...`, путь `/raw/branch/...` всё равно работает для raw-доступа — он отдаёт содержимое как файл. Если вдруг с картинками будут проблемы, для них можно использовать `/media/branch/<branch>/<path>` вместо `/raw/branch/...` (логику легко вынести в ту же функцию преобразования).

---

## 4. Шаг 2 — авто-фолбэк в приложении (рекомендуемый подход)

Идея: **не менять** ни `PersonaApiService`, ни `PersonaRepository`, ни JSON. Вместо этого добавить перехватчик на уровне OkHttp, который ловит сбой запроса к GitHub и прозрачно повторяет его на зеркало. Один перехватчик закрывает сразу всё: список персонажей, промпты и (через общий ImageLoader) иконки.

### 4.1 Хелпер преобразования URL

Новый файл, напр. `app/src/main/java/chaos/alice/pro/data/network/MirrorUrls.kt`:

```kotlin
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
```

### 4.2 Перехватчик с фолбэком

Новый файл, напр. `app/src/main/java/chaos/alice/pro/data/network/GithubMirrorFallbackInterceptor.kt`:

```kotlin
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

        // 2) GitHub ответил, но неуспешно (403/451/5xx и т.п.) → зеркало.
        if (!response.isSuccessful) {
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
```

### 4.3 Подключение к Retrofit-клиентам (`di/NetworkModule.kt`)

Перехватчик нужно добавить в OkHttp-клиенты, которые использует `PersonaApiService` (это `@ProxyClient`; для надёжности — и в `@DirectClient`). Добавляем `.addInterceptor(GithubMirrorFallbackInterceptor())` и заодно короткие таймауты, чтобы фолбэк срабатывал быстро, а не висел на дефолтных 10 c.

В `provideOkHttpClientWithProxy(...)` и `provideOkHttpClientDirect()`:

```kotlin
import java.util.concurrent.TimeUnit
// ...
val clientBuilder = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor(GithubMirrorFallbackInterceptor()) // ← фолбэк на зеркало
    .connectTimeout(6, TimeUnit.SECONDS)               // ← быстрый отказ → быстрый фолбэк
    .readTimeout(10, TimeUnit.SECONDS)
    .callTimeout(20, TimeUnit.SECONDS)
```

> Порядок важен: `GithubMirrorFallbackInterceptor` — это application-interceptor, его достаточно добавить один раз на клиент. Прокси-настройки и фолбэк совместимы: при повторе на зеркало тот же клиент использует тот же прокси.

`PersonaApiService` и `PersonaRepository` менять **не нужно** — захардкоженные GitHub-URL остаются, перехватчик подменит их при сбое.

### 4.4 Подключение к Coil (иконки персонажей)

Coil по умолчанию использует свой OkHttp-клиент, поэтому перехватчик до картинок не дойдёт. Чтобы иконки тоже падали на зеркало, дайте Coil свой ImageLoader с тем же перехватчиком. В Coil 2.7.0 (используется в проекте) это делается через `ImageLoaderFactory` на классе `Application`.

`ChaosAliceApp.kt`:

```kotlin
package chaos.alice.pro

import android.app.Application
import chaos.alice.pro.data.network.GithubMirrorFallbackInterceptor
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ChaosAliceApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }

    override fun newImageLoader(): ImageLoader {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(GithubMirrorFallbackInterceptor())
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttp)
            .build()
    }
}
```

После этого все `AsyncImage(model = persona.icon_url, ...)` автоматически используют этот ImageLoader — менять 7 экранов не нужно. Внешние `i.imgur.com` грузятся напрямую (перехватчик их пропускает).

---

## 5. Что НЕ нужно делать

- **Не редактировать `pers.json`/`cus_pers.json` в зеркале.** Они остаются с GitHub-ссылками внутри. Переписывание — только в рантайме и только при сбое. Это сохраняет единый источник правды: контент правится в одном месте (GitHub), зеркало синхронизируется само.
- **Не менять UI-экраны и `PersonaRepository`.** Перехватчик работает прозрачно ниже по стеку.
- **Не плодить второй вариант JSON** под зеркало — это удвоит поддержку и приведёт к рассинхрону.

---

## 6. Граничные случаи и заметки

- **Тип блокировки.** При DNS-спуфинге/SNI-блоке GitHub отдаёт `IOException` (UnknownHost/timeout/reset) — это ловится в `catch`. При явной блокировке через прокси-страницу может прийти `403/451` — ловится проверкой `!response.isSuccessful`. Оба сценария уводят на зеркало.
- **Ветка.** Регулярка вытаскивает имя ветки, поэтому всё работает, даже если когда-то ветку переименуют (`main`/`master`/др.) — лишь бы такая ветка существовала на зеркале.
- **Двойная задержка при первой загрузке.** Если GitHub «висит» до таймаута, фолбэк добавляет ожидание. Поэтому в 4.3/4.4 заданы короткие `connectTimeout`/`readTimeout`. При желании можно кэшировать факт «GitHub недоступен» на время сессии и сразу ходить на зеркало (простая `@Volatile`-переменная-флаг в перехватчике).
- **Проверка работоспособности.** Самый простой тест: временно подменить хост GitHub в hosts/через AdGuard на «недоступно» (или отключить доступ к `raw.githubusercontent.com`) и убедиться, что персонажи, промпты и иконки всё равно грузятся — значит фолбэк сработал.
- **HTTP-Referer.** В `GenericLlmApiService.kt:22` referer `https://github.com/BBQQYT/CA-promt` — это просто заголовок для OpenRouter-совместимых API, к загрузке персонажей отношения не имеет, менять не требуется.

---

## 7. Альтернатива на будущее — ручной переключатель

Если позже понадобится дать пользователю явный выбор источника (GitHub / Зеркало / Авто), достаточно:

- добавить в `SettingsRepository` поле `personaSource` (enum `GITHUB | MIRROR | AUTO`);
- в перехватчике учитывать его: `MIRROR` — переписывать всегда сразу; `GITHUB` — никогда; `AUTO` — текущее поведение с фолбэком;
- добавить пункт в экран настроек.

Базовая инфраструктура (`MirrorUrls` + перехватчик) при этом не меняется — переключатель надстраивается сверху.

---

## 8. Чек-лист внедрения

1. [ ] Создать pull-mirror `BBQQ_YT/CA-promt` на git.yuruyuri.fun, ветка `main`.
2. [ ] Проверить `…/raw/branch/main/pers.json` и `…/raw/branch/main/img/alice.png` в браузере.
3. [ ] Добавить `MirrorUrls.kt` и `GithubMirrorFallbackInterceptor.kt`.
4. [ ] Подключить перехватчик + таймауты в `NetworkModule` (оба OkHttp-клиента).
5. [ ] Реализовать `ImageLoaderFactory` в `ChaosAliceApp`.
6. [ ] Протестировать при заблокированном GitHub (персонажи, промпты, иконки грузятся).
