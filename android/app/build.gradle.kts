plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization")
}

android {
  namespace = "com.solvix.tabungan"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.solvix.tabungan"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.13"
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
}


dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.activity:activity-compose:1.8.2")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-text")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.animation:animation")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.biometric:biometric:1.1.0")
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation("io.github.jan-tennert.supabase:supabase-kt:2.4.1")
  implementation("io.github.jan-tennert.supabase:postgrest-kt:2.4.1")
  implementation("io.ktor:ktor-client-okhttp:2.3.12")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
