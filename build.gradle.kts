plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "org.kraft"
version = "0.1.0-SNAPSHOT"

application {
    mainClass.set("org.kraft.client.DesktopLauncherKt")
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