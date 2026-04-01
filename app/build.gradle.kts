plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.folklore25.ghosthand"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.folklore25.ghosthand"
        minSdk = 30
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(providers.gradleProperty("GHOSTHAND_RELEASE_STORE_FILE").get())
            storePassword = providers.gradleProperty("GHOSTHAND_RELEASE_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("GHOSTHAND_RELEASE_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("GHOSTHAND_RELEASE_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
