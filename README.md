# WeChatVideoRecorder
**仿微信小视频录制功能。**
采用Camera类。

![image](/screenshot/1.gif)

# How to use++
**1、Add permission in your Manifest.xml**
```xml
    <!-- Permission to use camera - required -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="com.samsung.android.providers.context.permission.WRITE_USE_APP_FEATURE_SURVEY" />
    <uses-permission android:name="android.permission.FLASHLIGHT" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
    <!-- Camera features - recommended -->
    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.camera.autofocus"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.camera.flash"
        android:required="false" />

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

**2、Use in code**
```java
    findViewById(R.id.btn_main_takevideo).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, TakeVideoActivity.class);
            startActivity(intent);
        }
    });
```