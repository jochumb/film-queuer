plugins {
    kotlin("jvm") version "2.4.10" apply false
    id("io.ktor.plugin") version "3.5.2" apply false
}

allprojects {
    group = "me.jochum.filmqueuer"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}