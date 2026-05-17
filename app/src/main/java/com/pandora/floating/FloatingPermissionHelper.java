package com.pandora.floating;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

/**
 * 悬浮窗权限帮助类
 * 处理Android 6.0 - 14.0各版本的权限适配
 */
public class FloatingPermissionHelper {

    public static final int REQUEST_OVERLAY_PERMISSION = 1001;

    /**
     * 检查是否拥有悬浮窗权限
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else {
            // Android 6.0之前，默认允许
            return true;
        }
    }

    /**
     * 申请悬浮窗权限
     */
    public static void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasOverlayPermission(activity)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName())
                );
                activity.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            }
        }
    }

    /**
     * 处理权限申请结果
     */
    public static boolean handlePermissionResult(Activity activity, int requestCode) {
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(activity)) {
                    Toast.makeText(activity, "悬浮窗权限已开启", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    Toast.makeText(activity, "请开启悬浮窗权限以使用该功能", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 检查并申请前台服务权限
     */
    public static boolean hasForegroundServicePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9.0+ 需要 FOREGROUND_SERVICE 权限
            return context.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * 检查是否有通知监听权限（用于获取媒体会话）
     */
    public static boolean hasNotificationListenerPermission(Context context) {
        String enabled = Settings.Secure.getString(
            context.getContentResolver(), 
            "enabled_notification_listeners"
        );
        String myService = context.getPackageName() + "/.floating.MusicNotificationListener";
        return enabled != null && enabled.contains(myService);
    }

    /**
     * 跳转到通知监听权限设置页面
     */
    public static void requestNotificationListenerPermission(Activity activity) {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        activity.startActivity(intent);
    }

    /**
     * 根据不同厂商跳转自启动设置页面
     */
    public static void goToAutoStartSettings(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Intent intent = null;

        try {
            switch (manufacturer) {
                case "xiaomi":
                    intent = new Intent();
                    intent.setComponent(new android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    ));
                    break;
                case "huawei":
                    intent = new Intent();
                    intent.setComponent(new android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    ));
                    break;
                case "oppo":
                    intent = new Intent();
                    intent.setComponent(new android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    ));
                    break;
                case "vivo":
                    intent = new Intent();
                    intent.setComponent(new android.content.ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    ));
                    break;
                default:
                    // 默认跳转到应用详情页
                    intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    break;
            }
            context.startActivity(intent);
        } catch (Exception e) {
            // 跳转失败，跳转到应用详情页
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        }
    }
}
