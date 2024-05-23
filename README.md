# 无名杀由理兼容版
todo: 添加文件管理器功能
安装apk后，apk固定以chrome 119内核启动Webview组件

## 环境要求
Android 7.0或以上

## 感谢
本项目中的Webview升级操作由[WebViewUpgrade](https://github.com/JonaNorman/WebViewUpgrade)提供

## 创建安卓项目
先按教程全局安装cordova环境(本项目用的是cordova12)

创建安卓项目: 
```
cordova platform add android
```

在platforms\android\app\build.gradle的dependencies块中添加:
```gradle
implementation fileTree(dir: 'src/main/libs', include: '*.jar')
implementation 'io.github.jonanorman.android.webviewup:core:0.1.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'com.alibaba:fastjson:1.1.55.android'
implementation 'androidx.palette:palette-ktx:1.0.0'
```
在platforms\android\app\build.gradle的android块中添加:
```gradle
android.applicationVariants.all {
    variant ->
        variant.outputs.all {
            if (buildType.name == 'release') {
                outputFileName = "无名杀由理兼容版v${variant.versionName}(${generateTime()}).ApK"
            }
        }
}
aaptOptions {
    // 表示不让aapt压缩的文件后缀
    noCompress "apk"
}
```
在这个块(android块)上面添加:
```gradle
def generateTime() {
    return new Date().format("yyyy-MM-dd")
}
android { ... }
```

在platforms\android\app\src\main\res\main\res\values\strings.xml添加:
```xml
<string name="app_import_title">无名杀由理兼容版</string>
<string name="app_import_label">无名杀导入(由理兼容版)</string>
```

然后打开Android Studio进行安卓开发