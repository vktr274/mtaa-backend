import org.jetbrains.kotlin.contracts.model.structure.UNKNOWN_COMPUTATION.type
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.4.10"
}

group = "com.mtaa.project"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    //ktor
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    //ORM exposed
    implementation("org.jetbrains.exposed:exposed-core:0.29.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.29.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.29.1")
    implementation("org.postgresql:postgresql:42.2.19.jre7")
    implementation("org.jetbrains.exposed:exposed-jodatime:0.29.1")

    //json
    implementation("io.ktor:ktor-gson:$ktor_version")

    //https
    implementation("io.ktor:ktor-network-tls-certificates:$ktor_version")

    //jwt
    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-gson:0.11.2")

    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

tasks.create("stage") {
    dependsOn("installDist")
}

//This is needed to load SSL certificate before app start
task("generateJks", JavaExec::class) {
    dependsOn("classes")
    main = "io.ktor.samples.http2.CertificateGenerator"
    classpath = sourceSets["main"].runtimeClasspath
}
getTasksByName("run", false).first().dependsOn("generateJks")
