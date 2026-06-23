plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":wiretap"))
    implementation(project(":wiretap-slf4j"))
    implementation(project(":wiretap-coroutines"))
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.25.2")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.2")
}

application {
    mainClass.set("wiretap.demo.MainKt")
}
