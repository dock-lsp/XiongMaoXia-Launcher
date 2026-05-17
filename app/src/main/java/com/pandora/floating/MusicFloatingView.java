package com.pandora.floating;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pandora.carlauncher.R;

/**
 * 音乐悬浮窗视图
 * 显示当前播放歌曲信息和控制按钮
 */
public class MusicFloatingView extends DraggableFloatingView {

    private TextView tvSongName;
    private TextView tvArtist;
    private ImageButton btnPlayPause;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private ImageButton btnClose;

    private boolean isPlaying = false;

    public MusicFloatingView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_floating_music, this, true);

        // 设置半透明背景
        setBackgroundColor(Color.parseColor("#CC000000"));

        // 初始化控件
        tvSongName = findViewById(R.id.tv_song_name);
        tvArtist = findViewById(R.id.tv_artist);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnClose = findViewById(R.id.btn_close);

        // 设置点击事件
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrev.setOnClickListener(v -> playPrev());
        btnNext.setOnClickListener(v -> playNext());
        btnClose.setOnClickListener(v -> hide());

        // 设置初始位置（右上角）
        layoutParams.x = getScreenWidth() - dpToPx(200);
        layoutParams.y = dpToPx(100);
        layoutParams.width = dpToPx(200);
        layoutParams.height = dpToPx(80);
    }

    /**
     * 更新歌曲信息
     */
    public void updateMusicInfo(String songName, String artist, boolean playing) {
        tvSongName.setText(songName != null ? songName : "未播放");
        tvArtist.setText(artist != null ? artist : "---");
        isPlaying = playing;
        updatePlayButton();
    }

    /**
     * 切换播放/暂停
     */
    private void togglePlayPause() {
        isPlaying = !isPlaying;
        updatePlayButton();
        // TODO: 发送媒体按钮广播或调用MediaController
    }

    /**
     * 更新播放按钮状态
     */
    private void updatePlayButton() {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_music_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_music_play);
        }
    }

    /**
     * 上一曲
     */
    private void playPrev() {
        // TODO: 发送上一曲控制
    }

    /**
     * 下一曲
     */
    private void playNext() {
        // TODO: 发送下一曲控制
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
