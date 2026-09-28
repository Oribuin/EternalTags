plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.1"
}

group = "dev.oribuin"
version = "2.0.0-pre3"


java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    disableAutoTargetJvm()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
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
    api("dev.rosewood:rosegarden:1.5.7")
    api("dev.triumphteam:triumph-gui:3.1.13") {  // https://mf.mattstudios.me/triumph-gui/introduction
        exclude(group = "com.google.code.gson", module = "gson") // Remove GSON, Already included in spigot api
        exclude(group = "net.kyori", module = "*") // Remove kyori
    }

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.5-alpha")
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
    }
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
            create<MavenPublication>("shadow") {
                from(components["shadow"])

                groupId = project.group as String
                artifactId = rootProject.name
                repositories {
                    if (project.hasProperty("mavenUser") && project.hasProperty("mavenPassword")) {
                        maven {
                            val version = project.version as String
                            credentials {
                                username = project.property("mavenUser") as String?
                                password = project.property("mavenPassword") as String?
                            }

                            val releasesRepoUrl = "https://repo.rosewooddev.io/repository/public-releases/"
                            val snapshotsRepoUrl = "https://repo.rosewooddev.io/repository/public-snapshots/"
                            url = uri(if (version.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                        }
                    }
                }
            }
        }
    }
    
    build {
        this.dependsOn(shadowJar)
    }

    this.named("generateMetadataFileForShadowPublication") {
        this.dependsOn("jar")
    }
}