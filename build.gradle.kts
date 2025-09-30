plugins {
    id("com.android.application") version "7.4.2"
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
    implementation("androidx.preference:preference:1.2.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.work:work-runtime:2.8.1")
// https://mvnrepository.com/artifact/com.klinkerapps/android-smsmms
    implementation("com.klinkerapps:android-smsmms:5.2.6")
    // Network libraries
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // JSON parsing
    implementation("org.json:json:20210307")

    // Add Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}