package com.xiaoxun.englishdailystudy;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.media.AudioManager;
import android.content.Context;

import com.xiaoxun.englishdailystudy.utils.AssertsFileUtils;
import com.xiaoxun.englishdailystudy.utils.CourseStruct;
import com.xiaoxun.englishdailystudy.view.MyCircle;

import java.io.IOException;
import com.xxun.explibrary.ExpInterface;
import android.util.XiaoXunUtil;

public class EndActivity extends Activity {

    private static final String WELL_DONE = "welldone_coin.mp3";
    private static final String COIN_EFFECT = "coin_effect.mp3";

    private RelativeLayout star_mv_ly;
    private MyCircle myCircle;
    private RelativeLayout coin_ly;
    private ImageView coin;
    private ImageView bg;

    private MediaPlayer mediaPlayer;

    private String word_progress_txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);
        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        initViews();
        initMediaplay();
        if(XiaoXunUtil.XIAOXUN_CONFIG_POINTSYSTEM_SUPPORT){
            ExpInterface.getExpInterfaceInstance().setExpData(this,3,"1");
        }
    }

    private void initMediaplay(){
        mediaPlayer = new MediaPlayer();
        AssetFileDescriptor fd = null;
        try {
            fd = AssertsFileUtils.readMediaFileFromAsserts(EndActivity.this, WELL_DONE);
            mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.reset();
                }
            });
            mediaPlayer.prepare();
            startMediaPlayer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playCoinEffct(){
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
            AssetFileDescriptor fd = null;
            try {
                fd = AssertsFileUtils.readMediaFileFromAsserts(EndActivity.this, COIN_EFFECT);
                mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mediaPlayer.reset();
                    }
                });
                mediaPlayer.prepare();
                startMediaPlayer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initViews(){
        star_mv_ly = (RelativeLayout)findViewById(R.id.star_mv_ly);
	star_mv_ly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EndActivity.this,ReviewActivity.class));
                finish();
            }
        });
        myCircle = (MyCircle)findViewById(R.id.progress_circle);
        myCircle.setColor( getResources().getColor(R.color.circle_60per_under_color),
                getResources().getColor(R.color.circle_60per_color));
        CourseStruct data = CourseStruct.getInsance(getApplicationContext());
        int words_size = data.wordList.size();
        word_progress_txt = String.valueOf(words_size) + "/" + String.valueOf(words_size);
        myCircle.setAnimationListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                AlphaAnimation anim = getAlphaAnimation();
                bg.startAnimation(anim);
                TranslateAnimation animt = getTranslateAnimation();
                star_mv_ly.startAnimation(animt);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        coin_ly = (RelativeLayout)findViewById(R.id.coin_ly);
        coin_ly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {		
		coin_ly.setClickable(false);
		playCoinEffct();
                coin.clearAnimation();
                coin.setImageResource(R.drawable.coin_anims);
                AnimationDrawable drawable = (AnimationDrawable)coin.getDrawable();
                drawable.start();
                int duration = 0;
                for(int i=0;i<drawable.getNumberOfFrames();i++){
                    duration += drawable.getDuration(i);
                }
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        //此处调用第二个动画播放方法
                        Animation anim = AnimationUtils.loadAnimation(EndActivity.this,R.anim.up_out_anim);
                        Animation.AnimationListener lis = new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                //playCoinEffct();
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                Animation animp = AnimationUtils.loadAnimation(EndActivity.this,R.anim.bottom_in_anim);
                                Animation.AnimationListener lisp = new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        myCircle.startAnimation(word_progress_txt,100);
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                };
                                animp.setAnimationListener(lisp);
                                star_mv_ly.setVisibility(View.VISIBLE);
                                star_mv_ly.startAnimation(animp);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        };
                        anim.setAnimationListener(lis);
                        coin_ly.startAnimation(anim);
                        coin_ly.setVisibility(View.GONE);
                    }
                }, duration+1000);

            }
        });
        coin = (ImageView)findViewById(R.id.coin);
        Animation anim = AnimationUtils.loadAnimation(EndActivity.this, R.anim.coin_scale_anim);
        coin.startAnimation(anim);
        bg = (ImageView)findViewById(R.id.bg);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMediaPlayer();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CourseStruct.clearCourse();
        Intent home=new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    public AlphaAnimation getAlphaAnimation(){
        /**
         * @param fromAlpha 开始的透明度，取值是0.0f~1.0f，0.0f表示完全透明， 1.0f表示和原来一样
         * @param toAlpha 结束的透明度，同上
         */
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.2f);
        //设置动画持续时长
        alphaAnimation.setDuration(3000);
        //设置动画结束之后的状态是否是动画的最终状态，true，表示是保持动画结束时的最终状态
        alphaAnimation.setFillAfter(true);
        //设置动画结束之后的状态是否是动画开始时的状态，true，表示是保持动画开始时的状态
        alphaAnimation.setFillBefore(true);
        //设置动画的重复模式：反转REVERSE和重新开始RESTART
        alphaAnimation.setRepeatMode(AlphaAnimation.REVERSE);
        //设置动画播放次数
        alphaAnimation.setRepeatCount(AlphaAnimation.INFINITE);
        //开始动画
        return alphaAnimation;
    }

    public TranslateAnimation getTranslateAnimation(){
        TranslateAnimation anim = new TranslateAnimation(0,0,0,10);
        anim.setFillAfter(true);
        anim.setDuration(1000);
        anim.setRepeatMode(TranslateAnimation.REVERSE);
        anim.setRepeatCount(TranslateAnimation.INFINITE);
        return anim;
    }

    private AudioManager am;

    public void startMediaPlayer(){
        // Request audio focus for playback
        int result = am.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC, // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer.start();
            // Start playback. 
        }
    }

    public void stopMediaPlayer(){
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }else if (mediaPlayer != null){
            mediaPlayer = null;
        }
        am.abandonAudioFocus(afChangeListener);
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }

            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (mediaPlayer == null) {
                    
                } else if (!mediaPlayer.isPlaying()) {

                    mediaPlayer.start();

                }
                // Resume playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                if (mediaPlayer.isPlaying()) {

                    mediaPlayer.stop();
                }
                am.abandonAudioFocus(afChangeListener);
                // Stop playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

            } else if (focusChange == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

            }
        }
    };
}
