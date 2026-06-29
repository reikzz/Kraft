plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "org.kraft"
version = "0.3.0"

application {
    mainClass.set("org.kraft.client.DesktopLauncherKt")
    applicationName = "Kraft"
}

distributions {
    main {
        contents {
            from("assets") {
                into("assets")
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    val gdxVersion = "1.13.1"

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
}