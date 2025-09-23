// G:\Nova pasta\PlayerAndroid2\settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
    plugins {
        id("com.android.application")
        id("org.jetbrains.kotlin.android")
        // COMENTE OU REMOVA ESTA LINHA:
        // id("com.google.devtools.ksp") 
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "PlayerAndroid2"
include(":app")