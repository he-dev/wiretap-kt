plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":wiretap"))
}

application {
    mainClass.set("wiretap.demo.MainKt")
}
