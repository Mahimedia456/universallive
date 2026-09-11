val universalLiveApiBaseUrl = providers.gradleProperty("UL_API_BASE_URL")
    .orElse("https://universallive.vercel.app/api/v1")
    .get()

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.universallive.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.universallive.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 39
        versionName = "0.40.0"
        buildConfigField("String", "UNIVERSALLIVE_API_BASE_URL", "\"$universalLiveApiBaseUrl\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}


dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation("com.github.pedroSG94.RootEncoder:library:2.8.1")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation(compose.runtime)
    implementation(compose.ui)
    debugImplementation(compose.uiTooling)
}
