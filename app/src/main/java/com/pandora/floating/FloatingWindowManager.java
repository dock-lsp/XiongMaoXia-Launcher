package com.pandora.floating;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 悬浮窗统一管理器
 * 负责管理所有悬浮插件的生命周期
 */
public class FloatingWindowManager {

    private static final String TAG = "FloatingWindowManager";

    private static FloatingWindowManager instance;

    private Context context;
    private Map<String, DraggableFloatingView> floatingViews;

    // 悬浮窗类型常量
    public static final String TYPE_MUSIC = "music";
    public static final String TYPE_MAP = "map";
    public static final String TYPE_WEATHER = "weather";

    private FloatingWindowManager(Context context) {
        this.context = context.getApplicationContext();
        this.floatingViews = new HashMap<>();
    }

    public static synchronized FloatingWindowManager getInstance(Context context) {
        if (instance == null) {
            instance = new FloatingWindowManager(context);
        }
        return instance;
    }

    /**
     * 显示指定类型的悬浮窗
     */
    public void showFloatingWindow(String type) {
        if (!FloatingPermissionHelper.hasOverlayPermission(context)) {
            Log.w(TAG, "没有悬浮窗权限，请先申请");
            return;
        }

        DraggableFloatingView view = floatingViews.get(type);

        if (view == null) {
            view = createFloatingView(type);
            if (view != null) {
                floatingViews.put(type, view);
            }
        }

        if (view != null) {
            view.show();
        }
    }

    /**
     * 隐藏指定类型的悬浮窗
     */
    public void hideFloatingWindow(String type) {
        DraggableFloatingView view = floatingViews.get(type);
        if (view != null) {
            view.hide();
        }
    }

    /**
     * 隐藏所有悬浮窗
     */
    public void hideAll() {
        for (DraggableFloatingView view : floatingViews.values()) {
            view.hide();
        }
    }

    /**
     * 销毁指定悬浮窗
     */
    public void destroyFloatingWindow(String type) {
        DraggableFloatingView view = floatingViews.get(type);
        if (view != null) {
            view.hide();
            floatingViews.remove(type);
        }
    }

    /**
     * 销毁所有悬浮窗
     */
    public void destroyAll() {
        for (DraggableFloatingView view : floatingViews.values()) {
            view.hide();
        }
        floatingViews.clear();
    }

    /**
     * 获取指定悬浮窗
     */
    public DraggableFloatingView getFloatingView(String type) {
        return floatingViews.get(type);
    }

    /**
     * 检查悬浮窗是否正在显示
     */
    public boolean isShowing(String type) {
        DraggableFloatingView view = floatingViews.get(type);
        return view != null && view.getParent() != null;
    }

    /**
     * 创建悬浮窗
     */
    private DraggableFloatingView createFloatingView(String type) {
        switch (type) {
            case TYPE_MUSIC:
                return new MusicFloatingView(context);
            case TYPE_MAP:
                return new MapFloatingView(context);
            case TYPE_WEATHER:
                return new WeatherFloatingView(context);
            default:
                return null;
        }
    }

    /**
     * 切换悬浮窗显示状态
     */
    public void toggleFloatingWindow(String type) {
        if (isShowing(type)) {
            hideFloatingWindow(type);
        } else {
            showFloatingWindow(type);
        }
    }
}
