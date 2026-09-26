plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    // 1.3.4 is built against Kotlin 1.9.24, matching this project; 1.3.5 needs Kotlin 2.
    id("app.cash.paparazzi") version "1.3.4" apply false
}
