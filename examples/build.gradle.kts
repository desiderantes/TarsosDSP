plugins {
    java
    kotlin("jvm")
    application
}

group = "be.tarsos.dsp"
version = "2.5-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":jvm"))
    testImplementation(testFixtures(project(":common")))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.1")
}



application {
    mainClassName = "be.tarsos.dsp.example.Main"
}



tasks.named<Test>("test") {
    useJUnitPlatform()
}



configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}