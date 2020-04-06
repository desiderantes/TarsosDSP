import de.mannodermaus.gradle.plugins.junit5.junitPlatform
import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.6.2")
        classpath("de.mannodermaus.gradle.plugins:android-junit5:1.6.0.0")
    }
}

plugins {
    id("com.android.library")
    id("de.mannodermaus.android-junit5")
    kotlin("android")
}
android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")

    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArgument(
            "runnerBuilder",
            "de.mannodermaus.junit5.AndroidJUnit5Builder"
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packagingOptions {
        exclude("META-INF/LICENSE*")
    }

    testOptions {
        junitPlatform {
            // Configure JUnit 5 tests here
            filters("debug") {
                excludeTags("slow")
            }

            // Using local dependency instead of Maven coordinates
            instrumentationTests.integrityCheckEnabled = false
        }
    }
}

tasks.withType<Test> {
    testLogging.events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
}

dependencies {
    val commonFixtures = testFixtures(project(":common"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("com.arthenica:mobile-ffmpeg-full:4.3.1")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation(kotlin("stdlib-jdk8"))
    api(project(":common"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.1")
    testImplementation(commonFixtures)
    androidTestImplementation("androidx.test:runner:1.2.0")
    androidTestImplementation("de.mannodermaus.junit5:android-test-core:1.2.0")
    androidTestImplementation(commonFixtures)
    androidTestRuntimeOnly("de.mannodermaus.junit5:android-test-runner:1.2.0")
}

