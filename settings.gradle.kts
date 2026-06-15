pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}

rootProject.name = "wiretap-kt"

include("wiretap")
include("demo")

project(":wiretap").projectDir = file("projects/wiretap")
project(":demo").projectDir = file("projects/demo")
