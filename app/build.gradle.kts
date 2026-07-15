plugins {
    alias(libs.plugins.idea.ext)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
}

idea {
    module {
        excludeDirs.add(file("src/release/generated/baselineProfiles"))
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

android {
    namespace = "com.flashsphere.rainwaveplayer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.flashsphere.rainwaveplayer"
        multiDexEnabled = true
        minSdk = 23
        targetSdk = 37
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CAST_APP_ID", "\"4AD422FD\"")
        buildConfigField("String", "TEST_CREDENTIALS", "\"\"")
        buildConfigField("String", "OKHTTP_VERSION", "\"${libs.versions.okhttp3.get()}\"")
        buildConfigField("boolean", "PIXELIZE_IMAGE", "false")
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        resources {
            excludes += listOf("META-INF/LICENSE", "META-INF/NOTICE")
        }
        dex {
            useLegacyPackaging = true
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    signingConfigs {
        create("release") {
            storeFile = findProperty("RELEASE_STORE_FILE")?.let { file(it as String) }
            storePassword = findProperty("RELEASE_STORE_PASSWORD") as String?
            keyAlias = findProperty("RELEASE_KEY_ALIAS") as String?
            keyPassword = findProperty("RELEASE_KEY_PASSWORD") as String?

            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-SNAPSHOT"
            isDebuggable = true
            isPseudoLocalesEnabled = true

            buildConfigField("String", "TEST_CREDENTIALS", "\"${project.findProperty("RAINWAVE_TEST_CREDENTIALS") ?: ""}\"")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            buildConfigField("String", "CAST_APP_ID", "\"2E4E683E\"")

            signingConfig = signingConfigs["release"]

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard/proguard-rules.pro")
        }
    }
    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
    }
    lint {
        informational.addAll(setOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "IconDuplicates",
            "AndroidGradlePluginVersion",
        ))
        warningsAsErrors = true
        abortOnError = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    androidResources {
        localeFilters += listOf("en", "fr")
    }
}

hilt {
    enableAggregatingTask = true
}

baselineProfile {
    dexLayoutOptimization = true
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.addAll(
        rootProject.layout.projectDirectory.file("stability_config.conf")
    )
}

dependencies {
    baselineProfile(project(":baselineprofile"))

    implementation(fileTree("libs") { include("*.jar") })
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.i18n)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.media3)
    implementation(libs.androidx.media3.okhttp)
    implementation(libs.androidx.media)
    implementation(libs.androidx.mediarouter)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.lifecycle.vm)
    implementation(libs.androidx.lifecycle.vm.navigation3)
    implementation(libs.androidx.lifecycle.vm.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.nav.compose) {
        exclude(group = "androidx.navigation")
    }
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.datastore.pref)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.androidx.compose.material3.window.size)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.reorderable)

    implementation(libs.androidx.tv.material)

    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)

    implementation(libs.google.cast)
    implementation(libs.google.cast.framework)
    implementation(libs.google.cast.tv)

    implementation(libs.retrofit2)
    implementation(libs.retrofit2.kotlinx.serialization)

    implementation(libs.kotlinx.serialization)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging)
    implementation(libs.okhttp3.tls)

    implementation(platform(libs.coil.bom))
    implementation(libs.coil.svg)
    implementation(libs.coil.okhttp)
    implementation(libs.coil.cachecontrol)
    implementation(libs.coil.compose)

    implementation(libs.timber)
    implementation(libs.process.phoenix)

//    debugImplementation(libs.leakcanary)

    coreLibraryDesugaring(libs.desugarJdkLibs)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(libs.androidx.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.assertk)
    androidTestImplementation(libs.dagger.hilt.testing)
    kspAndroidTest(libs.dagger.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertk)
    testImplementation(libs.jsonassert)
    testImplementation(libs.dagger.hilt.testing)
    kspTest(libs.dagger.hilt.compiler)
}

tasks.named("lint") {
    dependsOn(
        "lintDebug",
        "lintRelease",
    )
}
