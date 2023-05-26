plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 33
    
    defaultConfig {
        minSdk = 16
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    flavorDimensions.add("mode")

    productFlavors {
        create("manual") {
            dimension = "mode"
            applicationId = "com.google.location.nearby.apps.walkietalkie.manual"
        }
        create("automatic") {
            dimension = "mode"
            applicationId = "com.google.location.nearby.apps.walkietalkie.automatic"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-nearby:18.5.0")

    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.multidex:multidex:2.0.1")
}

