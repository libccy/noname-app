# 无名杀由理版
todo: 添加文件管理器功能

将公共的Api和升级Webview内核操作封装到[NoameCore模块](https://github.com/libnoname/noname-android-core)中，使所有App可以共用相同功能

## 环境要求
Android 7.0或以上

## 感谢
本项目中的Webview升级操作由[WebViewUpgrade](https://github.com/JonaNorman/WebViewUpgrade)提供

## 克隆本项目
git clone --recursive -b neo-yuri https://github.com/libccy/noname-app/

## 创建安卓项目
先按教程全局安装cordova环境(本项目用的是cordova12)

然后安装项目依赖

```
npm i cordova@12 -g
npm i
```

创建安卓项目: 
```
cordova platform add android@13
```

在platforms\android\settings.gradle中加入以下代码
```
include ":NonameCore"
```

platforms\android\app\build.gradle的android上面添加:
```gradle
def generateTime() {
    return new Date().format("yyyy-MM-dd")
}
android { ... }
```

在platforms\android\app\build.gradle的android块中添加:
```gradle
android.applicationVariants.all {
    variant ->
        variant.outputs.all {
            if (buildType.name == 'release') {
                outputFileName = "无名杀由理版v${variant.versionName}(${generateTime()}).ApK"
            }
        }
}

aaptOptions {
    // 表示不让aapt压缩的文件后缀
    noCompress "apk"
}
```

在platforms\android\app\build.gradle的dependencies块的SUB-PROJECT DEPENDENCIES END注释后加入:
```gradle
dependencies {
    ...
    // SUB-PROJECT DEPENDENCIES END

    // 要添加的如下
    implementation fileTree(dir: 'src/main/libs', include: '*.jar')
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.alibaba:fastjson:1.1.55.android'
    implementation 'androidx.palette:palette-ktx:1.0.0'
    implementation(project(path: ":NonameCore"))
}
```

由于不太了解Cordova的版本号设置，如果需要，在platforms\android\app\src\main\AndroidManifest.xml中修改指定的版本号(versionCode)

然后打开Android Studio进行安卓开发

~~由理版或由理兼容版使用`MT管理器`的签名进行分发~~

~~其中由理版的签名状态是v1+v2，由理兼容版的签名状态是v1+v2+v3~~

为了防止倒卖，在[NoameCore模块](https://github.com/libnoname/noname-android-core)中加入了签名验证功能，并且不再使用`MT管理器`的签名进行分发