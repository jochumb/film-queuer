plugins {
    kotlin("jvm") version "2.2.10" apply false
    id("io.ktor.plugin") version "3.2.3" apply false
}

allprojects {
    group = "me.jochum.filmqueuer"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}