pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create("basic", BasicAuthentication::class)
            }
            credentials {
                username = "mapbox"
                password = System.getProperty("MAPBOX_DOWNLOADS_TOKEN")
            }
        }

    }
}

rootProject.name = "MDTNavigation"
include(":app")
 