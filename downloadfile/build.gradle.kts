plugins {
    id("android-dynamic-base")
}
android {
    namespace = "com.catelt.downloadfile"
}

dependencies {
    // Main app dependency to access interfaces and viewmodels
    implementation(project(":app"))

    implementation("com.squareup.okhttp3:okhttp:${Dependencies.Versions.okhttp}")
    implementation("androidx.work:work-runtime-ktx:${Dependencies.Versions.workRuntimeKtx}")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${Dependencies.Versions.lifecycleRuntimeKtx}")
}