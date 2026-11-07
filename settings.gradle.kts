rootProject.name = "Lockruna"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":api-contract")
include(":shared:domain")
include(":shared:application")
include(":shared:data")
include(":shared:presentation")
include(":shared:umbrella")
include(":androidApp")
include(":server")
