package com.pandora.floating;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * 可拖动的悬浮视图基类
 * 支持拖动、边界检测、边缘吸附
 */
public abstract class DraggableFloatingView extends FrameLayout {

    protected WindowManager windowManager;
    protected WindowManager.LayoutParams layoutParams;

    private float initialX;
    private float initialY;
    private float initialTouchX;
    private float initialTouchY;

    private boolean isDragging = false;
    private static final int DRAG_THRESHOLD = 10; // 拖动阈值

    // 吸附相关
    private boolean enableSnapToEdge = true; // 是否启用边缘吸附
    private int snapThreshold = 50; // 吸附阈值

    public DraggableFloatingView(Context context) {
        super(context);
        init();
    }

    private void init() {
        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        layoutParams = createDefaultLayoutParams();
    }

    /**
     * 创建默认的LayoutParams配置
     */
    protected WindowManager.LayoutParams createDefaultLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params.format = PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        params.gravity = Gravity.START | Gravity.TOP;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        return params;
    }

    /**
     * 显示悬浮窗
     */
    public void show() {
        try {
            if (getParent() == null) {
                windowManager.addView(this, layoutParams);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 隐藏悬浮窗
     */
    public void hide() {
        try {
            if (getParent() != null) {
                windowManager.removeView(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新悬浮窗位置
     */
    protected void updatePosition(int x, int y) {
        layoutParams.x = x;
        layoutParams.y = y;
        windowManager.updateViewLayout(this, layoutParams);
    }

    /**
     * 获取屏幕宽度
     */
    protected int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取屏幕高度
     */
    protected int getScreenHeight() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = false;
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - initialTouchX;
                float deltaY = event.getRawY() - initialTouchY;

                if (!isDragging && (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD)) {
                    isDragging = true;
                    onDragStart();
                }

                if (isDragging) {
                    int newX = (int) (initialX + deltaX);
                    int newY = (int) (initialY + deltaY);

                    // 边界检测
                    newX = Math.max(0, Math.min(newX, getScreenWidth() - getWidth()));
                    newY = Math.max(0, Math.min(newY, getScreenHeight() - getHeight()));

                    updatePosition(newX, newY);
                    onDragging(newX, newY);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    onDragEnd();

                    // 边缘吸附
                    if (enableSnapToEdge) {
                        performSnapToEdge();
                    }
                } else {
                    // 点击事件
                    performClick();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 执行边缘吸附
     */
    private void performSnapToEdge() {
        int centerX = layoutParams.x + getWidth() / 2;
        int screenCenter = getScreenWidth() / 2;

        int targetX;
        if (centerX < screenCenter) {
            // 吸附到左边
            targetX = 0;
        } else {
            // 吸附到右边
            targetX = getScreenWidth() - getWidth();
        }

        // 只有距离边缘在阈值内才吸附
        if (Math.abs(layoutParams.x - targetX) < snapThreshold) {
            updatePosition(targetX, layoutParams.y);
        }
    }

    /**
     * 拖动开始回调
     */
    protected void onDragStart() {
        // 子类可以重写
    }

    /**
     * 拖动中回调
     */
    protected void onDragging(int x, int y) {
        // 子类可以重写
    }

    /**
     * 拖动结束回调
     */
    protected void onDragEnd() {
        // 子类可以重写
    }

    // ==================== Getter and Setter ====================

    public boolean isEnableSnapToEdge() {
        return enableSnapToEdge;
    }

    public void setEnableSnapToEdge(boolean enableSnapToEdge) {
        this.enableSnapToEdge = enableSnapToEdge;
    }

    public int getSnapThreshold() {
        return snapThreshold;
    }

    public void setSnapThreshold(int snapThreshold) {
        this.snapThreshold = snapThreshold;
    }

    public int getCurrentX() {
        return layoutParams.x;
    }

    public int getCurrentY() {
        return layoutParams.y;
    }
}
