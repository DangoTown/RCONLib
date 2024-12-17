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

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    archives(sourceJar)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourceJar)
            artifactId = "rconlib"
        }
    }

    repositories {
        maven {
            url = uri("https://maven.rtast.cn/releases/")
            credentials {
                username = "RTAkland"
                password = System.getenv("PUBLISH_TOKEN")
            }
        }
    }
}
