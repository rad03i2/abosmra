plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.radwan.abosmra"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.radwan.abosmra"
        minSdk = 26
        targetSdk = 37
        versionCode = 14
        versionName = "2.3.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationInstallation {
        enableBaselineProfile = true
    }

    buildTypes {
        release {
            // Testing release: optimized like production, signed with the debug key so it
            // can be installed directly on the phone. Replace before store publishing.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")

    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    val roomVersion = "3.0.3"
    implementation("androidx.room3:room3-runtime:$roomVersion")
    implementation("androidx.sqlite:sqlite-framework:2.7.1")
    ksp("androidx.room3:room3-compiler:$roomVersion")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
