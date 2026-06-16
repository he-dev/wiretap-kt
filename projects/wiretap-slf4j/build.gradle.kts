plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":wiretap"))
    api("org.slf4j:slf4j-api:2.0.17")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.21")
}

tasks.test {
    useJUnitPlatform()
}
