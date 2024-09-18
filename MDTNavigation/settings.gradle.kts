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
            // Do not change the username below. It should always be "mapbox" (not your username).
            credentials.username = "mapbox"
            // Use the secret token stored in gradle.properties as the password
            credentials.password = "sk.eyJ1IjoibWFzaGVyYW5zYXJpIiwiYSI6ImNtMTRqdXV4MTFodjQybXNnN3N2N3U5NTIifQ.-xMWRcYtDqh9_9o2nIoS1w"
            authentication.create<BasicAuthentication>("basic")
        }
    }
}

rootProject.name = "MDTNavigation"
include(":app")
 