plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    application
    id("com.github.node-gradle.node") version "3.5.1"
}

application {
    mainClass.set("ServerKt")
}

node {
    version.set("20.15.1")
    download.set(true)
    nodeProjectDir.set(file("${project.projectDir}/frontend"))
}

tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildFrontend") {
    group = "frontend"
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "build"))
}

tasks.register<com.github.gradle.node.npm.task.NpmTask>("devFrontend") {
    group = "frontend"
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "dev"))
}

val generatedSvelteDir = "${project.layout.buildDirectory.get().asFile.absolutePath}/generated/resources/"

tasks.register<Copy>("copyFrontend") {
    group = "frontend"
    dependsOn("buildFrontend")
    from("${project.projectDir}/frontend/build")  // SvelteKit typically outputs to a 'build' directory
    into("$generatedSvelteDir/static")
}

tasks.register<Jar>("fatJar") {
    group = "build"
    archiveClassifier.set("fat")

    // Include the main class in manifest
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    // Include all runtime dependencies
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    // Include the frontend build if it exists
    dependsOn("copyFrontend")

    // Exclude META-INF signatures from dependencies to avoid conflicts
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("processResources") {
    dependsOn("copyFrontend")
}

sourceSets {
    main {
        resources {
            srcDir(generatedSvelteDir)
        }
    }

    kotlin {
        dependencies {
            implementation(project.dependencies.enforcedPlatform("io.ktor:ktor-bom:2.3.12"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

            implementation("io.ktor:ktor-server-content-negotiation")
            implementation("io.ktor:ktor-server-cors")
            implementation("io.ktor:ktor-server-netty")
            implementation("io.ktor:ktor-server-html-builder")
            implementation("io.ktor:ktor-server-websockets")

            implementation("io.ktor:ktor-serialization")
            implementation("io.ktor:ktor-serialization-kotlinx-json")

            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

            implementation("ch.qos.logback:logback-classic:1.5.15")
            implementation("ch.qos.logback:logback-core:1.5.15")

        }
    }
}

repositories {
    mavenCentral()
}

tasks.register<Exec>("startCloudflaredTunnel") {
    group = "Cloudflare Tunnel"
    description = "Starts the Cloudflared tunnel"

    commandLine("cloudflared", "tunnel", "run", "sheetshare")
}