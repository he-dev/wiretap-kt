plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.21")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.21")
}

tasks.test {
    useJUnitPlatform()
}
