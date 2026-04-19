plugins {
    id("java-library")
    alias(libs.plugins.run.velocity)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
    compileOnly(libs.snakeyaml)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.api.get())
    }
}
