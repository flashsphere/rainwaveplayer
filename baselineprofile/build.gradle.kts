plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.flashsphere.baselineprofile"
    compileSdk = 36

    compileOptions {
        sourceCompatibility(21)
        targetCompatibility(21)
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    defaultConfig {
        minSdk = 28
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TEST_CREDENTIALS", "\"${project.properties["RAINWAVE_TEST_CREDENTIALS"]}\"")
    }
    buildFeatures {
        buildConfig = true
    }
    targetProjectPath = ":app"
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.espresso)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.junit4)
}

androidComponents {
    onVariants(selector().all()) { v ->
        val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
        v.instrumentationRunnerArguments.put(
            "targetAppId",
            v.testedApks.map { artifactsLoader.load(it)!!.applicationId }
        )
    }
}
