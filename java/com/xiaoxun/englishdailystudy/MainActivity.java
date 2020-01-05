package com.xiaoxun.englishdailystudy;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation; import android.provider.Settings;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.view.MotionEvent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;

import com.xiaoxun.statistics.XiaoXunStatisticsManager;
import com.xiaoxun.englishdailystudy.utils.AssertsFileUtils;
import com.xiaoxun.englishdailystudy.utils.CourseStruct;
import com.xiaoxun.englishdailystudy.utils.DialogUtils;
import com.xiaoxun.englishdailystudy.utils.FilesManager;
import com.xiaoxun.englishdailystudy.view.MyCircle;
import com.xiaoxun.englishdailystudy.utils.NetWorkUtils;
import java.io.IOException;
import java.lang.ref.WeakReference;
import android.util.Log;
import android.app.AlertDialog;
import android.media.AudioManager;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private static final String START_STUDY = "start_study.mp3";
    private static final String LOAD_COURSE = "course_load.mp3";
    private static final String FINISH_STUDY = "study_finish.mp3";
    private static final String ACTION_NET_SWITCH_SUCC = "com.xiaoxun.sdk.action.SWITCH_SUCC";

    private static final int MSG_ANIMATE_MEDIAPLAY = 0;
    private static final int MSG_ANIMATE_DOWNLOADPROGRESS = 1;
    private static final int MSG_NETWORK_TIMEOUT = 2;

    private static final int TIME_OUT = 30*1000;

    private RelativeLayout entry_ly;
    private RelativeLayout progress_ly;
    private MyCircle progress_circle;
    private ImageView bg;
    private RelativeLayout star_mv_ly;

    private String word_progress_txt = "";
    private float word_progress = 0f;

    private MediaPlayer mediaPlayer;

    private boolean isFinish = false;

    private int unablelearn = 0;

    private boolean needWait = true;

    private CourseStruct data;

    private boolean isDown = false;
    private int mLastX = 0;
    private int mLastY = 0;

    private int download_progress = 0;

    private Handler MyHandler = new MyHandler(this);

    private static class MyHandler extends Handler{
        WeakReference<MainActivity> wakeref;
        public MyHandler(MainActivity activity) {
            wakeref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (wakeref.get() == null) {
                Log.e(TAG, "activity is release.");
                return;
            }
            switch (msg.what){
                case MSG_ANIMATE_MEDIAPLAY:
                    //wakeref.get().entry_ly.setVisibility(View.GONE);
                    //wakeref.get().progress_ly.setVisibility(View.VISIBLE);
		    wakeref.get().progress_circle.startAnimation(wakeref.get().word_progress_txt,wakeref.get().word_progress);
		    if(!wakeref.get().isFinish){
			wakeref.get().playMediaplay(START_STUDY);
		    }                   
                    break;
		case MSG_ANIMATE_DOWNLOADPROGRESS:
		    if(wakeref.get().download_progress < 100){
			wakeref.get().download_progress += 5;
			wakeref.get().progress_circle.setProgressText(wakeref.get().download_progress + "%");
			sendEmptyMessageDelayed(MSG_ANIMATE_DOWNLOADPROGRESS,20);
		    }else{
			wakeref.get().animCircleConfig();
		    }
		    break;
		case MSG_NETWORK_TIMEOUT:
		    if(wakeref.get().isFinishing() && wakeref.get().needWait){
		    	wakeref.get().showDialog();
		    }
		    break;
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent){
	    String action = intent.getAction();
	    if (action.equals(ACTION_NET_SWITCH_SUCC)) {
		needWait = false;	
		initData();
	    }
	}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
	unablelearn = getIntent().getIntExtra("unable_learn",0);
        initViews();
        initFiles();
	Intent intent = new Intent("com.xiaoxun.xxun.story.finish");
	intent.setPackage("com.xxun.watch.storydownloadservice");
	sendBroadcast(intent);
        mediaPlayer = new MediaPlayer();
	initBroadcastReceiver();

	XiaoXunStatisticsManager statisticsManager = (XiaoXunStatisticsManager) getSystemService("xun.statistics.service");
	statisticsManager.stats(XiaoXunStatisticsManager.STATS_STORY);

	am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }


    private void playMediaplay(String name){
	if(unablelearn == 1){
	    return;
	}
        if(mediaPlayer != null && !mediaPlayer.isPlaying()) {
            AssetFileDescriptor fd = null;
            try {
                fd = AssertsFileUtils.readMediaFileFromAsserts(MainActivity.this, name);
		mediaPlayer.reset();
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
        entry_ly = (RelativeLayout)findViewById(R.id.entry_ly);
        progress_ly = (RelativeLayout)findViewById(R.id.progress_ly);

        progress_circle = (MyCircle)findViewById(R.id.progress_circle);
        progress_circle.setColor( getResources().getColor(R.color.circle_60per_under_color),
                getResources().getColor(R.color.circle_60per_color));
	progress_circle.setProgress(0f);
        progress_circle.setAnimationListener(new Animator.AnimatorListener() {
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
	progress_circle.setProgressText("0/0");

        bg = (ImageView)findViewById(R.id.bg);
        star_mv_ly = (RelativeLayout)findViewById(R.id.star_mv_ly);
        star_mv_ly.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
		int rawx = (int)event.getRawX();
		int rawy = (int)event.getRawY();
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
			mLastX = (int)event.getRawX();
			mLastY = (int)event.getRawY();
                        if(!isDown){
                            isDown = true;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
			int offx = rawx - mLastX;
			int offy = rawy - mLastY;
			if(Math.abs(offx) > 5 || Math.abs(offy) > 5){
                        	isDown = false;
			}
                        break;
                    case MotionEvent.ACTION_UP:
                        if(isDown){
                            if(mediaPlayer != null && mediaPlayer.isPlaying()){
                                mediaPlayer.stop();
                            }
			    if(!progress_circle.getProgressText().equals("0/0")){
			    	if(!isFinish){
                            		startActivity(new Intent(MainActivity.this,Study2Activity.class));
			    		//startActivity(new Intent(MainActivity.this,EndActivity.class));
			    	}else{
					startActivity(new Intent(MainActivity.this,ReviewActivity.class));
					//startActivity(new Intent(MainActivity.this,EndActivity.class));
				}
			    }else{
				playMediaplay(LOAD_COURSE);
			    }
                        }
			isDown = false;
                        break;
                }
                return true;
            }
        });
    }

    private void initFiles(){
        FilesManager.initDir();
    }

    private void initData(){
        //need check the course's update
        data = CourseStruct.getInsance(getApplicationContext());
        data.init(new CourseStruct.updateCourseListener() {
            @Override
            public void onFinished() {
                //animCircleConfig();
		if(data.needAnim){
		    MyHandler.sendEmptyMessage(MSG_ANIMATE_DOWNLOADPROGRESS);
		}else{
		    animCircleConfig();	
		}
            }
	    @Override
            public void onError(int cause) {
		Log.e(TAG, "onError cause:" + cause);
		if(cause == 0){
		    MyHandler.sendEmptyMessageDelayed(MSG_NETWORK_TIMEOUT,TIME_OUT);
		}else{
		    needWait = true;
		    MyHandler.sendEmptyMessageDelayed(MSG_NETWORK_TIMEOUT,60);
		}
            }
        });
    }


    private void animCircleConfig(){
        int words_size = data.wordList.size();
        int words_finish_size = data.getFinishedWordsSize();
        if(words_finish_size == words_size){
            isFinish = true;
	    playMediaplay(FINISH_STUDY);
        }
        words_finish_size = words_finish_size - data.getUnableLearnWordsSize();
        word_progress = (float)words_finish_size / (float)words_size;
        word_progress = word_progress * 100;
        word_progress_txt = String.valueOf(words_finish_size) + "/" + String.valueOf(words_size);
        if(word_progress != 0f && word_progress < 100f){
            MyHandler.sendEmptyMessageDelayed(MSG_ANIMATE_MEDIAPLAY,500);
        }else if(word_progress == 0){
            MyHandler.sendEmptyMessageDelayed(MSG_ANIMATE_MEDIAPLAY, 2000);
        }else if(word_progress >= 100f){
            star_mv_ly.setClickable(false);
            MyHandler.sendEmptyMessageDelayed(MSG_ANIMATE_MEDIAPLAY,500);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//	int networkType = NetWorkUtils.getConnectionType(this);
//        Log.e(TAG, "Network Info: " + networkType);
//        if (networkType == NetWorkUtils.NETWORKTYPE_INVALID ||
//                networkType == NetWorkUtils.NETWORKTYPE_MOBILE_2G) {
//            MyHandler.sendEmptyMessage(MSG_NETWORK_TIMEOUT);
//            return;
//        }
        initData();
        //animCircleConfig();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMediaPlayer();
	if(MyHandler != null){
	    MyHandler.removeCallbacksAndMessages(null);	
	}
	if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
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

    private void showDialog(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.txt_network_error_title));
        builder.setMessage(getResources().getString(R.string.txt_network_error));
        builder.setPositiveButton(getResources().getString(R.string.txt_network_btn_confirm),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        AlertDialog dialog=builder.create();
        dialog.show();
    }

    private void initBroadcastReceiver(){
	IntentFilter intentFilter = new IntentFilter();
	intentFilter.addAction(ACTION_NET_SWITCH_SUCC);
	registerReceiver(mBroadcastReceiver, intentFilter);
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
