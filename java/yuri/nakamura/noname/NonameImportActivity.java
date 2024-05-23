package yuri.nakamura.noname;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.palette.graphics.Palette;

import com.noname.api.Utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AbstractFileHeader;
import net.lingala.zip4j.model.ExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.progress.ProgressMonitor;

import org.apache.cordova.LOG;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import com.alibaba.fastjson.JSON;

public class NonameImportActivity extends Activity {
    private static final String TAG = "NonameImportActivity";

    // 1.不设置内置zip
    // 2.通过压缩包的Uri进入本Activity
    // 3.Activity绑定一个xml，引入json和自适应字体颜色
    // 4.解压zip,处理乱码和密码
    // 5.删除缓存
    // 6.进入游戏并保存压缩包信息，如果是扩展那就在webview执行保存

    /** 标题文字 */
    private TextView titleTextView;

    /** 展示文字 */
    private TextView messageTextView;

    /** 进度条 */
    private ProgressBar progressBar;

    private TextView currentMessage;

    private TextView subTitle;

    /** 压缩包密码 */
    private char[] password;

    /** 缓存的zip文件，cache/currentLoadFile.zip */
    private File cacheFile = null;

    /** zip实例 */
    private ZipFile zipFile = null;

    /** 储存乱码文件名 */
    private final Map<String, String> fixFileHeaders = new HashMap<>();

    /** zip根目录是否有noname.config.txt，仅在导入离线包有效 */
    private boolean hasConfigFile;

    /** json实例 */
    private JSONObject styleJson;

    /** 解压有错误不能进入游戏 */
    private boolean hasError = false;

    private File getPackageDir() {
        return getExternalFilesDir(null).getParentFile();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        titleTextView = findViewById(R.id.title);
        messageTextView = findViewById(R.id.messages);
        messageTextView.setAutoLinkMask(Linkify.WEB_URLS);
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        progressBar = findViewById(R.id.progress);
        currentMessage = findViewById(R.id.current_message);
        subTitle = findViewById(R.id.title2);

        // 低版本安卓需要存储权限
        ArrayList<String> permissions = new ArrayList<>();
        String [] requestPermissions = getRequestPermissions();
        Log.e(TAG, Arrays.toString(requestPermissions));
        for (String permission : requestPermissions) {
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(permission)) {
                permissions.add(permission);
            }
        }
        Log.e(TAG, permissions.toString());

