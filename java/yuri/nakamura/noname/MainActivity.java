/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package yuri.nakamura.noname;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.noname.api.NonameJavaScriptInterface;
import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.source.UpgradeAssetSource;
import com.norman.webviewup.lib.source.UpgradeSource;
import com.norman.webviewup.lib.util.ProcessUtils;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class MainActivity extends CordovaActivity {

    private static boolean webViewInited = false;

    private ProgressDialog WebViewUpgradeProgressDialog;

    private void ActivityOnCreate(Bundle extras) {
        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog.dismiss();
            WebViewUpgradeProgressDialog = null;
        }

        try {
            if (extras != null && extras.getString("importExtensionName") != null) {
                String extName = extras.getString("importExtensionName");
                URI uri = new URI(launchUrl);
                String newQuery = uri.getQuery();
                String appendQuery = "importExtensionName=" + extName;
                if (newQuery == null) {
                    newQuery = appendQuery;
                } else {
                    newQuery += "&" + appendQuery;
                }
                URI newUri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
                Log.e(TAG, newUri.toString());
                loadUrl(newUri.toString());
            }
            else {
                loadUrl(launchUrl);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            // Set by <content src="index.html" /> in config.xml
            loadUrl(launchUrl);
        }

        View view = appView.getView();
        SystemWebView webview = (SystemWebView) view;
        WebSettings settings = webview.getSettings();
        Log.e(TAG, settings.getUserAgentString());
        initWebviewSettings(webview, settings);
    }

    private void initWebviewSettings(SystemWebView webview, WebSettings settings) {
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setTextZoom(100);
        webview.addJavascriptInterface(new NonameJavaScriptInterface(MainActivity.this, webview, preferences),
                "NonameAndroidBridge");
        WebView.setWebContentsDebuggingEnabled(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        boolean is64Bit = ProcessUtils.is64Bit();
        String[] supportBitAbis = is64Bit ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;

        // 内置的apk只有这3种，如果都不包含，就不触发升级内核操作（例如: 虚拟机需要x86）
        int indexOfArm64 = Arrays.binarySearch(supportBitAbis, "arm64-v8a");
        int indexOfArmeabi = Arrays.binarySearch(supportBitAbis, "armeabi-v7a");
        // int indexOfX86 = Arrays.binarySearch(supportBitAbis, "x86");

        Log.e(TAG, Arrays.toString(supportBitAbis));

        if (webViewInited || (indexOfArm64 < 0 && indexOfArmeabi < 0 /*&& indexOfX86 < 0*/)) {
            ActivityOnCreate(extras);
        } else {
            webViewInited = true;

            if (WebViewUpgradeProgressDialog == null) {
                WebViewUpgradeProgressDialog = new ProgressDialog(this);
                WebViewUpgradeProgressDialog.setTitle("正在更新Webview内核");
                WebViewUpgradeProgressDialog.setCancelable(false);
                WebViewUpgradeProgressDialog.setIndeterminate(false);
                WebViewUpgradeProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                WebViewUpgradeProgressDialog.setMax(100);
                WebViewUpgradeProgressDialog.setProgress(0);
                if (WebViewUpgradeProgressDialog.isShowing())
                    WebViewUpgradeProgressDialog.hide();
            }

            WebViewUpgrade.addUpgradeCallback(new UpgradeCallback() {
                @Override
                public void onUpgradeProcess(float percent) {
                    if (percent <= 0.9 && !WebViewUpgradeProgressDialog.isShowing()) {
                        WebViewUpgradeProgressDialog.show();
                    }
                    WebViewUpgradeProgressDialog.setProgress((int) (percent * 100));
                }

                @Override
                public void onUpgradeComplete() {
                    Log.e(TAG, "onUpgradeComplete");
                    WebViewUpgradeProgressDialog.setProgress(100);
                    ActivityOnCreate(extras);
                }

                @Override
                public void onUpgradeError(Throwable throwable) {
                    Log.e(TAG, "onUpgradeError: " + throwable.getMessage());
                    ActivityOnCreate(extras);
                }
            });

            try {
                // 添加webview
                UpgradeSource upgradeSource = new UpgradeAssetSource(
                        getApplicationContext(),
                        "com.google.android.webview_119.0.6045.194.apk",
                        new File(getApplicationContext().getFilesDir(),
                                "com.google.android.webview/119.0.6045.194.apk"));

                // 只使用119内核，不进行其他判断
                WebViewUpgrade.upgrade(upgradeSource);
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
                ActivityOnCreate(extras);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Bundle extras = intent.getExtras();
        Log.e(TAG, String.valueOf(extras != null));
        if (extras != null) {
            String extName = extras.getString("importExtensionName");
            boolean importPackage = extras.getBoolean("importPackage", false);
            View view = appView.getView();
            SystemWebView webview = (SystemWebView) view;
            if (webview != null) {
                if (extName != null) {
                    webview.evaluateJavascript("(() => {" +
                            "const event = new CustomEvent('importExtension', { " +
                            "detail: { extensionName: '" + extName + "'}" +
                            "});" +
                            "window.dispatchEvent(event);" +
                            "})();", null);
                }
                if (importPackage) {
                    webview.evaluateJavascript("(() => {" +
                            "const event = new CustomEvent('importPackage', { " +
                            "detail: { importPackage: true }" +
                            "});" +
                            "window.dispatchEvent(event);" +
                            "})();", null);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (appView != null) {
            SystemWebView webview = (SystemWebView) appView.getView();
            if (webview != null && webview.canGoBack()) {
                webview.goBack();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        if (WebViewUpgradeProgressDialog != null) {
            WebViewUpgradeProgressDialog.hide();
            WebViewUpgradeProgressDialog.dismiss();
            WebViewUpgradeProgressDialog = null;
        }

        super.onDestroy();
    }
}
