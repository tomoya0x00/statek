package dependencies

object Dep {

    private const val kotlin = "1.3.72"

    object GradlePlugin {
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Dep.kotlin}"
        const val android = "com.android.tools.build:gradle:3.6.1"
        const val bintray = "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5"
    }

    object Kotlin {
        const val common = "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlin"
        const val jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin"
        const val js = "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlin"
    }

    object Test {
        const val common = "org.jetbrains.kotlin:kotlin-test:$kotlin"
        const val annotation = "org.jetbrains.kotlin:kotlin-test-annotations-common:$kotlin"
        const val jvm = "org.jetbrains.kotlin:kotlin-test-junit:$kotlin"
        const val js = "org.jetbrains.kotlin:kotlin-test-js:$kotlin"
    }

}
