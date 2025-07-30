import java.io.FileInputStream
import java.util.Properties

plugins {
    id("android-application")
}

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    dynamicFeatures += setOf(":scanqr", ":deeplink", ":qrcode")
}

dependencies {
    implementation("com.google.zxing:core:${Dependencies.Versions.zxingCore}")
    implementation("androidx.datastore:datastore-preferences:${Dependencies.Versions.datastore}")
    implementation("com.google.code.gson:gson:${Dependencies.Versions.gson}")
    
    implementation("com.squareup.okhttp3:okhttp:${Dependencies.Versions.okhttp}")
    implementation("androidx.work:work-runtime-ktx:${Dependencies.Versions.workRuntimeKtx}")
    
    // Dynamic feature modules
    implementation("com.google.android.play:feature-delivery:${Dependencies.Versions.featureDelivery}")
    implementation(kotlin("reflect"))
}