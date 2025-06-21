
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")

}

android {
    namespace = "android.bignerdranch.reportsapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "android.bignerdranch.reportsapp"
        minSdk = 30
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)


    //Yandex MapKit
    implementation (libs.maps.mobile)

    //Compose
    implementation(libs.coil.compose) // Для Compose
    implementation (libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.coil.video) // Если нужна поддержка видео

    // Firebase Authentication (основная)
    implementation(libs.firebase.auth.ktx)

    // Firebase BoM (для управления версиями)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // Корутины для асинхронных операций
    implementation(libs.kotlinx.coroutines.android)

    // Для await() в Firebase операциях
    implementation(libs.kotlinx.coroutines.play.services)

    // Firestore
    implementation(libs.firebase.firestore.ktx) // Для Kotlin

    // SharedPreferences
    implementation(libs.androidx.preference.ktx)

    // Для viewModelScope в ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // AWS S3 SDK (совместим с VK Cloud)
    implementation(libs.aws.android.sdk.s3)
    implementation(libs.aws.android.sdk.core)

    // Для работы с файлами и MIME-типами
    implementation(libs.commons.io)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}