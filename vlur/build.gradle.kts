import com.android.build.gradle.internal.tasks.UnstrippedLibs
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.nmcp)
}

android {
    namespace = "com.sadvakassov.vlur"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
            buildArgumentList {
                UnstrippedLibs.add("-DANDROID_TOOLCHAIN=clang")
                UnstrippedLibs.add("-DANDROID_STL=c++_static")
            }
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeExtenstion.get()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.android)
    implementation(libs.timber)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

nmcp {
    publishAllPublications {
        val keyUsername = "SONATYPE_USERNAME"
        val keyPassword = "SONATYPE_PASSWORD"
        username = (findProperty(keyUsername) as? String) ?: System.getenv(keyUsername)
        password = (findProperty(keyPassword) as? String) ?: System.getenv(keyPassword)
        publicationType = "USER_MANAGED"
    }
}
