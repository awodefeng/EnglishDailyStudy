package com.xiaoxun.englishdailystudy.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import com.xiaoxun.englishdailystudy.R;

public class MyCircle extends View{
    /**
     * 画笔对象的引用
     */
    private Paint[] paints;
    RectF oval;
    /**
     * 圆环的颜色
     */
    private int roundColor;
    /**
     * 圆环的宽度
     */
    private float roundWidth;
    /**
     * 圆环进度的颜色
     */
    private int roundProgressColor;
    /**
     * 移动
     */
    Scroller scroller;

    protected float progress;
    protected String progressText = "";

    private Animator.AnimatorListener listener;

    public MyCircle(Context context) {
        this(context, null);
    }

    public MyCircle(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyCircle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        paints = new Paint[4];
        for (int i = 0; i < paints.length; i++) {
            paints[i] = new Paint();
        }
        oval = new RectF();

        TypedArray mTypedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.myRoundProgress, defStyleAttr, 0);

        roundColor = mTypedArray.getColor(R.styleable.myRoundProgress_myRoundColor,
                Color.GRAY);
        roundWidth = mTypedArray.getDimension(R.styleable.myRoundProgress_myRoundWidth,
                8);
        roundProgressColor = mTypedArray.getColor(
                R.styleable.myRoundProgress_myRoundProgressColor, Color.RED);
        mTypedArray.recycle();

//		AccelerateInterpolator localAccelerateInterpolator = new AccelerateInterpolator();
//		this.scroller = new Scroller(context, localAccelerateInterpolator);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float centre = getWidth() / 2; // 获取圆心的x坐标
        float radius = (centre - roundWidth / 2); // 圆环的半径

        paints[0].setColor(roundColor); // 设置圆环的颜色
        paints[0].setStyle(Paint.Style.STROKE); // 设置空心
        paints[0].setStrokeWidth(roundWidth); // 设置圆环的宽度
        paints[0].setAntiAlias(true); // 消除锯齿
        paints[0].setStrokeCap(Paint.Cap.ROUND);// 圆角

        canvas.drawCircle(centre, centre, radius, paints[0]); // 画出圆环

        paints[0].setColor(roundProgressColor);
        // 用于定义的圆弧的形状和大小的界限.指定圆弧的外轮廓矩形区域
        // 椭圆的上下左右四个点(View 左上角为0)
        oval.set(centre - radius, centre - radius, centre + radius, centre
                + radius);

        //画圆弧
        canvas.drawArc(oval, -90, progress, false, paints[0]);

        Log.e("xxxx","ondraw progress : " + progress);

        /**
         * 画进度百分比的text
         */
        paints[0].setStrokeWidth(0);
        paints[0].setColor(roundProgressColor);
        paints[0].setTextSize(getResources().getDimension(R.dimen.circle_progress_num_txt_size));
        paints[0].setTypeface(Typeface.DEFAULT_BOLD); // 设置字体
        float textWidth = paints[0].measureText(progressText);
        canvas.drawText(progressText, centre - textWidth / 2,
                centre + 40 / 2, paints[0]); // 画出进度百分比

//        paints[1].setStrokeWidth(0);
//        paints[1].setColor(getResources().getColor(R.color.text_color_2));
//        paints[1].setTextSize(getResources().getDimension(R.dimen.circle_progress_txt_size));
//        paints[1].setTypeface(Typeface.DEFAULT_BOLD); // 设置字体
//        float textWidth1 = paints[1].measureText(getResources().getString(R.string.txt_progress));
//        canvas.drawText(getResources().getString(R.string.txt_progress), centre - textWidth1 / 2,
//                centre + 40 / 2 + 40, paints[1]);
    }

    public static final String PROGRESS_PROPERTY = "progress";

    public float getProgress() {
	//Log.e("xxxx","getProgress progress.");
        return progress;
    }

    public String getProgressText(){
 	return progressText;
    }

    public void setProgress(float progress) {
        this.progress = progress * 360 / 100;
        //Log.e("xxxx","setProgress progress : " + progress);
        invalidate();// UI thread
    }

    public void startAnimation(String progressText, float progress){
        this.progressText = progressText;
        this.progress = progress;

        AnimatorSet animation = new AnimatorSet();

        ObjectAnimator progressAnimation = ObjectAnimator.ofFloat(this,"progress", 0f, progress);
        progressAnimation.setDuration(1000);// 动画执行时间

        progressAnimation.setInterpolator(new DecelerateInterpolator());

        //animation.playTogether(progressAnimation);//动画同时执行,可以做多个动画
	animation.play(progressAnimation);
        animation.addListener(listener);
        animation.start();
    }

    public void setAnimationListener(Animator.AnimatorListener lis){
        listener = lis;
    }

    public void setColor(int roundColor,int roundProgressColor){
        this.roundColor = roundColor;
        this.roundProgressColor = roundProgressColor;
    }

    public void setProgressText(String text) {
        this.progressText = text;
        invalidate();// UI thread
    }
}
