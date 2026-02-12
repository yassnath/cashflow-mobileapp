plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
}

android {
  namespace = "com.solvix.tabungan"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.solvix.tabungan"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures {
    compose = true
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    isCoreLibraryDesugaringEnabled = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  lint {
    // Supabase/Ktor major upgrades require coordinated API migration.
    disable += "NewerVersionAvailable"
  }
}


dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.core:core-ktx:1.17.0")
  implementation("androidx.activity:activity-compose:1.12.4")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-text")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.animation:animation")
  implementation("com.google.android.material:material:1.13.0")
  implementation("androidx.biometric:biometric:1.1.0")
  implementation("androidx.work:work-runtime-ktx:2.11.1")
  implementation("io.github.jan-tennert.supabase:supabase-kt:2.4.1")
  implementation("io.github.jan-tennert.supabase:postgrest-kt:2.4.1")
  implementation("io.ktor:ktor-client-okhttp:2.3.12")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
  testImplementation("junit:junit:4.13.2")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
