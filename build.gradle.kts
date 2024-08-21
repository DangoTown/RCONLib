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

tasks.register<Jar>("sourceJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    archives(tasks.named("sourceJar"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourceJar"])
        }
    }

    repositories {
        maven {
            url = uri("https://repo.rtast.cn/api/v4/projects/3/packages/maven")
            credentials {
                username = "RTAkland"
                password = System.getenv("TOKEN")
            }
        }
    }
}
