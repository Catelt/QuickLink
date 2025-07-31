plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.catelt.quicklink"
    compileSdk = Dependencies.compileSdk

    defaultConfig {
        applicationId = "com.catelt.quicklink"
        minSdk = Dependencies.minSdk
        targetSdk = Dependencies.targetSdk
        versionCode = 106
        versionName = "1.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:${Dependencies.Versions.coreKtx}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Dependencies.Versions.lifecycleRuntimeKtx}")
    implementation("androidx.activity:activity-compose:${Dependencies.Versions.activityCompose}")
    implementation(platform("androidx.compose:compose-bom:${Dependencies.Versions.composeBom}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:${Dependencies.Versions.junit}")
    androidTestImplementation("androidx.test.ext:junit:${Dependencies.Versions.junitVersion}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Dependencies.Versions.espressoCore}")
    androidTestImplementation(platform("androidx.compose:compose-bom:${Dependencies.Versions.composeBom}"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
} 