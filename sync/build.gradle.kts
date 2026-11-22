plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pinfold.app.sync"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.activity.ktx)
}
