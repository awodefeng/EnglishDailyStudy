package com.xiaoxun.englishdailystudy;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.app.Service;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import com.xiaoxun.englishdailystudy.utils.AESUtil;
import com.xiaoxun.englishdailystudy.utils.AssertsFileUtils;
import com.xiaoxun.englishdailystudy.utils.AuditRecorderConfiguration;
import com.xiaoxun.englishdailystudy.utils.CourseStruct;
import com.xiaoxun.englishdailystudy.utils.ExtAudioRecorder;
import com.xiaoxun.englishdailystudy.utils.FailRecorder;
import com.xiaoxun.englishdailystudy.utils.FilesManager;
import com.xiaoxun.englishdailystudy.utils.HttpsConnectionListener;
import com.xiaoxun.englishdailystudy.utils.HttpsUrlConnection;
import com.xiaoxun.englishdailystudy.utils.WordStruct;
import com.xiaoxun.englishdailystudy.view.CardGroupView;
import com.xiaoxun.englishdailystudy.view.CustomGifView;
import com.xiaoxun.englishdailystudy.view.FrameImageView;
import com.xiaoxun.englishdailystudy.view.WordsLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Random;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import com.xiaoxun.englishdailystudy.utils.ShakeListener;
import com.xiaoxun.sdk.XiaoXunNetworkManager;
import android.media.AudioManager;
import android.content.Context;
import android.os.PowerManager;

public class Study2Activity extends Activity {
    private static final String TAG = "Study2Activity";
    private static final String COME_ON = "come_on.mp3";

    private static final int MSG_WAVE2_ANIMATION = 2;
    private static final int MSG_WAVE3_ANIMATION = 3;
    private static final int MSG_LONG_TOUCH = 4;
    private static final int MSG_UNCERTAUN_GONE = 5;
    private static final int MSG_PLAY_TIPS = 6;
    private static final int MSG_WAIT_GONE = 7;
    private static final int OFFSET = 600;

    private static final String[] tips = {"tips_1.mp3","tips_2.mp3","tips_3.mp3","tips_4.mp3"};
    private static final int TIPS_INTERVAL = 12000;
    private boolean isAction = false;

    private XiaoXunNetworkManager nerservice;

    private int isLongTouch = 0;
    private boolean isDown = false;

    private CardGroupView card;
    private RelativeLayout record_btn;
    private FrameImageView btn_iv;
    private FrameLayout wait_ly;
    private CustomGifView wait_view;
    private ImageView wave1;
    private ImageView wave2;
    private RelativeLayout star_ly;
    private ImageView top_star;
    private ImageView left_star;
    private ImageView right_star;
    private FrameLayout unableLearn_ly;

    private Animation mAnimationSet1, mAnimationSet2;
    private ObjectAnimator FrameAnimator;

    private CourseStruct course;
    private WordStruct word;
    private ArrayList<WordStruct> removedWords = new ArrayList<>();

    private MediaPlayer mediaPlayer;

    private int wakeTime = 8000;

    private SensorManager sensorManager;
    private ShakeListener shakeListener;

