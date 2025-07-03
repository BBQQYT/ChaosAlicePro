// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.11.0" apply false // Версия может отличаться
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // Версия может отличаться
    // 👇 ВОТ ПРАВИЛЬНАЯ СТРОКА ДЛЯ КОРНЕВОГО ФАЙЛА
    id("com.google.dagger.hilt.android") version "2.49" apply false
}