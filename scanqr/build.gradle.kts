plugins {
    id("android-dynamic-base")
}
android {
    namespace = "com.catelt.scanqr"
}

dependencies {
    // CameraX
    implementation("androidx.camera:camera-camera2:${Dependencies.Versions.cameraCamera2}")
    implementation("androidx.camera:camera-lifecycle:${Dependencies.Versions.cameraCamera2}")
    implementation("androidx.camera:camera-view:${Dependencies.Versions.cameraCamera2}")

    // ML Kit for barcode scanning
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:${Dependencies.Versions.barcodeScanning}")

    // Main app dependency to access interfaces and viewmodels
    implementation(project(":app"))
}