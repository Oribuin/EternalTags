import org.apache.tools.ant.filters.ExpandProperties

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.5"
}

group = "dev.oribuin"
version = "2.0.0"


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    
    disableAutoTargetJvm()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://libraries.minecraft.net")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.helpch.at/releases/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.mattstudios.me/artifactory/public/")
}

dependencies {
    api("dev.rosewood:rosegarden:1.4.4")
    api("dev.triumphteam:triumph-gui:3.1.10") {  // https://mf.mattstudios.me/triumph-gui/introduction
        exclude(group = "com.google.code.gson", module = "gson") // Remove GSON, Already included in spigot api
        exclude(group = "net.kyori", module = "*") // Remove kyori
    }
    
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks {
    compileJava {
        this.options.compilerArgs.add("-parameters")
        this.options.isFork = true
        this.options.encoding = "UTF-8"
    }

    shadowJar {
        this.archiveClassifier.set("")

        this.relocate("dev.rosewood.rosegarden", "${project.group}.eternaltags.libs.rosegarden")
        this.relocate("dev.triumphteam.gui", "${project.group}.eternaltags.libs.gui")

        // rosegarden relocation
        this.relocate("com.zaxxer", "${project.group}.eternaltags.libs.hikari")
        this.relocate("org.slf4j", "${project.group}.eternaltags.libs.slf4j")
    }

    processResources {
        this.filesMatching("**/plugin.yml") {
            this.expand("version" to project.version)
        }
    }

    publishing {
        publications {
            create("shadow", MavenPublication::class) {
                project.shadow.component(this)
                this.artifactId = "eternaltags"
                this.pom.name.set("eternaltags")
            }
        }

        repositories {
            val version = project.version as String
            val mavenUser = project.properties["mavenUser"] as String?
            val mavenPassword = project.properties["mavenPassword"] as String?

            if (mavenUser != null && mavenPassword != null) {
                maven {
                    credentials {
                        username = mavenUser
                        password = mavenPassword
                    }

                    val releasesRepoUrl = "https://repo.rosewooddev.io/repository/public-releases/"
                    val snapshotsRepoUrl = "https://repo.rosewooddev.io/repository/public-snapshots/"
                    url = uri(if (version.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                }
            }
        }
    }

    build {
        this.dependsOn(shadowJar)
    }
    
}