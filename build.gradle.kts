buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
    }
}
plugins {
    id("com.android.application") version "7.3.0" apply false
    // ...
    id("com.android.library") version "7.4.2" apply false

    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.1" apply false
}