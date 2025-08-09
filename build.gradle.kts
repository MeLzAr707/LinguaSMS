plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.translator.messagingapp"
    compileSdk = 34  // Downgraded from 35 to 34 for better compatibility

    // Add this line to enable BuildConfig
    buildFeatures {
        buildConfig = true
    }
    
    // Add D8 optimization settings to fix NullPointerException issues
    dexOptions {
        javaMaxHeapSize = "4g"
        preDexLibraries = true
    }
    
    // Add D8 optimization settings to fix NullPointerException issues
    dexOptions {
        javaMaxHeapSize = "4g"
        preDexLibraries = true
    }

    defaultConfig {
        applicationId = "com.translator.messagingapp"
        minSdk = 24
        targetSdk = 34  // Downgraded from 35 to 34 for better compatibility
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["ENABLE_DEBUG_TOOLS"] = "false"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "ENABLE_DEBUG_TOOLS", "true")
            manifestPlaceholders["ENABLE_DEBUG_TOOLS"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "ENABLE_DEBUG_TOOLS", "false")
            manifestPlaceholders["ENABLE_DEBUG_TOOLS"] = "false"
        }
    }
    
    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }

    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.preference)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.appcompat.v141)
    implementation(libs.material.v150)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.google.playServices.base)
    implementation(libs.circleimageview)
    implementation(libs.androidx.work.runtime)

    // Network libraries
    implementation(libs.okhttp)

    // JSON parsing
    implementation(libs.json)

    // Add Glide for image loading
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.v113)
    androidTestImplementation(libs.espresso.core.v340)
}