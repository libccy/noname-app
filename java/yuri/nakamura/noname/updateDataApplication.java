package yuri.nakamura.noname;

import android.app.Application;
import android.util.Log;

import java.io.File;

public class updateDataApplication extends Application {
	private static final String TAG = "updateDataApplication";

	@Override
	public void onCreate() {
		super.onCreate();
		File dataPath = getFilesDir().getParentFile();
		Log.e(TAG, dataPath.getAbsolutePath());
		File xwalkCore = new File(dataPath, "app_xwalkcore/");
		if (xwalkCore.exists()) {
			File oldDataDirectory = new File(xwalkCore, "Default");
			File newDataDirectory = new File(xwalkCore, "DefaultProfile");
			// 新文件夹移动回旧文件夹
			if (newDataDirectory.exists()) {
				if (oldDataDirectory.exists()) {
					deleteFile(oldDataDirectory);
				}
				// 重命名文件夹以同步数据
				newDataDirectory.renameTo(new File(xwalkCore, "Default"));
			}
			File webviewCore = new File(dataPath, "app_webview/");
			if (webviewCore.exists()) {
				deleteFile(webviewCore);
			}
			// 重命名文件夹以同步数据
			xwalkCore.renameTo(webviewCore);
		}
	}

	private void deleteFile(File file) {
		if (file.exists()) {
			if (file.isFile()) {
				file.delete();
			} else if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (int i = 0; i < files.length; i++) {
					this.deleteFile(files[i]);
				}
				file.delete();
			}
		} else {
			Log.e(TAG, "所删除的文件不存在");
		}
	}
}
