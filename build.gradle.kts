import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("maven-publish")
}

val libVersion: String by project

group = "cn.rtast"
version = libVersion

repositories {
    mavenCentral()
}

tasks.compileKotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}

tasks.compileJava {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

publishing {
    repositories {
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/DangoTown/RCONLib")
            credentials {
                username = "RTAkland"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            groupId = "cn.rtast"
            artifactId = "rconlib"
            version = libVersion
        }
    }
}