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
<<<<<<< HEAD

@Suppress("UnstableApiUsage")
=======
>>>>>>> f710fb1 (Merge)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

<<<<<<< HEAD
rootProject.name = "Habitizer"
include(":app")
include(":lib")
include(":observables")
=======
rootProject.name = "Habitizer-Test"
include(":app")
>>>>>>> f710fb1 (Merge)
