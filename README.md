<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12&height=200&section=header&text=Chaos%20Alice%20Pro%20%E2%9A%A1%EF%B8%8F&fontSize=50&fontColor=ffffff&animation=fadeIn&fontAlignY=35" />
  <h1>🤖 Android-клиент для ИИ-персонажей</h1>
  <img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=20&duration=3000&pause=1000&color=6366F1&center=true&vCenter=true&width=700&lines=%F0%9F%94%97+Google+Gemini+%7C+OpenAI+%7C+OpenRouter;%F0%9F%93%A6+Jetpack+Compose+%7C+MVVM+%7C+Hilt;%F0%9F%8E%A8+Streaming+UI+%7C+Fork+%D0%B4%D0%B8%D0%B0%D0%BB%D0%BE%D0%B3%D0%BE%D0%B2" />
  <p>
    <img src="https://img.shields.io/github/stars/BBQQYT/ChaosAlicePro?style=for-the-badge&color=ff6b6b" />
    <img src="https://img.shields.io/github/last-commit/BBQQYT/ChaosAlicePro?style=for-the-badge&color=6366f1" />
    <img src="https://img.shields.io/github/license/BBQQYT/ChaosAlicePro?style=for-the-badge&color=10b981" />
  </p>
</div>

---

## 🚀 О проекте

**Chaos Alice Pro** — НЕКОММЕРЧЕСКОЕ Android-приложение на Kotlin. Создано как лаборатория для экспериментов с AI-моделями и быстро выросло в удобный клиент общения с персонажами.

---

## ✨ Возможности

- 🌐 Поддержка провайдеров: Google Gemini, OpenAI, OpenRouter
- ⚡ Streaming-ответы (эффект печати) и остановка генерации
- 🧵 Управление диалогом: редактирование, удаление, форк ветки
- 🖼️ Отправка изображений для мультимоделей Gemini (v2.0+)
- 🌍 Прокси (HTTP/SOCKS), проверка соединения, разделение трафика
- 🧩 Персонажи: загрузка внешних JSON с ролями и промптами

---

## 🧰 Технологии

- Язык: Kotlin
- UI: Jetpack Compose
- Архитектура: MVVM
- DI: Hilt
- Сеть: Retrofit, OkHttp, Ktor Client
- БД: Room | Настройки: DataStore | Изображения: Coil
- Сериализация: Kotlinx.serialization

---

## 📦 Установка

```bash
# Клонирование
git clone https://github.com/BBQQYT/ChaosAlicePro.git
cd ChaosAlicePro

# Сборка
./gradlew assembleDebug
```

Откройте проект в Android Studio и запустите на устройстве/эмуляторе.

---

## 🔧 Настройка провайдеров

- В настройках приложения укажите API-ключи для выбранных провайдеров
- Выберите предпочитаемые модели из списка
- Для прокси включите параметры сети и при необходимости задайте логин/пароль

---

## 🗺️ Дорожная карта

1. Улучшение адаптации ИИ к длине сообщений
2. Чат с несколькими ИИ одновременно
3. Попробую починить баг с отправкой фото
4. Добавить API Perplexity
5. Возможно: генерация фото от ИИ (Nano Banana)

---

## 📄 Лицензия

MIT License — см. файл LICENSE.

## 🙏 Благодарности

- Арты: **@oobiiooddddooo**, **@PalmaDerevio** (Telegram)
- Спасибо Google, OpenAI, OpenRouter и Яндексу за технологии и вдохновение
