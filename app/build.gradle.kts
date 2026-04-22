plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.photosclone"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.photosclone"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.ui.test)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Thư viện load ảnh cực mượt cho Kotlin
    implementation("io.coil-kt:coil:2.5.0")
    // Coroutines để chạy ngầm việc quét file USB
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Thư viện DocumentFile để xử lý Storage Access Framework
    implementation("androidx.documentfile:documentfile:1.0.1")
    // Thư viện hỗ trợ phóng to ảnh bằng cử chỉ ngón tay
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
}