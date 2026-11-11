plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "pl.tools.intellij"
version = "1.2.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Build against the oldest supported platform to protect binary compatibility.
        // The plugin uses only platform/editor APIs,
        // so it does not require the Java, SQL or Groovy plugins.
        intellijIdea("2024.2.6")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