        if (!permissions.isEmpty()) {
            StringBuilder permissionBuilder = new StringBuilder();
            for(String s : permissions){
                permissionBuilder.append(s);
                permissionBuilder.append(' ');
            }
            updateText("正在申请权限"+permissionBuilder);
            (new Handler(Looper.getMainLooper())).postDelayed(() -> {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 999);
            },100);
        } else {
            afterHasPermissions();
        }
    }

    /** 区分不同安卓版本的权限名称 */
    @NonNull
    private String[] getRequestPermissions() {
        String [] requestPermissions;
        if (Build.VERSION.SDK_INT < 33) {
            requestPermissions = new String[] {
                    // 读取文件权限
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    // 写入文件权限
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
        else {
            requestPermissions = new String[] {
                    // 读取图片权限
                    Manifest.permission.READ_MEDIA_IMAGES,
                    // 读取视频权限
                    Manifest.permission.READ_MEDIA_VIDEO,
                    // 读取音频权限
                    Manifest.permission.READ_MEDIA_AUDIO
            };
        }
        return requestPermissions;
    }

    /** 权限请求回调 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 999) {
            boolean hasDenied = false;
            StringBuilder text = new StringBuilder("您未授予");
            for (int index = 0; index < grantResults.length; index++) {
                int ret = grantResults[index];
                Log.e(TAG, permissions[index]);
                Log.e(TAG, String.valueOf(index));
                Log.e(TAG, "______________");
                if (ret != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT > 29) {
                        if (permissions[index].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                                permissions[index].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) continue;
                    }
                    text.append(permissions[index]).append(",");;
                    hasDenied = true;
                }
            }
            if (hasDenied &&
                    !getSharedPreferences("nonameyuri", MODE_PRIVATE)
                            .getBoolean("showFirstPermissionsDialog", false)) {
                text.append("权限。\n");
                text.append("如果您的设备小于安卓11，将因为无法正常使用读写功能而退出本页面。\n");
                text.append("如果您没有弹出窗口询问权限，可能是被系统的安全策略禁止，请在应用设置中手动授权应用的存储权限。");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);
                TextView textView = new TextView(this);
                textView.setText(text);
                textView.setTextSize(25);
                textView.setTextColor(Color.WHITE);
                builder.setView(textView);
                Toast.makeText(this, text.toString(), Toast.LENGTH_LONG).show();
                builder.setNegativeButton("知道了", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT < 30) {
                        finish();
                    }
                    else afterHasPermissions();
                });
                builder.create().show();
                getSharedPreferences("nonameyuri", MODE_PRIVATE)
                        .edit()
                        .putBoolean("showFirstPermissionsDialog", true)
                        .apply();
            }
            else {
                afterHasPermissions();
            }
        }
    }

    /** 更新显示信息 */
    @SuppressLint("SetTextI18n")
    private void updateText(final String msg) {
        runOnUiThread(() -> {
            if (messageTextView == null) return;
            if (messageTextView.getText().length() > 0) {
                Editable editable = (Editable) messageTextView.getText();
                Spanned HtmlText = Html.fromHtml(msg, Html.FROM_HTML_MODE_LEGACY);
                editable.insert(0, "\n");
                editable.insert(0, HtmlText);
            } else {
                messageTextView.setText(msg, TextView.BufferType.EDITABLE);
            }
        });
    }

    /** 权限申请后的回调 */
    private void afterHasPermissions() {
        setViewBackground(findViewById(R.id.main_linear), getStyleJson().optString("frame",""));
        setTextColor();
        // 如果有zip数据传入
        if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            loadZipData(getIntent().getData());
        }
        // 否则正常启动MainActivity
        else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            this.finish();
        }
    }

    /** app文件夹内是否有游戏主文件 */
    public boolean inited() {
        File appPath = getPackageDir();
        File[] files = new File[]{
                new File(appPath, "game/update.js"),
                new File(appPath, "game/config.js"),
                new File(appPath, "game/package.js"),
                new File(appPath, "game/game.js"),
        };
        for (File file : files) {
            if (!file.exists()) {
                return false;
            }
        }
        return true;
    }

    /** 导入压缩文件 */
    private void loadZipData(final Uri uri){
        new Thread() {
            public void run() {
                updateText("正在加载压缩包文件...");
                try {
                    // 把文件写入cache/currentLoadFile.zip
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    cacheFile = new File(getExternalCacheDir(), "currentLoadFile.zip");
                    Utils.inputStreamToFile(inputStream, cacheFile, 1024 * 1024 *100, bytes -> updateText("已读取" + (bytes / (1024 * 1024)) + "MB，请稍候"));
                    zipFile = new ZipFile(cacheFile);

                    if (!zipFile.isValidZipFile()) {
                        throw new Exception("压缩文件不合法,可能被损坏。");
                    }
                    else if (zipFile.getFileHeader("game/game.js") != null) {
                        updateText("压缩包被识别成游戏主文件包");
                        if (zipFile.getFileHeader("noname.config.txt") != null) {
                            hasConfigFile = true;
                        }
                        importPackage();
                        return;
                    }
                    else if (zipFile.getFileHeader("extension.js") != null || zipFile.getFileHeader("extension.ts") != null) {
                        updateText("压缩包被识别成扩展包");
                        if (!inited()) {
                            updateText("检测到您的文件缺失不能进入游戏，所以暂时不能导入扩展。请先导入离线包/完整包，或者在游戏的初始界面下载文件");
                            return;
                        }
                        importExtension();
                        return;
                    }
                    else {
                        // 判断是否是文件夹嵌套的扩展或主文件包
                        List<FileHeader> list = zipFile.getFileHeaders();
                        List<String> mainFiles = Arrays.asList(
                                "game/update.js",
                                "game/config.js",
                                "game/package.js",
                                "game/game.js"
                        );
                        boolean isMain = list.stream().anyMatch(fileHeader -> {
                            String fileName = fileHeader.getFileName();
                            return mainFiles.stream().anyMatch(fileName::endsWith);
                        });
                        boolean isExtension = list.stream().anyMatch(fileHeader -> {
                            String fileName = fileHeader.getFileName();
                            return (
                                    fileName.endsWith("/extension.js") &&
                                            !fileName.endsWith("/boss/extension.js") &&
                                            !fileName.endsWith("/cardpile/extension.js") &&
                                            !fileName.endsWith("/wuxing/extension.js") &&
                                            !fileName.endsWith("/coin/extension.js")
                            ) || fileName.endsWith("/extension.ts");
                        });
                        // 是文件夹嵌套的主文件包
                        if (isMain) {
                            updateText("压缩包被识别成文件夹嵌套的主文件包");
                            Object[] paths = list.stream()
                                    .filter(fileHeader -> {
                                        String fileName = fileHeader.getFileName();
                                        return fileName.endsWith("game/game.js");
                                    })
                                    .map(AbstractFileHeader::getFileName)
                                    .toArray();
                            // 取最短的路径
                            String path = "";
                            int strLen = -1;
                            for (Object p: paths) {
                                String p1 = p.toString();
                                if (p1.length() < strLen || strLen == -1) {
                                    strLen = p1.length();
                                    path = p1;
                                }
                            }
                            String rootPath = path.substring(0, path.indexOf("game/game.js"));
                            if (list.stream().filter(fileHeader -> (rootPath + "/noname.config.txt").equals(fileHeader.getFileName())).toArray().length > 0) {
                                hasConfigFile = true;
                            }
                            // updateText("rootPath: " + rootPath);
                            importPackage(rootPath);
                            return;
                        }
                        else if (isExtension) {
                            updateText("压缩包被识别成文件夹嵌套的扩展");
                            if (!inited()) {
                                updateText("检测到您的文件缺失不能进入游戏，所以暂时不能导入扩展。请先导入离线包/完整包，或者在游戏的初始界面下载文件");
                                return;
                            }
                            Object[] paths = list.stream()
                                    .filter(fileHeader -> {
                                        String fileName = fileHeader.getFileName();
                                        return fileName.endsWith("/extension.js") || fileName.endsWith("/extension.ts");
                                    })
                                    .map(AbstractFileHeader::getFileName)
                                    .toArray();
                            // 取最短的路径
                            String path = "";
                            int strLen = -1;
                            for (Object p: paths) {
                                String p1 = p.toString();
                                if (p1.length() < strLen || strLen == -1) {
                                    strLen = p1.length();
                                    path = p1;
                                }
                            }
                            String rootPath = path.substring(0, path.indexOf(path.endsWith("/extension.js") ? "extension.js" : "extension.ts"));
                            // updateText("rootPath: " + rootPath);
                            importExtension(rootPath);
                            return;
                        }
                    }
                    /*
                        updateText("压缩包识别失败，请手动选择目录导入");
                        // 手动选择目录
                        Intent ListViewIntent = new Intent(NonameImportActivity.this, ListViewActivity.class);
                        ListViewIntent.putExtra("type", "folder");
                        startActivityForResult(ListViewIntent, 2);
                    */
                    updateText("压缩包识别失败");
                } catch (Exception e) {
                    e.printStackTrace();
                    updateText("文件解压出现异常，已停止解压：" + e.getMessage());
                }
            }
        }.start();
    }

    /** 导入扩展 */
    private void importExtension() throws Exception {
        if (cacheFile.length() >= 50 * 1024 * 1024) {
            updateText("这个文件比较大，请耐心等待。");
        }
        if (zipFile.isEncrypted()) {
            updateText("这个文件需要密码");
            setPassword(1, null);
        } else {
            importExtension2();
        }
    }

    /** 导入文件夹嵌套的扩展 */
    private void importExtension(String rootPath) throws Exception {
        if (cacheFile.length() >= 50 * 1024 * 1024) {
            //ToastUtils.show(NonameImportActivity.this,  "这个文件比较大，请耐心等待。");
            updateText("这个文件比较大，请耐心等待。");
        }
        if (zipFile.isEncrypted()) {
            updateText("这个文件需要密码");
            setPassword(1, rootPath);
        } else {
            importExtension2(rootPath);
        }
    }

    /** 导入离线包/完整包 */
    private void importPackage() throws Exception {
        if (cacheFile.length() >= 50 * 1024 * 1024) {
            updateText("这个文件比较大，请耐心等待。");
        }
        if (zipFile.isEncrypted()) {
            updateText("这个文件需要密码");
            setPassword(2, null);
        } else {
            importPackage2();
        }
    }

    /** 导入文件夹嵌套的离线包/完整包 */
    private void importPackage(String rootPath) throws Exception {
        if (cacheFile.length() >= 50 * 1024 * 1024) {
            updateText("这个文件比较大，请耐心等待。");
        }
        if (zipFile.isEncrypted()) {
            updateText("这个文件需要密码");
            setPassword(2, rootPath);
        } else {
            importPackage2(rootPath);
        }
    }

    /** 输入密码然后继续进行解压逻辑 */
    private void setPassword(int method, String rootPath) {
        runOnUiThread(() -> {
            final EditText editText = new EditText(NonameImportActivity.this);
            new AlertDialog.Builder(NonameImportActivity.this)
                    .setTitle("请输入压缩包密码")
                    .setView(editText)
                    .setCancelable(false)
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton("确定", (dialogInterface, i) -> {
                        if (editText.getText().length() == 0) {
                            // alertDialog框消失后重新出现
                            setPassword(method, rootPath);
                        } else {
                            char[] password = editText.getText().toString().toCharArray();
                            this.password = password;
                            zipFile.setPassword(password);
                            try {
                                if (method == 1) {
                                    if (rootPath != null) {
                                        importExtension2(rootPath);
                                    } else {
                                        importExtension2();
                                    }
                                } else if (method == 2) {
                                    if (rootPath != null) {
                                        importPackage2(rootPath);
                                    } else {
                                        importPackage2();
                                    }
                                } else if (method == 3) {
                                    extractAll(rootPath, null, null);
                                }
                            } catch (Exception e) {
                                String message = e.getMessage();
                                if (message != null) {
                                    if ("Wrong password!".equals(e.getMessage())) {
                                        updateText("密码错误！请重新输入密码！");
                                        setPassword(method, rootPath);
                                    } else {
                                        e.printStackTrace();
                                        updateText("解压失败！\n" + e.getMessage());
                                    }
                                } else {
                                    updateText("解压失败！");
                                }
                            }
                        }
                    }).create().show();
        });
    }

    /** 修复乱码文件名 */
    private void fixGarbledFileNames() throws IOException {
        List<FileHeader> list = zipFile.getFileHeaders();
        for (FileHeader fileHeader : list) {
            if (fileHeader.getExtraDataRecords() != null) {
                for (ExtraDataRecord extraDataRecord : fileHeader.getExtraDataRecords()) {
                    long identifier = extraDataRecord.getHeader();
                    if (identifier == 0x7075) {
                        byte[] bytes = extraDataRecord.getData();
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        byte version = buffer.get();
                        // assert (version == 1);
                        if (version == 1) {
                            // LOG.e(TAG, Arrays.toString(bytes));
                            String garbledName = fileHeader.getFileName();
                            String fixedName = new String(bytes, 5, bytes.length - 5, StandardCharsets.UTF_8);
                            updateText("修正乱码前的文件名: " + garbledName);
                            updateText("修正乱码后的文件名: " + fixedName);
                            LOG.e(TAG, "修正乱码前的文件名: " + garbledName);
                            LOG.e(TAG, "修正乱码后的文件名: " + fixedName);
                            fixFileHeaders.put(garbledName, fixedName);
                            // fileHeader.setFileName(fixedName);
                            // fileHeader.setFileNameLength(fixedName.length());
                            LOG.e(TAG, "-----------");
                            break;
                        }
                    }
                }
            }
        }
        if (!fixFileHeaders.isEmpty()) {
            fixFileHeaders.forEach((key, value) -> {
                updateText("自动修正乱码后的文件名: " + value);
            });
            // zipFile.renameFiles(fixFileHeaders);
        }
    }

    /** 从文件中获取扩展名称 */
    private String getExtensionName(File file) throws Exception {
        // new json file
        File jsonFile = new File(file.getParentFile(), "info.json");
        Log.e(TAG, jsonFile.getAbsolutePath());
        if (jsonFile.exists() && jsonFile.canRead() && jsonFile.isFile()) {
            InputStream in = new BufferedInputStream(new FileInputStream(jsonFile));
            Scanner s = new Scanner(in).useDelimiter("\\A");
            String conf = s.hasNext() ? s.next() : "";
            // String conf = IOUtils.toString(in, StandardCharsets.UTF_8);
            String name = JSON.parseObject(conf).getString("name");
            if (name != null) {
                Log.e(TAG, name);
                updateText("从info.json解析出的扩展名为: " + name);
                return name;
            }
        }
        // old
        if (!file.exists() && "extension.js".equals(file.getName())) {
            file = new File(file.getParentFile(), "extension.ts");
        }
        Scanner scanner = new Scanner(file);
        boolean appear = false;
        String s = "name:\"";
        String s2 = "name: \"";
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.trim();
            if (line.startsWith("/// <reference path=") || line.startsWith("///<reference path=")) {
                continue;
            }
            // 未格式化的扩展
            if (line.contains("game.import(")) {
                appear = true;
                int index = line.indexOf(s);
                if (index < 0) {
                    continue;
                }
                String ret = line.substring(index + s.length(), line.indexOf('\"', index + s.length()));
                if (!ret.isEmpty()) {
                    Log.e(TAG, ret);
                    updateText("从extension.js解析出的扩展名为: " + ret);
                    return ret;
                }
            }
            if (appear && (line.startsWith(s) || line.startsWith(s2))) {
                String str = line.startsWith(s) ? s : s2;
                int length = str.length();
                int index = line.indexOf(str);
                String ret = line.substring(index + length, line.indexOf('\"', index + length));
                if (!ret.isEmpty()) {
                    Log.e(TAG, ret);
                    updateText("从extension.js解析出的扩展名为: " + ret);
                    return ret;
                }
            }
        }
        throw new Exception("解析扩展名失败");
    }

    /** 从扩展名获取文件实例 */
    private File getExtensionFile(String extname) {
        File file = getPackageDir();
        return new File(file, "extension/" + extname);
    }

    /** 密码输入完成后，判断扩展名称并解压 */
    private void importExtension2() throws Exception {
        fixGarbledFileNames();
        File cacheDir = new File(getExternalCacheDir(), Utils.getRandomString(10));
        try { zipFile.extractFile("extension.js", cacheDir.getPath()); } catch (ZipException ignored) {}
        try { zipFile.extractFile("extension.ts", cacheDir.getPath()); } catch (ZipException ignored) {}
        try { zipFile.extractFile("info.json", cacheDir.getPath()); } catch (ZipException ignored) {}
        File cacheJs = new File(cacheDir, "extension.js");
        try {
            // ts的扩展名还是以info.json解析为准
            String extensionName = getExtensionName(cacheJs);
            updateText("扩展名解析为：" + extensionName);
            runOnUiThread(() -> {
                final EditText editText = new EditText(NonameImportActivity.this);
                editText.setText(extensionName);
                new AlertDialog.Builder(NonameImportActivity.this)
                        .setTitle("请确认扩展名是否正确")
                        .setView(editText)
                        .setCancelable(false)
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                            if (editText.getText().length() == 0) {
                                Toast.makeText(NonameImportActivity.this,  "请输入扩展名！", Toast.LENGTH_LONG).show();
                            } else {
                                final String extName = editText.getText().toString();
                                updateText("正在解压到对应扩展文件夹。请耐心等待。");
                                extractAll(getExtensionFile(extName).getPath(), cacheDir, extName);
                            }
                        }).create().show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                final EditText editText = new EditText(NonameImportActivity.this);
                new AlertDialog.Builder(NonameImportActivity.this)
                        .setTitle("扩展名解析失败，请手动输入扩展名（纯扩展名，不包含版本号等信息）")
                        .setView(editText)
                        .setCancelable(false)
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                            if (editText.getText().length() == 0) {
                                Toast.makeText(NonameImportActivity.this,  "请输入扩展名！", Toast.LENGTH_LONG).show();
                            } else {
                                final String extensionName = editText.getText().toString();
                                updateText("扩展名输入为：" + extensionName + "\n正在解压到对应扩展文件夹。请耐心等待。");
                                extractAll(getExtensionFile(extensionName).getPath(), cacheDir, extensionName);
                            }
                        }).create().show();
            });
        }
    }

    private void importExtension2(String rootPath) throws Exception {
        fixGarbledFileNames();
        // cache里创建一个随机字符串的文件夹
        String randomString = Utils.getRandomString(10);
        File cacheDir = new File(getExternalCacheDir(), randomString);
        // 把extension.js解压到随机字符串的文件夹
        try { zipFile.extractFile(rootPath + "extension.js", cacheDir.getPath()); } catch (ZipException ignored) {}
        try { zipFile.extractFile(rootPath + "extension.ts", cacheDir.getPath()); } catch (ZipException ignored) {}
        try { zipFile.extractFile(rootPath + "info.json", cacheDir.getPath()); } catch (ZipException ignored) {}
        File cacheJs = new File(cacheDir, rootPath + "extension.js");
        String [] split = rootPath.split("/");
        new Thread() {
            public void run() {
                try {
                    String extensionNameFromDir = split[split.length - 1];
                    String extensionNameFromJs = getExtensionName(cacheJs);
                    updateText("从文件夹名解析扩展名为: " + extensionNameFromDir);
                    updateText("从js/ts/json文件解析扩展名为: " + extensionNameFromJs);
                    String extensionName;
                    if (extensionNameFromDir.equals(extensionNameFromJs)) {
                        extensionName = extensionNameFromDir;
                    } else {
                        updateText("解析结果不同，以js/ts/json文件的解析结果为准");
                        extensionName = extensionNameFromJs;
                    }
                    updateText("扩展名解析为：" + extensionName);
                    runOnUiThread(() -> {
                        final EditText editText = new EditText(NonameImportActivity.this);
                        new AlertDialog.Builder(NonameImportActivity.this)
                                .setTitle("请确认扩展名是否正确")
                                .setView(editText)
                                .setCancelable(false)
                                .setPositiveButton("确定", (dialogInterface, num) -> {
                                    if (editText.getText().length() == 0) {
                                        Toast.makeText(NonameImportActivity.this,  "请输入扩展名！", Toast.LENGTH_LONG).show();
                                    } else {
                                        final String extName = editText.getText().toString();
                                        updateText("开始检测解压文件，此过程时间可能会较长，请耐心等待。");
                                        try {
                                            // 获取到extension的文件夹位置
                                            File extDir = getExtensionFile(extName);
                                            if (!extDir.exists()) {
                                                extDir.mkdir();
                                            }
                                            // updateText("rootPath: " + rootPath);
                                            // updateText("原文件数量: " + zipFile.getFileHeaders().size());
                                            // 移除除了rootPath外的文件
                                            List<String> filesToRemove = new ArrayList<>();
                                            Map<String, String> fileNamesMap = new HashMap<>();
                                            List<FileHeader> list = zipFile.getFileHeaders();
                                            for (FileHeader f: list) {
                                                String name = f.getFileName();
                                                // 如果rootPath是a/b时，name为a此判断依旧返回真
                                                // if (!name.startsWith(rootPath)) {
                                                if (!name.startsWith(rootPath) && rootPath.indexOf(name) != 0) {
                                                    filesToRemove.add(name);
                                                } else if(!f.isDirectory()) {
                                                    fileNamesMap.put(name, name.substring(rootPath.length()));
                                                }
                                            }
                                            // zip文件中删除多个文件和文件夹
                                            zipFile.removeFiles(filesToRemove);
                                            // 把rootPath中的文件移动到zip根目录
                                            zipFile.renameFiles(fileNamesMap);
                                            // 删除rootPath
                                            zipFile.removeFile(rootPath);
                                            // 循环删除
                                            if (rootPath.split("/").length > 1) {
                                                String[] split = rootPath.split("/");
                                                for (int i = split.length - 1; i > -1; i--) {
                                                    String p = "";
                                                    for (int j = 0; j <= i; j++) {
                                                        p = p + split[j] + '/';
                                                    }
                                                    zipFile.removeFile(p);
                                                }
                                            }
                                            // 解压zip
                                            // updateText("filesToRemove: " + filesToRemove.size());
                                            // updateText("fileNamesMap: " + fileNamesMap.size());
                                            // zipFile.extractAll(extDir.getPath());
                                            extractAll(extDir.getPath(), cacheDir, extName);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            updateText("解压失败，已停止解压\n" + e);
                                        }
                                    }
                                }).create().show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    updateText("解压失败，已停止解压\n" + e);
                }
            }
        }.start();
    }

    /** 密码输入完成后解压 */
    private void importPackage2() throws Exception {
        fixGarbledFileNames();
        File data = getPackageDir();
        new Thread() {
            public void run() {
                try {
                    extractAll(data.getPath(), null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    updateText("解压失败，已停止解压\n" + e.getMessage());
                }
            }
        }.start();
    }

    private void importPackage2(String rootPath) throws Exception {
        fixGarbledFileNames();
        File data = getPackageDir();
        new Thread() {
            public void run() {
                try {
                    // 移除除了rootPath外的文件
                    List<String> filesToRemove = new ArrayList<>();
                    Map<String, String> fileNamesMap = new HashMap<>();
                    List<FileHeader> list = zipFile.getFileHeaders();
                    for (FileHeader f: list) {
                        String name = f.getFileName();
                        if (!name.startsWith(rootPath) && rootPath.indexOf(name) != 0) {
                            filesToRemove.add(name);
                        } else if(!f.isDirectory()) {
                            fileNamesMap.put(name, name.substring(rootPath.length()));
                        }
                    }
                    // zip文件中删除多个文件和文件夹
                    zipFile.removeFiles(filesToRemove);
                    // 把rootPath中的文件移动到zip根目录
                    zipFile.renameFiles(fileNamesMap);
                    // 删除rootPath
                    zipFile.removeFile(rootPath);
                    // 循环删除
                    if (rootPath.split("/").length > 1) {
                        String[] split = rootPath.split("/");
                        for (int i = split.length - 1; i > -1; i--) {
                            StringBuilder p = new StringBuilder();
                            for (int j = 0; j <= i; j++) {
                                p.append(split[j]).append('/');
                            }
                            zipFile.removeFile(p.toString());
                        }
                    }
                    // 解压zip
                    extractAll(data.getPath(), null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    updateText("解压失败，已停止解压\n" + e.getMessage());
                }
            }
        }.start();
    }

    /** 解压文件 */
    private void extractAll(String filePath, File cacheDir, String extName) {
        runOnUiThread(() -> {
            // zipFile = new ZipFile(zipFile.getFile());
            // zipFile.setPassword(this.password);
            subTitle.setText("正在解压");
            new Thread(){
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    try {
                        int size = zipFile.getFileHeaders().size();
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(() -> {
                            subTitle.setText("正在解压");
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setMax(100);
                            progressBar.setProgress(0);
                            // currentMessage.setTextColor(getRandomColor());
                            currentMessage.setText(getWaitingMessage());
                            subTitle.setText("正在解压");
                        });
                        updateText("开始解压zip(共" + size + "个文件)，请耐心等待");

                        // 解压操作
                        zipFile.setRunInThread(true);
                        zipFile.extractAll(filePath, fixFileHeaders);
                        // 解压进度
                        final ProgressMonitor progressMonitor = zipFile.getProgressMonitor();

                        while (!progressMonitor.getState().equals(ProgressMonitor.State.READY)) {
                            Log.e(TAG, "Percentage done: " + progressMonitor.getPercentDone());
                            Log.e(TAG, "Current file: " + progressMonitor.getFileName());
                            handler.post(() -> {
                                // subTitle.setText("正在解压" + progressMonitor.getFileName());
                                progressBar.setProgress(progressMonitor.getPercentDone());
                                currentMessage.setText(getWaitingMessage());
                            });
                            Thread.sleep(200);
                        }

                        if (progressMonitor.getResult().equals(ProgressMonitor.Result.ERROR)) {
                            Log.e(TAG, "Error occurred. Error message: " + progressMonitor.getException().getMessage());
                            updateText("<font color='red'>解压失败：" + progressMonitor.getException().getMessage() + "</font>");
                            progressMonitor.getException().printStackTrace();
                            Log.e(TAG, progressMonitor.getFileName());
                        }
                        else if (progressMonitor.getResult().equals(ProgressMonitor.Result.CANCELLED)) {
                            Log.e(TAG, "Task cancelled");
                            updateText("<font color='red'>解压失败：进程取消</font>");
                        }
                        else if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)) {
                            zipFile.setRunInThread(false);
                            updateText("<font color='green'>解压完成</font>");
                            handler.post(() -> {
                                progressBar.setProgress(100);
                            });
                            if (hasConfigFile) {
                                File configFile = new File(filePath, "noname.config.txt");
                                if (configFile.exists() && configFile.isFile()) {
                                    try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                                        StringBuilder sb = new StringBuilder();
                                        String line;
                                        while ((line = br.readLine()) != null) {
                                            sb.append(line);
                                            sb.append(System.lineSeparator());
                                        }
                                        getSharedPreferences("nonameyuri", MODE_PRIVATE)
                                                .edit()
                                                .putString("config", sb.toString())
                                                .apply();
                                        updateText("读取配置文件成功，启动后将自动导入");
                                        configFile.delete();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        updateText("读取配置文件异常: " + e.getMessage());
                                    }
                                }
                            } else updateText("该压缩包没有配置文件");
                            // 清除缓存
                            clearCache(cacheDir);
                            // 进入无名杀
                            if (extName != null) {
                                afterFinishImportExtension(extName);
                            } else {
                                afterFinishImportExtension();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        updateText("<font color='red'>解压遇到错误，已停止解压：" + e.getMessage() +"</font>");
                        e.printStackTrace();
                        hasError = true;
                    }
                }
            }.start();
        });
    }

    /** 删除缓存文件 */
    private void clearCache(File cacheDir) {
        if (cacheDir != null) {
            updateText("正在清除缓存");
            try {
                removeDir(cacheDir);
                updateText("清除缓存成功！");
            } catch (Exception e) {
                updateText("清除缓存失败！" + e.getMessage());
            }
        }
        // 删除cache/currentLoadFile.zip文件
        try {
            cacheFile.delete();
        } catch (Exception e) {
            updateText("删除cache/currentLoadFile.zip失败，请手动清除应用缓存: " + e.getMessage());
        }
    }

    /** 递归删除非空文件夹 */
    private void removeDir(File file){
        // 获取该文件下所有子文件和子文件夹
        File[] files = file.listFiles();

        // 循环遍历数组中的所有子文件和文件夹
        if (files != null){
            // 判断是否是文件，如果是，就删除
            for (File file1 : files) {
                if (file1.isFile()) {
                    file1.delete();
                }
                // 在循环中，判断遍历出的是否是文件夹
                if (file1.isDirectory()){
                    // 如果是文件夹,就递归删除里面的文件
                    removeDir(file1);
                    // 删除该文件夹里所有文件后,当前文件夹就为空了,那么就可以删除该文件夹了
                    file1.delete();
                }
            }
        }
        // 删除完里面的文件夹后，当前文件夹也删除
        file.delete();
    }

    /** 不延时进入游戏 */
    private void afterFinishImportExtension() {
        if (hasError) return;
        updateText("<font color='green'>正在为你启动无名杀</font>");
        //Intent intent = new Intent(this, MainActivity.class);
        PackageManager packageManager = this.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
        intent.setPackage(null);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("importPackage", true);
        startActivity(intent);
        finish();
    }


    /** 延时1.5S进入游戏 */
    private void afterFinishImportExtension(String extName) {
        if (hasError) return;

        PackageManager packageManager = this.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
        intent.setPackage(null);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("importExtensionName", extName);

        updateText("<font color='green'>正在为你启动无名杀</font>");
        Timer timer = new Timer();
        timer.schedule(new TimerTask(){
            public void run(){
                timer.cancel();
                startActivity(intent);
                finish();
            }
        }, 1500);
    }

    /** 根据json配置导入界面的样式信息 */
    private JSONObject getStyleJson() {
        if (styleJson != null) {
            return styleJson;
        }
        try {
            File data = getExternalFilesDir("apk");
            File file = new File(data, "style.json");
            Scanner scanner = new Scanner(file);
            StringBuilder stringBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine());
            }
            scanner.close();
            styleJson = new JSONObject(stringBuilder.toString());
            return styleJson;
        } catch (Throwable e){
            return new JSONObject();
        }
    }

    /**
     * 根据json配置导入界面的背景图片
     */
    private void setViewBackground(View view, String path) {
        if (TextUtils.isEmpty(path)) return;
        File file = new File(getPackageDir(),path);
        if (!file.exists()) {
            updateText("文件" + path+  "不存在");
            return;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
            view.setBackground(new BitmapDrawable(getResources(),bitmap));
            fileInputStream.close();
        } catch (Throwable e) {
            updateText("图片加载失败");
        }
    }

    /** 提取view的背景图片信息 */
    private Bitmap getViewBitmap(View view) {
        // 获取View的宽度和高度
        int width = view.getWidth();
        int height = view.getHeight();

        // 如果View还没有测量尺寸，则需要先强制测量
        if (width == 0 || height == 0) {
            // 测量子View的宽高
            view.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            // 获取测量后的尺寸
            width = view.getMeasuredWidth();
            height = view.getMeasuredHeight();
        }

        // 创建一个与View尺寸匹配的Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 创建一个Canvas，并将Bitmap绑定到Canvas上
        Canvas canvas = new Canvas(bitmap);

        // 将View的内容绘制到Canvas上
        view.draw(canvas);

        // 返回生成的Bitmap
        return bitmap;
    }

    private void setTextColor() {
        JSONObject style = getStyleJson();
        getWindow().getDecorView().post(() -> {
            try {
                Bitmap bitMap = null;
                String wallpaperPath = style.optString("wallpaper","");
                updateText("配置壁纸地址" + wallpaperPath);
                File bitmapFile = new File(getPackageDir(),wallpaperPath);
                if (!TextUtils.isEmpty(wallpaperPath) && bitmapFile.exists()){
                    try {
                        FileInputStream fileInputStream = new FileInputStream(bitmapFile);
                        bitMap = BitmapFactory.decodeStream(fileInputStream);
                        getWindow().getDecorView().setBackground(new BitmapDrawable(getResources(),bitMap));
                        if (bitMap == null){
                            updateText("无法解析壁纸");
                        }
                    } catch (Throwable e){
                        updateText("解析壁纸失败");
                    }
                } else {
                    updateText("壁纸配置文件" + wallpaperPath + "不存在");
                }
                if (bitMap == null) {
                    updateText("正在加载默认壁纸");
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                    if (wallpaperManager.isWallpaperSupported() &&
                            Build.VERSION.SDK_INT < 33 &&
                            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // 默认获取系统壁纸
                        BitmapDrawable bitmapDrawable = (BitmapDrawable) wallpaperManager.getDrawable();
                        // 获取系统壁纸的Bitmap
                        bitMap = bitmapDrawable.getBitmap();
                    } else {
                        View view = getWindow().getDecorView();
                        // 截屏获取view
                        bitMap = getViewBitmap(view);
                    }
                }
                JSONObject colorJson = getStyleJson().optJSONObject("textColor");
                final String titleColor = colorJson == null ? "" : colorJson.optString("title","");
                final String messageColor = colorJson == null ? "" :colorJson.optString("message","");
                if (!TextUtils.isEmpty(titleColor) && !TextUtils.isEmpty(messageColor)){
                    titleTextView.setTextColor(Color.parseColor(titleColor));
                    messageTextView.setTextColor(Color.parseColor(messageColor));
                }
                else {
                    Palette.from(bitMap).maximumColorCount(10).generate(palette -> {
                        Palette.Swatch s = palette.getDominantSwatch();      // 独特的一种
                        Palette.Swatch s1 = palette.getVibrantSwatch();      // 获取到充满活力的这种色调
                        Palette.Swatch s2 = palette.getDarkVibrantSwatch();  // 获取充满活力的黑
                        Palette.Swatch s3 = palette.getLightVibrantSwatch(); // 获取充满活力的亮
                        Palette.Swatch s4 = palette.getMutedSwatch();        // 获取柔和的色调
                        Palette.Swatch s5 = palette.getDarkMutedSwatch();    // 获取柔和的黑
                        Palette.Swatch s6 = palette.getLightMutedSwatch();   // 获取柔和的亮
                        Log.e("Palette", "s1为: " + (s1 != null ? Integer.toHexString(s1.getRgb()) : "null"));
                        Log.e("Palette", "s2为: " + (s2 != null ? Integer.toHexString(s2.getRgb()) : "null"));
                        Log.e("Palette", "s3为: " + (s3 != null ? Integer.toHexString(s3.getRgb()) : "null"));
                        Log.e("Palette", "s4为: " + (s4 != null ? Integer.toHexString(s4.getRgb()) : "null"));
                        Log.e("Palette", "s5为: " + (s5 != null ? Integer.toHexString(s5.getRgb()) : "null"));
                        Log.e("Palette", "s6为: " + (s6 != null ? Integer.toHexString(s6.getRgb()) : "null"));
                        if (s6 != null) {
                            titleTextView.setTextColor(TextUtils.isEmpty(titleColor)?s6.getRgb():Color.parseColor(titleColor));
                            messageTextView.setTextColor(TextUtils.isEmpty(messageColor)?s6.getRgb():Color.parseColor(messageColor));
                            Log.e("Palette", "已将字体颜色替换为: " + s6.getRgb());
                        } else {
                            titleTextView.setTextColor(Color.parseColor(titleColor));
                            messageTextView.setTextColor(Color.parseColor(messageColor));
                        }
                        if (s5 != null) {
                            titleTextView.setShadowLayer(10, 5, 5, s5.getRgb());
                            messageTextView.setShadowLayer(10, 5, 5, s5.getRgb());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                updateText("<font color='red'>获取壁纸主色调失败：" + e.getMessage() + "</font>");
                Log.e(TAG, e.getMessage());
            }
        });
    }

    public static final String[] WAITING_MESSAGES = {
            "无名杀的创造者是水乎，也叫村长。",
            "最好不要在扩展文件里包含除英文字母和英文标点之外的任何符号，防止导入的时候麻烦。",
            "苏婆玛丽奥是无名杀的现任更新者。",
            "GPLv3协议提倡开源与共享，是无名杀代码的基础协议。",
            "萌新记着要看公告和教程哦，不要频繁提问，大佬很忙的。",
            "导入很慢吗？不要急，好饭不怕晚。",
            "无名杀的代码是用JavaScript写成的。",
            "我知道你很急，但是你先别急。",
            "无名杀扩展内置的导入键并不好用，不建议使用。",
            "无名杀的外壳和本体代码是分开的，目前本体仓库在GitHub上由苏婆进行更新维护。",
            "代码混淆的扩展有一定的风险，请谨慎甄别发布者。",
            "不要退出，你也不想导入出问题吧？",
            "请不要拿无名杀去别的圈子招仇恨，这种行为并不会显得你很智慧。",
            "写扩展，最好学会手动编辑文件。内置的编辑器太坑啦！",
            "无名杀的所有扩展，都有遵守GPLv3协议的义务。",
            "写扩展不是为了竞争武将强度的，孙悟空可以三棒槌打死鲁智深，但那并没啥意思。",
            "如果碰到错误弹窗，请滑动截取完整的弹窗信息再去询问作者。不然他也看不明白什么情况。",
            "越开放的扩展，在传播上有更多的优势。",
            "熟练掌握万能导入法，防止一切崩溃问题。",
            "强中更有强中手，一山更比一山高。",
            "关于无名杀的种种问题，牢记别人帮你是情分，别人不帮你是本分。",
            "无名杀不排斥开发自己的版本，但是必须遵守GPL协议。",
            "如果你导入扩展后出现弹窗，请关闭扩展后再尝试，如果是扩展问题，联系扩展作者解决。",
            "无名杀的仓库在Github上，任何人都可以提交代码。",
            "技能的async/await写法比传统step写法更为方便。",
            "推荐使用Visual Studio Code编辑扩展代码。",
            "提问需要技巧，贸然的提问只会让人摸不着头脑。",
            "建议先去学习JavaScript的基础语法，再了解自定义技能相关教程。",
            "GPLv3协议保障作者的署名权，引用GPLv3协议的代码的项目，需要以相同的协议开源。"
    };

    private int currentWaiting = 0;

    private long lastWaitingTime = 0;

    private String getWaitingMessage(){
        if (currentWaiting % WAITING_MESSAGES.length == 0) {
            for (int i = 0; i < WAITING_MESSAGES.length; i++) {
                String a = WAITING_MESSAGES[i];
                int random = (int)(Math.random() * WAITING_MESSAGES.length);
                String b = WAITING_MESSAGES[random];
                WAITING_MESSAGES[i] = b;
                WAITING_MESSAGES[random] = a;
            }
            currentWaiting += 1;
        }
        String ret = WAITING_MESSAGES[currentWaiting % WAITING_MESSAGES.length];
        long ct = System.currentTimeMillis();
        if (ct - lastWaitingTime >= 3500) {
            lastWaitingTime = ct;
            currentWaiting += 1;
        }
        return "小提示：" + ret;
    }
}
