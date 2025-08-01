import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.detekt)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.robolectric.android.project)
  alias(libs.plugins.robolectric.spotless)
}

android {
  compileSdk = 35
  namespace = "org.robolectric.sparsearray"

  defaultConfig { minSdk = 21 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  testOptions {
    targetSdk = 35
    unitTests.isIncludeAndroidResources = true
  }

  androidComponents {
    beforeVariants(selector().all()) { variantBuilder ->
      // sparsearray does not support AndroidTest.
      variantBuilder.enableAndroidTest = false
    }
  }
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_1_8 } }

dependencies {
  compileOnly(AndroidSdk.MAX_SDK.coordinates)

  testCompileOnly(AndroidSdk.MAX_SDK.coordinates)
  testRuntimeOnly(AndroidSdk.MAX_SDK.coordinates)
  testImplementation(project(":robolectric"))
  testImplementation(libs.junit4)
  testImplementation(libs.truth)
  testImplementation(libs.kotlin.stdlib)
}
