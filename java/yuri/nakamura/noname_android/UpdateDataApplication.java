package yuri.nakamura.noname_android;

import android.app.Application;

public class UpdateDataApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (getClass().getSuperclass() != Application.class) {
            throw new RuntimeException("this class is not my UpdateDataApplication");
        }
    }
}
