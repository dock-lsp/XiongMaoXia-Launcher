package com.xmxcar.floating;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.xmxcar.launcher.R;

/**
 * 天气悬浮窗视图
 * 显示当前天气信息
 */
public class WeatherFloatingView extends DraggableFloatingView {

    private TextView tvTemperature;
    private TextView tvWeather;
    private TextView tvCity;

    public WeatherFloatingView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_floating_weather, this, true);

        // 设置半透明背景
        setBackgroundColor(Color.parseColor("#CC2196F3"));

        // 初始化控件
        tvTemperature = findViewById(R.id.tv_temperature);
        tvWeather = findViewById(R.id.tv_weather);
        tvCity = findViewById(R.id.tv_city);

        // 设置初始位置（左下角）
        layoutParams.x = dpToPx(20);
        layoutParams.y = getScreenHeight() - dpToPx(150);
        layoutParams.width = dpToPx(150);
        layoutParams.height = dpToPx(80);

        // 设置示例数据
        setSampleData();
    }

    /**
     * 设置示例天气数据
     */
    private void setSampleData() {
        tvTemperature.setText("26°C");
        tvWeather.setText("晴");
        tvCity.setText("北京");
    }

    /**
     * 更新天气信息
     */
    public void updateWeatherInfo(String temperature, String weather, String city) {
        tvTemperature.setText(temperature != null ? temperature : "--°C");
        tvWeather.setText(weather != null ? weather : "--");
        tvCity.setText(city != null ? city : "--");
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
