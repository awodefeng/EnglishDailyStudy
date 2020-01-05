package com.xiaoxun.englishdailystudy.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

public class FrameImageView extends android.support.v7.widget.AppCompatImageView {
    private int imageLevel = 0;
    private int maxLevel = 5;

    public FrameImageView(Context context) {
        super(context);
    }

    public FrameImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FrameImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageLevel(int level) {
        if (this.imageLevel == level)
            return;
        super.setImageLevel(level);
        this.imageLevel = level;
    }

    public int getImageLevel() {
        return imageLevel;
    }

    public void nextLevel() {
        setImageLevel(imageLevel++ % maxLevel);
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;

    }
}
