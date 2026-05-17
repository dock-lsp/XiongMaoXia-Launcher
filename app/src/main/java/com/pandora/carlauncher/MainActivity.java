package com.pandora.carlauncher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pandora.floating.FloatingForegroundService;
import com.pandora.floating.FloatingPermissionHelper;
import com.pandora.floating.FloatingWindowManager;

/**
 * 熊猫侠车载启动器 - 主界面
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;

    private FloatingWindowManager floatingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        floatingManager = FloatingWindowManager.getInstance(this);

        initViews();
        checkAndRequestPermissions();
    }

    private void initViews() {
        // 音乐悬浮窗按钮
        Button btnMusicFloat = findViewById(R.id.btn_music_float);
        btnMusicFloat.setOnClickListener(v -> toggleFloatingWindow(FloatingWindowManager.TYPE_MUSIC));

        // 地图悬浮窗按钮
        Button btnMapFloat = findViewById(R.id.btn_map_float);
        btnMapFloat.setOnClickListener(v -> toggleFloatingWindow(FloatingWindowManager.TYPE_MAP));

        // 天气悬浮窗按钮
        Button btnWeatherFloat = findViewById(R.id.btn_weather_float);
        btnWeatherFloat.setOnClickListener(v -> toggleFloatingWindow(FloatingWindowManager.TYPE_WEATHER));

        // 显示所有悬浮窗
        Button btnShowAll = findViewById(R.id.btn_show_all);
        btnShowAll.setOnClickListener(v -> showAllFloatingWindows());

        // 隐藏所有悬浮窗
        Button btnHideAll = findViewById(R.id.btn_hide_all);
        btnHideAll.setOnClickListener(v -> hideAllFloatingWindows());

        // 权限设置按钮
        Button btnPermission = findViewById(R.id.btn_permission);
        btnPermission.setOnClickListener(v -> FloatingPermissionHelper.requestOverlayPermission(this));
    }

    /**
     * 切换悬浮窗显示状态
     */
    private void toggleFloatingWindow(String type) {
        if (!FloatingPermissionHelper.hasOverlayPermission(this)) {
            floatingManager.toggleFloatingWindow(type);

            // 如果显示悬浮窗，启动前台保活服务
            if (floatingManager.isShowing(type)) {
                FloatingForegroundService.startService(this);
                Toast.makeText(this, getTypeName(type) + "悬浮窗已开启", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getTypeName(type) + "悬浮窗已关闭", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show();
            FloatingPermissionHelper.requestOverlayPermission(this);
        }
    }

    /**
     * 显示所有悬浮窗
     */
    private void showAllFloatingWindows() {
        if (!FloatingPermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show();
            FloatingPermissionHelper.requestOverlayPermission(this);
            return;
        }

        floatingManager.showFloatingWindow(FloatingWindowManager.TYPE_MUSIC);
        floatingManager.showFloatingWindow(FloatingWindowManager.TYPE_MAP);
        floatingManager.showFloatingWindow(FloatingWindowManager.TYPE_WEATHER);

        FloatingForegroundService.startService(this);
        Toast.makeText(this, "所有悬浮窗已开启", Toast.LENGTH_SHORT).show();
    }

    /**
     * 隐藏所有悬浮窗
     */
    private void hideAllFloatingWindows() {
        floatingManager.hideAll();
        FloatingForegroundService.stopService(this);
        Toast.makeText(this, "所有悬浮窗已关闭", Toast.LENGTH_SHORT).show();
    }

    /**
     * 获取悬浮窗类型名称
     */
    private String getTypeName(String type) {
        switch (type) {
            case FloatingWindowManager.TYPE_MUSIC:
                return "音乐";
            case FloatingWindowManager.TYPE_MAP:
                return "地图";
            case FloatingWindowManager.TYPE_WEATHER:
                return "天气";
            default:
                return "";
        }
    }

    /**
     * 检查并请求必要权限
     */
    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.FOREGROUND_SERVICE
        };

        boolean needRequest = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "部分权限未授予，部分功能可能无法使用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FloatingPermissionHelper.handlePermissionResult(this, requestCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注意：不在这里销毁悬浮窗，让悬浮窗在后台继续运行
    }
}
