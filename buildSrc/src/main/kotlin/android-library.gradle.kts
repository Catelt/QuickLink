import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = Dependencies.compileSdk

    defaultConfig {
        minSdk = Dependencies.minSdk
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        compose = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
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
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:${Dependencies.Versions.junit}")
    androidTestImplementation("androidx.test.ext:junit:${Dependencies.Versions.junitVersion}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Dependencies.Versions.espressoCore}")
    androidTestImplementation(platform("androidx.compose:compose-bom:${Dependencies.Versions.composeBom}"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
