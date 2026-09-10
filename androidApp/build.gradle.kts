plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.universallive.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.universallive.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 30
        versionName = "0.30.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}


dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation("com.github.pedroSG94.RootEncoder:library:2.8.1")
    implementation(compose.runtime)
    implementation(compose.ui)
    debugImplementation(compose.uiTooling)
}
