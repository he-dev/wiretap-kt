plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":wiretap"))
    api(project(":wiretap-slf4j"))

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test-junit5
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.21")
}

tasks.test {
    useJUnitPlatform()
}
