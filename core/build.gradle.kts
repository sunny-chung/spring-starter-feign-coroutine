import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("maven-publish")
    kotlin("kapt")
}

group = "com.github.sunny-chung"
version = "0.3.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

repositories {
    mavenCentral()
}

val feignCoreVersion = "13.2.1"

dependencies {
    implementation("io.github.openfeign:feign-kotlin:$feignCoreVersion")
    implementation("io.github.openfeign:feign-slf4j:$feignCoreVersion")
    implementation("org.springframework.cloud:spring-cloud-openfeign-core:4.0.4")
    implementation("org.springframework:spring-webflux:6.0.11")
    implementation("io.projectreactor.netty:reactor-netty-http:1.1.10")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

    implementation("org.springframework.boot:spring-boot-starter-actuator:3.2.2")
    implementation("io.github.openfeign:feign-micrometer:$feignCoreVersion")
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "spring-starter-feign-coroutine"

            afterEvaluate {
                from(components["kotlin"])
            }
        }
    }
}
