plugins {
    kotlin("jvm") version "2.1.0"
    application
    jacoco
    id("com.diffplug.spotless") version "6.19.0"
    `maven-publish`
}

group = "jp.skypencil"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.9.3"
val guavaVersion = "33.5.0-jre"
val immutablesVersion = "2.9.3"

dependencies {
    implementation("org.ow2.asm:asm-analysis:9.5")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.immutables:value:$immutablesVersion:annotations")
    annotationProcessor("org.immutables:value:$immutablesVersion")

    testImplementation("com.google.guava:guava:$guavaVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("jp.skypencil.Main")
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.named("check") {
    dependsOn("jacocoTestReport")
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "jp.skypencil"
            from(components["java"])
        }
    }
}

defaultTasks("spotlessApply", "build")
