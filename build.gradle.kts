plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.ide.plugin"
version = "1.0-SNAPSHOT"

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public/") }
    maven { url = uri("https://maven.aliyun.com/repository/central/") }
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    mavenCentral()
}

dependencies {
    // Netty NIO Client
    implementation("io.netty:netty-all:4.1.112.Final")
    // JSON Parser
    implementation("com.google.code.gson:gson:2.11.0")
    // UI Animation (Trident)
    implementation("org.pushing-pixels:radiance-animation:8.5.0")
}

// Configure Gradle IntelliJ Plugin
intellij {
    version.set("2023.2.5")
    type.set("IC")

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // ======================================
    // Fat‑Jar: package plugin classes and all dependencies into one jar file
    // Output: build/libs/plugins-1.0-SNAPSHOT-all.jar
    // Usage: put jar under <idea>/plugins/LanPartner/lib/ directory
    // ======================================
    register<Jar>("fatJar") {
        group = "build"
        description = "Build Fat‑Jar with all included dependencies"
        archiveBaseName.set("LanPartner")
        archiveVersion.set("${project.version}")
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        // Compiled project classes
        from(sourceSets.main.get().output)

        // Merge runtime dependencies, exclude IntelliJ built‑in libraries to avoid conflicts
        from({
            configurations.runtimeClasspath.get()
                .filter { it.exists() && it.name.endsWith(".jar") }
                .map { zipTree(it) }
        })

        // Exclude signature files which cause conflicts after merging multiple jars
        exclude("META‑INF/*.SF")
        exclude("META‑INF/*.DSA")
        exclude("META‑INF/*.RSA")
        exclude("META‑INF/*.LIST")
        exclude("module-info.class")

        manifest {
            attributes(
                "Implementation-Title" to "LanPartner",
                "Implementation-Version" to project.version,
                "Built-By" to "Gradle"
            )
        }
    }
}