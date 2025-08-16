plugins {
    id("android-library")
}
android {
    namespace = "com.catelt.downloadfile"
}

dependencies {
    implementation(project(Dependencies.Feature.COMPONENT))

    implementation("com.squareup.okhttp3:okhttp:${Dependencies.Versions.okhttp}")
    implementation("androidx.work:work-runtime-ktx:${Dependencies.Versions.workRuntimeKtx}")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${Dependencies.Versions.lifecycleRuntimeKtx}")
}