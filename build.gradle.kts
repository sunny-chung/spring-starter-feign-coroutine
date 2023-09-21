plugins {
    val kotlinVersion = "1.8.22"

    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("kapt") version kotlinVersion apply false
}
