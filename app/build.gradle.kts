plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.1.20-Beta1"
}

android {
    namespace = "com.example.enginerpm"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.enginerpm"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //implementation("org.jetbrains.kotlin.android:org.jetbrains.kotlin.android.gradle.plugin:2.1.20-Beta1")
    implementation("com.github.wendykierp:JTransforms:3.1")
}