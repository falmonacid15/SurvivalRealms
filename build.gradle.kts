plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "org.falmdev"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://repo.minuskube.fr/public")
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
    exclude(group = "org.bukkit",   module = "bukkit")
    exclude(group = "org.bukkit",   module = "craftbukkit")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("me.clip:placeholderapi:2.12.2")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("fr.minuskube.inv:smart-invs:1.2.7")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("at.favre.lib.crypto.bcrypt", "org.falmdev.libs.bcrypt")
        relocate("com.zaxxer.hikari",          "org.falmdev.libs.hikari")
        relocate("fr.minuskube.inv",           "org.falmdev.libs.smartinvs")
        relocate("org.json.simple", "org.falmdev.libs.json")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx3G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}