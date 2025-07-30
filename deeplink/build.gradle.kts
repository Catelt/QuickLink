plugins {
    id("android-dynamic-base")
}
android {
    namespace = "com.catelt.deeplink"
}

dependencies {
    // Main app dependency to access interfaces and viewmodels
    implementation(project(":app"))
    
    // Compose dependencies using BOM (same as main app)
    implementation(platform("androidx.compose:compose-bom:${Dependencies.Versions.composeBom}"))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
} 