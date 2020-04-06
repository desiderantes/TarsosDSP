group = "be.tarsos.dsp"
version = "2.5-SNAPSHOT"

buildscript {

    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.6.2")
        classpath("de.mannodermaus.gradle.plugins:android-junit5:1.6.0.0")
        classpath(kotlin("gradle-plugin", version = "1.3.71"))

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        jcenter()
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
    }
}