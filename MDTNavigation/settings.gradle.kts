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

//        maven(url = "https://mapbox.bintray.com/mapbox") {
//            credentials {
//                username = "mapbox"
//                password = "sk.eyJ1Ijoic2FtYXNoIiwiYSI6ImNrdGNtenJodjI2dzYydmpwbDVybnZic3IifQ.3hAuiE7ZsDWMIWJFb_r2cg"
//            }
//        }
//
//        maven(url = "https://api.mapbox.com/downloads/v2/releases/maven") {
//            credentials {
//                username = "mapbox"
//                password = "sk.eyJ1Ijoic2FtYXNoIiwiYSI6ImNrdHdpNHQzZjExbTUyd21yeWhiNGVhdXYifQ.o0xPxBIZmIib6HOqM5VB8Q"
//            }
//        }
    }
}

rootProject.name = "MDTNavigation"
include(":app")
 