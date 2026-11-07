pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack for open-source libraries
        maven { url = java.net.URI("https://jitpack.io") }
        // AutoNavi / AMap maven repository
        maven { url = java.net.URI("https://repo1.maven.org/maven2/") }
    }
}

rootProject.name = "NewmanGPS"
include(":app")

