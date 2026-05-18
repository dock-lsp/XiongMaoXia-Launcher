package com.xmxcar.floating;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.xmxcar.launcher.R;

/**
 * 地图导航悬浮窗视图
 * 显示导航信息：剩余距离、路线指引、服务区信息
 */
public class MapFloatingView extends DraggableFloatingView {

    private TextView tvNaviInfo;
    private TextView tvDistance;
    private TextView tvNextTurn;
    private TextView tvServiceArea;

    public MapFloatingView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_floating_map, this, true);

        // 设置半透明背景
        setBackgroundColor(Color.parseColor("#CC1A1A1A"));

        // 初始化控件
        tvNaviInfo = findViewById(R.id.tv_navi_info);
        tvDistance = findViewById(R.id.tv_distance);
        tvNextTurn = findViewById(R.id.tv_next_turn);
        tvServiceArea = findViewById(R.id.tv_service_area);

        // 设置初始位置（左上角）
        layoutParams.x = dpToPx(20);
        layoutParams.y = dpToPx(100);
        layoutParams.width = dpToPx(220);
        layoutParams.height = dpToPx(120);

        // 设置示例数据
        setSampleData();
    }

    /**
     * 设置示例导航数据
     */
    private void setSampleData() {
        tvNaviInfo.setText("正在导航");
        tvDistance.setText("剩余 12.5 公里");
        tvNextTurn.setText("前方 500 米右转");
        tvServiceArea.setText("下一服务区：25 公里");
    }

    /**
     * 更新导航信息
     */
    public void updateNaviInfo(String naviStatus, String distance, String nextTurn, String serviceArea) {
        tvNaviInfo.setText(naviStatus != null ? naviStatus : "未导航");
        tvDistance.setText(distance != null ? distance : "--");
        tvNextTurn.setText(nextTurn != null ? nextTurn : "--");
        tvServiceArea.setText(serviceArea != null ? serviceArea : "--");
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
