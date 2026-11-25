plugins {
    java
}

group = "ru.javaroot"
version = "2.0.0"

val targetJava = 21

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJava))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(targetJava)
    }

    compileTestJava {
        options.encoding = "UTF-8"
        options.release.set(targetJava)
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version.toString())
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveBaseName.set("JavaRestart")
        archiveClassifier.set("")
    }

    test {
        useJUnitPlatform()
    }
}
