plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("kapt")

}

android {
    namespace = "com.karwa.mdtnavigation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.karwa.mdtnavigation"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "MDT_NAV-V-${variant.versionName}-${variant.versionCode}-${variant.name}.apk"
                println("OutputFileName: $outputFileName")
                output.outputFileName = outputFileName
            }
    }
    dataBinding {
        this.enable = true
    }
    compileOptions {
        sourceCompatibility  = JavaVersion.VERSION_17
        targetCompatibility  = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
//    buildFeatures {
//        compose = true
//    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
//    implementation("androidx.activity:activity-compose:1.8.1")
//    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
//    implementation("androidx.compose.ui:ui")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
//    implementation("androidx.compose.ui:ui-graphics")
//    implementation("androidx.compose.ui:ui-tooling-preview")
//    implementation("androidx.compose.material3:material3")
    // Mapbox Maps SDK
    implementation("com.mapbox.maps:android:10.15.0")

    // Mapbox Navigation SDK
    implementation("com.mapbox.navigation:android:2.18.0")

    // Map Matching functionality
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-services:6.3.0")
//    implementation("com.mapbox.common:mapbox-common:10.15.0")

    // Turf for polyline decoding and geometry calculations (optional)
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:6.3.0")

//    implementation("com.mapbox.navigation:android:2.17.12")
//    implementation("com.mapbox.maps:android:11.6.0")
//    implementation("com.mapbox.maps:android:11.6.0")

//    implementation("com.mapbox.mapboxsdk:mapbox-android-sdk-versions:1.1.3")
//    implementation("com.google.maps.android:android-maps-utils:0.5")
    implementation("com.google.android.gms:play-services-maps:18.0.2")
    implementation("com.google.android.gms:play-services-location:20.0.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.2")
    implementation("androidx.cardview:cardview:1.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
//    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
//    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
//    debugImplementation("androidx.compose.ui:ui-tooling")
//    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("com.google.maps.android:android-maps-utils:2.2.5")

    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("com.facebook.shimmer:shimmer:0.5.0")

}