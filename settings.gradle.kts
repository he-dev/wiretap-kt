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
include("wiretap-slf4j")
include("demo")

project(":wiretap").projectDir = file("projects/wiretap")
project(":wiretap-slf4j").projectDir = file("projects/wiretap-slf4j")
project(":demo").projectDir = file("projects/demo")
