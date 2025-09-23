<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
android:layout_width="match_parent"
android:layout_height="match_parent"
android:background="@android:color/black">

<com.google.android.exoplayer2.ui.StyledPlayerView
android:id="@+id/playerView"
android:layout_width="match_parent"
android:layout_height="match_parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
        dependencies {
            implementation("androidx.core:core-ktx:1.9.0")
            implementation("androidx.appcompat:appcompat:1.6.1")
            implementation("androidx.constraintlayout:constraintlayout:2.1.4")
            implementation("androidx.leanback:leanback:1.2.0-beta01")

            // ExoPlayer - Player de mídia recomendado pelo Google
            implementation("com.google.android.exoplayer:exoplayer-core:2.18.7")
            implementation("com.google.android.exoplayer:exoplayer-ui:2.18.7")
            implementation("com.google.android.exoplayer:exoplayer-hls:2.18.7")

            // OkHttp - Para carregar a lista M3U
            implementation("com.squareup.okhttp3:okhttp:4.12.0")

            // Kotlin Coroutines - Para tarefas assíncronas
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") 
        }        