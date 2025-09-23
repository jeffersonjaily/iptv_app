plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // COMENTE OU REMOVA ESTA LINHA:
    // id("com.google.devtools.ksp") // Aplica o plugin KSP
}

android {
    namespace = "com.seuprojeto"
    compileSdk = 34 // Android 14 (API 34)

    defaultConfig {
        applicationId = "com.seuprojeto"
        minSdk = 21 // Android 5.0 Lollipop
        targetSdk = 34 // Android 14 (API 34)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Dependências de bibliotecas locais (se houver arquivos .jar na pasta 'libs')
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")

    // Rede e JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ExoPlayer
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")

    // Glide (agora usando annotationProcessor)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // MUDEI AQUI: de ksp para annotationProcessor
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") 

    // Testes
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}