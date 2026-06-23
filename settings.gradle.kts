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
include("wiretap-coroutines")
include("demo")

project(":wiretap").projectDir = file("projects/wiretap")
project(":wiretap-slf4j").projectDir = file("projects/wiretap-slf4j")
project(":wiretap-coroutines").projectDir = file("projects/wiretap-coroutines")
project(":demo").projectDir = file("projects/demo")
