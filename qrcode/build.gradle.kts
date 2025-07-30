plugins {
    id("android-dynamic-base")
}
android {
    namespace = "com.catelt.qrcode"
}

dependencies {
    // Main app dependency to access interfaces and viewmodels
    implementation(project(":app"))
} 