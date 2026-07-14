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
    // Netty NIO 客户端
    implementation("io.netty:netty-all:4.1.112.Final")
    // JSON
    implementation("com.google.code.gson:gson:2.11.0")
    // UI 动画 (Trident)
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

    // ══════════════════════════════════════
    //  Fat JAR：插件 class + 所有依赖打入一个 JAR
    //  产物: build/libs/plugins-1.0-SNAPSHOT-all.jar
    //  用法: 放入 IDEA 插件目录 (<idea>/plugins/LanPartner/lib/) 即可
    // ══════════════════════════════════════
    register<Jar>("fatJar") {
        group = "build"
        description = "打包 Fat JAR（包含所有依赖）"
        archiveBaseName.set("LanPartner")
        archiveVersion.set("${project.version}")
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        // 插件自身编译产物
        from(sourceSets.main.get().output)

        // 合并所有运行时依赖（排除 IntelliJ Platform 自带的，避免冲突）
        from({
            configurations.runtimeClasspath.get()
                .filter { it.exists() && it.name.endsWith(".jar") }
                .map { zipTree(it) }
        })

        // 排除数字签名文件（多个 JAR 合并时会冲突）
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/*.LIST")
        exclude("module-info.class")

        // 保留 plugin.xml（确保不被覆盖）
        manifest {
            attributes(
                "Implementation-Title" to "局域网伙伴 (LanPartner)",
                "Implementation-Version" to project.version,
                "Built-By" to "Gradle"
            )
        }
    }
}
