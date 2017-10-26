## プロジェクトのbuild.gradleについて
新たにgoogle play servicesのnearbyを使用するために、アプリケーションレベルのbuild.gradleに
google play servicesのAPIをインポートしました。
しかし、プロジェクトレベルのほうのbuild.gradleにも記述を加えないと、正常にgradleを実行することができません。
そのため、プロジェクトレベルのほうのbuild.gradleに

allprojects {
    repositories {
        jcenter()
        maven {
            url "https://maven.google.com"
        }
    }
}

を付け加えてください。

### 例
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath "com.android.tools.build:gradle:2.3.3"
    }
}

allprojects {
    repositories {
        jcenter()
        maven {
            url "https://maven.google.com"
        }
    }
}