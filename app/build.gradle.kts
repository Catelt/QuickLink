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

            // Generate native debug symbols for upload to Google Play
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

}

dependencies {
    implementation("com.google.zxing:core:${Dependencies.Versions.zxingCore}")
    implementation("androidx.datastore:datastore-preferences:${Dependencies.Versions.datastore}")
    implementation("com.google.code.gson:gson:${Dependencies.Versions.gson}")

    // Regular module dependencies
    implementation(project(Dependencies.Feature.COMPONENT))

    // CameraX
    implementation("androidx.camera:camera-camera2:${Dependencies.Versions.cameraCamera2}")
    implementation("androidx.camera:camera-lifecycle:${Dependencies.Versions.cameraCamera2}")
    implementation("androidx.camera:camera-view:${Dependencies.Versions.cameraCamera2}")

    // ML Kit for barcode scanning
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:${Dependencies.Versions.barcodeScanning}")
}