    private ExtAudioRecorder recorder;
    ExtAudioRecorder.RecorderListener listener = new ExtAudioRecorder.RecorderListener() {
        @Override
        public void recordFailed(FailRecorder failRecorder) {
            if (failRecorder.getType() == FailRecorder.FailType.NO_PERMISSION) {
                Toast.makeText(Study2Activity.this, "录音失败，可能是没有给权限", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Study2Activity.this, "发生了未知错误", Toast.LENGTH_SHORT).show();
            }
        }
    };

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    };

    private MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        WeakReference<Study2Activity> wakeref;

        public MyHandler(Study2Activity activity) {
            wakeref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (wakeref.get() == null) {
                //Log.e(TAG, "activity is release.");
                return;
            }
            switch (msg.what) {
                case MSG_WAVE2_ANIMATION:
                    wakeref.get().wave2.startAnimation(wakeref.get().mAnimationSet2);
                    break;
                case MSG_WAVE3_ANIMATION:
                    //wakeref.get().wave3.startAnimation(wakeref.get().mAnimationSet3);
                    break;
                case MSG_LONG_TOUCH:
                    if (wakeref.get().isLongTouch == 1) {
                        wakeref.get().isLongTouch = 2;
                        wakeref.get().setAudioOutputFile();
                        wakeref.get().recorder.prepare();
                        wakeref.get().recorder.start();
                        wakeref.get().startWaveAnim();
                    }
                    break;
		case MSG_UNCERTAUN_GONE:
                    wakeref.get().unableLearn_ly.setVisibility(View.GONE);
                    break;
		case MSG_PLAY_TIPS:
                    if(!wakeref.get().isAction){
                        wakeref.get().playTips();
                    }else{
                        wakeref.get().isAction = false;
                    }
                    sendEmptyMessageDelayed(MSG_PLAY_TIPS,TIPS_INTERVAL);
                    break;
		case MSG_WAIT_GONE:
		    wakeref.get().wait_ly.setVisibility(View.GONE);
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study2);
	nerservice = (XiaoXunNetworkManager)getSystemService("xun.network.Service");
	am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        initAudio();
        initViews();
        initEvent();
        setData();
	initSensor();
    }

    @Override
    protected void onResume() {
        super.onResume();
	wakeTime = getScreenOffTime();
	//Log.e("xxxx","screenOffTime : " + wakeTime);
	setScreenOffTime(3 * wakeTime);
	//Log.e("xxxx","screenOffTime now : " + getScreenOffTime());
        if (recorder == null || recorder.getState() == ExtAudioRecorder.State.ERROR) {
            initAudio();
        }
        if (mediaPlayer == null) {
            initMediePlay();
        }
 	sensorManager.registerListener(shakeListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

	mHandler.sendEmptyMessageDelayed(MSG_PLAY_TIPS,TIPS_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
	Log.e("xxxx","onDestroy.");
        if(recorder != null){
            recorder.release();
            recorder = null;
        }
        if(mediaPlayer != null){
            stopMediaPlayer();
	    mHandler.removeMessages(MSG_PLAY_TIPS);
        }
        if(mHandler != null){
            mHandler.removeCallbacks(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(shakeListener);

	mHandler.removeMessages(MSG_PLAY_TIPS);	

//	setScreenOffTime(wakeTime);
//	Log.e("xxxx","screenOffTime now : " + getScreenOffTime());
    }

    private void setData() {
        course = CourseStruct.getInsance(getApplicationContext());
	if(course.wordList.size() == 0){
	    course.init(new CourseStruct.updateCourseListener() {
                @Override
                public void onFinished() {
                    //animCircleConfig();
		    addCards();
                }
	        @Override
                public void onError(int course) {
		    finish();
                }
           });
	}else{
	    addCards();
	}
    }

    private void initAudio() {
        AuditRecorderConfiguration configuration = new AuditRecorderConfiguration.Builder()
                .recorderListener(listener)
                .handler(handler)
                .uncompressed(true)
                .builder();

        recorder = new ExtAudioRecorder(configuration);
    }

    private void initViews(){
        card = (CardGroupView)findViewById(R.id.card);
        card.setLoadSize(3);
        card.setMargin(0.001);

        record_btn = (RelativeLayout) findViewById(R.id.record_btn_ly);
        record_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
		isAction = true;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isLongTouch = 1;
                        if(!isDown) {
                            FrameAnimator.setIntValues(1,6);
                            FrameAnimator.start();
                            isDown = true;
                        }
                        mHandler.sendEmptyMessageDelayed(MSG_LONG_TOUCH, 300);
                        break;
                    case MotionEvent.ACTION_UP:
                        //Log.e(TAG, "ACTION_UP");
                        if (isLongTouch == 2) {
                            isLongTouch = 0;
			    if(!course.isNetworkAvailable()){
				stopWaveAnim();
				if(FrameAnimator.isRunning()){
                            	    new Handler().postDelayed(new Runnable() {
                                    	@Override
                                    	public void run() {
                                            FrameAnimator.setIntValues(6,1);
                                            FrameAnimator.start();
                                        }
                                    },100);
                        	}else{
                            	    FrameAnimator.setIntValues(6,1);
                            	    FrameAnimator.start();
                        	}
				Toast.makeText(Study2Activity.this, getResources().getString(R.string.txt_network_bad), Toast.LENGTH_SHORT).show();
				break;
			    }
                            int time = recorder.stop();
                            if (time > 0) {
                                evaluatingHttps();
                            } else {
                                String st2 = getResources().getString(R.string.The_recording_time_is_too_short);
                                Toast.makeText(Study2Activity.this, st2, Toast.LENGTH_SHORT).show();
                            }                    
                            recorder.reset();
                        }
                        if(FrameAnimator.isRunning()){
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    FrameAnimator.setIntValues(6,1);
                                    FrameAnimator.start();
                                }
                            },100);
                        }else{
                            FrameAnimator.setIntValues(6,1);
                            FrameAnimator.start();
                        }
                        isLongTouch = 0;
                        isDown = false;
			stopWaveAnim();
                        break;
		    case MotionEvent.ACTION_MOVE:
			if(isLongTouch == 2){
			   return true;	
			}
			break;
                }
                return true;
            }
        });


        btn_iv = (FrameImageView) findViewById(R.id.record_btn);
        btn_iv.getDrawable().setLevel(1);
        FrameAnimator= ObjectAnimator.ofInt(btn_iv,"imageLevel",1,6);
        FrameAnimator.setRepeatCount(0);
        FrameAnimator.setInterpolator(new LinearInterpolator());
        FrameAnimator.setDuration(200);

        wave1 = (ImageView)findViewById(R.id.wave1);
        wave2 = (ImageView)findViewById(R.id.wave2);
        mAnimationSet1 = AnimationUtils.loadAnimation(this, R.anim.wave_anim);
        mAnimationSet2 = AnimationUtils.loadAnimation(this, R.anim.wave_anim);

        star_ly = (RelativeLayout)findViewById(R.id.star_ly);
        top_star = (ImageView)findViewById(R.id.top_star);
        left_star = (ImageView)findViewById(R.id.left_star);
        right_star = (ImageView)findViewById(R.id.right_star);

        wait_ly = (FrameLayout) findViewById(R.id.wait_ly);
        wait_view = (CustomGifView) findViewById(R.id.wait_view);
        wait_view.setMovieResource(R.drawable.progress);

	unableLearn_ly = (FrameLayout)findViewById(R.id.unableLearn_ly);
    }

    private void initMediePlay() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
    }

    private void initEvent() {
        card.setLoadMoreListener(new CardGroupView.LoadMore() {
            @Override
            public void load() {
                addMoreCards();
            }
        });
        card.setLeftOrRightListener(new CardGroupView.LeftOrRight() {
            @Override
            public void leftOrRight(boolean left) {
                if (left) {
                    //Toast.makeText(Study2Activity.this, "向左滑！", Toast.LENGTH_SHORT).show();
                } else {
                    //Toast.makeText(Study2Activity.this, "向右滑！", Toast.LENGTH_SHORT).show();
		   finish();
                }
            }
        });
        card.setCardUpdateListener(new CardGroupView.CardUpdate() {
            @Override
            public void update(String en) {
                word = course.getWordbyEntxt(en);
                int finishsum = course.getFinishedWordsSize();
                if(course.wordList.size() - finishsum < 3){
                    try {
                        playComeOn();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        card.setCardTurnOverListener(new CardGroupView.CardTurnOver() {
            @Override
            public void tounOver(int st) {
		isAction = true;
                if(st == 0){
                    try {
                        spellWordsInCn();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(st == 1){
                    try {
                        spellWordsInEn();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

	card.setCardThrowOutlistener(new CardGroupView.CardThrowOut() {
            @Override
            public void throwOut() {
                removedWords.add(word); 
            }
        });
    }

    private void addCards(){
        for(WordStruct item  : course.wordList){
            if(item.getScore() == -1) {
                card.addView(getCard(item));
            }
        }
        String en = card.getTopCardEnTxt();
        word = course.getWordbyEntxt(en);
    }

    private void addMoreCards(){
        //String en = card.getTopCardEnTxt();
        //for(WordStruct item  : course.wordList){
        //    if(item.getScore() == -1 && !item.getEn_text().equals(en)) {
        //        card.addView(getCard(item));
        //    }
        //}
        for(WordStruct item  : removedWords){
            card.addView(getCard(item));
        }
	removedWords.clear();
    }

    private View getCard(WordStruct word) {
        View card = LayoutInflater.from(this).inflate(R.layout.layout_card, null);
        ImageView img = (ImageView)card.findViewById(R.id.img);
        TextView en_txt = (TextView)card.findViewById(R.id.en_txt);
        TextView cn_txt = (TextView)card.findViewById(R.id.cn_txt);
        WordsLayout wl = (WordsLayout)card.findViewById(R.id.words_img);
    //    try {
    //        Bitmap bm = AssertsFileUtils.getImageFromAssetsFile(Study2Activity.this, word.getPic_name());
    //        img.setImageBitmap(bm);
    //    } catch (IOException e) {
    //        e.printStackTrace();
    //    }

 	Bitmap bm = getImgFromResourceDir(word.getPic_name());
        img.setImageBitmap(bm);

        String en = word.getEn_text();
	en = en.replace("\n","");
	en = en.replace(" ","");
        int type = 0;
        if(en.length() <= 6){
            type = WordsLayout.IMAGEVIEW_TYPE_BIG;
        }else if(en.length() > 6 && en.length() <= 8){
            type = WordsLayout.IMAGEVIEW_TYPE_NORMAL;
        }else{
            type = WordsLayout.IMAGEVIEW_TYPE_SMALL;
        }
        setWordImageViews(en,type,wl);
        en_txt.setText(word.getEn_text());
        cn_txt.setText(word.getCn_text());
        return card;
    }

    private void setWordImageViews(String en,int type,WordsLayout ly){
        int len = en.length();
        int size = 0;
        ArrayList<ImageView> list = new ArrayList<>();
        if(type == WordsLayout.IMAGEVIEW_TYPE_BIG){
            size = WordsLayout.SIZE_BIG;
        }else if(type == WordsLayout.IMAGEVIEW_TYPE_NORMAL){
            size = WordsLayout.SIZE_NORMAL;
        }else{
            size = WordsLayout.SIZE_SMALL;
        }
        for(int i=0;i<len;i++){
            ImageView img = new ImageView(this);
            img.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            String word = en.substring(i,i+1);
            char c = word.charAt(0);
            String name;
            if(Character.isUpperCase(c)){
                name = word.toLowerCase() + "_01";
            }else{
                name = word + "_02";
            }
            int resId = getResources().getIdentifier(name,"drawable",getPackageName());
            img.setImageResource(resId);
            list.add(img);
        }
        ly.addViews(list);
    }

    private void setAudioOutputFile() {
        File file = new File(Environment.getExternalStorageDirectory(),
                FilesManager.APP_PATH + FilesManager.RECORD_PATH + "/"
                        + String.valueOf(word.getId()) + FilesManager.RECORD_FILE_STRING + ".wav");
        recorder.setOutputFile(file.getAbsolutePath());
    }

    private void evaluatingHttps() {
        final int word_id = word.getId();
        final File file = new File(Environment.getExternalStorageDirectory(),
                FilesManager.APP_PATH + FilesManager.RECORD_PATH + "/" + String.valueOf(word_id) + FilesManager.RECORD_FILE_STRING + ".wav");
        try {
            JSONObject pl = new JSONObject();
            pl.put("eid", nerservice.getWatchEid());// "AC94D60238B457FCBD6E7FC5000D2B82"
            pl.put("sn", String.valueOf(System.currentTimeMillis()));
            pl.put("dailyId", CourseStruct.getInsance(getApplicationContext()).daiylyId);
            pl.put("wordId", word_id);
            pl.put("time", 10);
            byte[] AESStr = AESUtil.encryptBytes(pl.toString().getBytes(), nerservice.getAESKey(), nerservice.getAESKey()); //"A44ILFTpggKuon8M"
            final String out = Base64.encodeToString(AESStr, Base64.NO_WRAP)+  nerservice.getSID();//"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
	    //byte[] AESStr = AESUtil.encryptBytes(pl.toString().getBytes(), "A44ILFTpggKuon8M", "A44ILFTpggKuon8M");
	    //final String out = Base64.encodeToString(AESStr, Base64.NO_WRAP) + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            HttpsConnectionListener lis = new HttpsConnectionListener() {
                @Override
                public void onFinished(String result) {
                    if (result == null) {
                        //Log.e("xxxx", "result is null or empty.");
                        wait_ly.setVisibility(View.GONE);
                        return;
                    }
                    try {
                        //Log.e("xxxx", result);
                        JSONObject pl = new JSONObject(result);
                        String msg = (String) pl.get("msg");
                        if (msg.equals("success")) {
                            int score = pl.getInt("score");
                            wait_ly.setVisibility(View.GONE);
                            if (score > 0) {
                                star_ly.setVisibility(View.VISIBLE);
                                word.setScore(score);
				course.saveCourseToLocSp();
                                shwoAnim(score);
                            } else {
                                //0 score add to wrong note
				Toast.makeText(Study2Activity.this, "未识别，请再念一次", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(String cause) {
                    //Log.e("xxxx", cause);
                    mHandler.sendEmptyMessage(MSG_WAIT_GONE);
                }
            };
            HttpsUrlConnection con = new HttpsUrlConnection(HttpsUrlConnection.TYPE_POST_FILE, out,
                    HttpsUrlConnection.URL_FILE_UPLOAD, file.getAbsolutePath(), lis);
            wait_ly.setVisibility(View.VISIBLE);
            con.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void spellWordsInEn() throws IOException {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
       // AssetFileDescriptor fd =
       //         AssertsFileUtils.readMediaFileFromAsserts(Study2Activity.this, word.getPronounce_en());
       // mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());

        String path = getVoiceFromResourceFir(word.getPronounce_en());
	mediaPlayer.reset();
	if(path == null){
	   return;	
	}
        mediaPlayer.setDataSource(path);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();
            }
        });
        mediaPlayer.prepare();
        startMediaPlayer();
    }

    private void spellWordsInCn() throws IOException {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
     //   AssetFileDescriptor fd =
     //           AssertsFileUtils.readMediaFileFromAsserts(Study2Activity.this, word.getPronounce_cn());
     //   mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());

        String path = getVoiceFromResourceFir(word.getPronounce_cn());
	mediaPlayer.reset();
	if(path == null){
	   return;
	}
        mediaPlayer.setDataSource(path);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();
            }
        });
        mediaPlayer.prepare();
        startMediaPlayer();
    }

    private void startWaveAnim(){
        wave1.setVisibility(View.VISIBLE);
        //wave2.setVisibility(View.VISIBLE);

        wave1.startAnimation(mAnimationSet1);
        //mHandler.sendEmptyMessageDelayed(MSG_WAVE2_ANIMATION,OFFSET);
    }

    private void stopWaveAnim(){
        wave1.clearAnimation();
        wave2.clearAnimation();
        wave1.setVisibility(View.GONE);
        wave2.setVisibility(View.GONE);
	mHandler.removeCallbacks(null);
    }

    private void shwoAnim(int score) {
        if (score > 0) {
            top_star.setVisibility(View.VISIBLE);
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.star_anim);
	    playStarEffect(score);
            if (score == 1) {
                Animation.AnimationListener lis = new scoreAnimListener();
                anim.setAnimationListener(lis);
            }
            top_star.startAnimation(anim);
        }
        if (score > 1) {
            final Animation anim = AnimationUtils.loadAnimation(this, R.anim.star_anim);
            if (score == 2) {
                Animation.AnimationListener lis = new scoreAnimListener();
                anim.setAnimationListener(lis);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    left_star.setVisibility(View.VISIBLE);
                    left_star.startAnimation(anim);
                }
            }, 300);
        }

        if (score > 2) {
            final Animation anim = AnimationUtils.loadAnimation(this, R.anim.star_anim);
            if (score == 3) {
                Animation.AnimationListener lis = new scoreAnimListener();
                anim.setAnimationListener(lis);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    right_star.setVisibility(View.VISIBLE);
                    right_star.startAnimation(anim);
                }
            }, 600);
        }
    }

    private void playComeOn() throws IOException {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
        AssetFileDescriptor fd =
                AssertsFileUtils.readMediaFileFromAsserts(Study2Activity.this,COME_ON);
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
    }

    class scoreAnimListener implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (course.getNextStudyWord() == null) {
    
			if(course.getUnableLearnWordsSize() != 0){
                            Intent it = new Intent(Study2Activity.this, MainActivity.class);
			    it.putExtra("unable_learn",1);
                            startActivity(it);
                            finish();
                        }else {
                            Intent it = new Intent(Study2Activity.this, SentenceActivity.class);
                            startActivity(it);
                            finish();
			}
                    } else {
                        star_ly.setVisibility(View.INVISIBLE);
                        card.removeTopCard();
                        if(top_star.getVisibility() == View.VISIBLE){
                            top_star.setVisibility(View.GONE);
                        }
                        if(left_star.getVisibility() == View.VISIBLE){
                            left_star.setVisibility(View.GONE);
                        }
                        if(right_star.getVisibility() == View.VISIBLE){
                            right_star.setVisibility(View.GONE);
                        }
                    }
                }
            }, 500);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    private void initSensor(){
        sensorManager = (SensorManager) this.getSystemService(Service.SENSOR_SERVICE);
        shakeListener = new ShakeListener(new ShakeListener.Shakeable() {
            @Override
            public void onShake(Object... objs) {
                //Log.e("xxxx","onShake");
		isAction = true;
		if(unableLearn_ly.getVisibility() != View.VISIBLE) {
                    if (!shakeListener.isShaking) {
                        word.setScore(-2);
                        wrongWordsUpload(nerservice.getWatchEid(),nerservice.getAESKey(),nerservice.getSID(),word);
                    }
                }
            }
        });
    }

    public void wrongWordsUpload(String eid,String key,String sid,final WordStruct wword){
        JSONObject obj = new JSONObject();
        try {
            obj.put("sn", String.valueOf(System.currentTimeMillis()));
            obj.put("eid", eid);
            obj.put("dailyId",course.daiylyId);
            JSONArray array = new JSONArray();
            array.put(Long.valueOf(word.getId()));
            obj.put("wordIds",array);
            byte[] AESStr = AESUtil.encryptBytes(obj.toString().getBytes(), key, key);           
            final String out = Base64.encodeToString(AESStr, Base64.NO_WRAP) + sid;
            HttpsConnectionListener lis = new HttpsConnectionListener() {
                @Override
                public void onFinished(String result) {
                    mHandler.sendEmptyMessage(MSG_UNCERTAUN_GONE);
                    if (course.getNextStudyWord() != null) {
			course.saveCourseToLocSp();
                        card.removeTopCard();
                    } else {
			course.saveCourseToLocSp();
			Intent it = new Intent(Study2Activity.this, MainActivity.class);
			it.putExtra("unable_learn",1);
                        startActivity(it);
                        finish();
                    }
                }

                @Override
                public void onError(String cause) {
                    mHandler.sendEmptyMessage(MSG_UNCERTAUN_GONE);
                    wword.setScore(-1);
                }
            };
            HttpsUrlConnection con = new HttpsUrlConnection(HttpsUrlConnection.TYPE_POST_WRONG_WORDS, out,
                    HttpsUrlConnection.URL_WRONG_WORD_UPLOAD, String.valueOf(word.getId()), lis);
            con.execute();
            unableLearn_ly.setVisibility(View.VISIBLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getImgFromResourceDir(String name){
        File fp = FilesManager.getResourceFile();
        if(fp != null){
            File imgF = new File(fp,name);
            if(imgF.exists()){
                Bitmap image = null;
                image = BitmapFactory.decodeFile(imgF.getAbsolutePath());
                return image;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    public static String getVoiceFromResourceFir(String name){
        File fp = FilesManager.getResourceFile();
        if(fp != null){
            File voiF = new File(fp,name);
            if(voiF.exists()){
                return voiF.getAbsolutePath();
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    /**
     * 获得锁屏时间  毫秒
     */
    private int getScreenOffTime(){
        int screenOffTime=0;
        try{
            screenOffTime = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
        }
        catch (Exception localException){

        }
        return screenOffTime;
    }
    /**
     * 设置背光时间  毫秒
     */
    private void setScreenOffTime(int paramInt){
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EngStd");
	wakeLock.acquire(paramInt);
    }

    private void playStarEffect(int score){
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
        String name = "one_star.mp3";
        if(score == 1){
            name = "one_star.mp3";
        }else if(score == 2){
            name = "two_star.mp3";
        }else if(score == 3){
            name = "three_star.mp3";
        }
        AssetFileDescriptor fd = null;
        try {
            fd = AssertsFileUtils.readMediaFileFromAsserts(Study2Activity.this, name);
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

    private void playTips(){
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }

        Random random = new Random();
        int s = random.nextInt(4);

        AssetFileDescriptor fd = null;
        try {
            fd = AssertsFileUtils.readMediaFileFromAsserts(Study2Activity.this, tips[s]);
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
