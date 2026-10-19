// Корневой файл build.gradle.kts
// Здесь мы только объявляем плагины для всего проекта, не применяя их напрямую.
// Версии берутся из gradle/libs.versions.toml, чтобы не дублировать их здесь.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.android) apply false
}
