package com.xiaoxun.englishdailystudy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.xiaoxun.englishdailystudy.view.CustomGifView;
import com.xiaoxun.englishdailystudy.view.Rotate3dAnimation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class StudyActivity extends Activity {

    private static final String TAG = "StudyActivity";

    private static final String COME_ON = "come_on.mp3";

    private static final int[] backColors =
            new int[]{R.color.back_0, R.color.back_1, R.color.back_2, R.color.back_3,
                    R.color.back_4, R.color.back_5};
    private static final int[] backImgs =
            new int[]{R.drawable.back_2,R.drawable.back_1,R.drawable.back_3,R.drawable.back_5,
                    R.drawable.back_6,R.drawable.back_4};

    private int curColor = 0;

    private RelativeLayout layout;
    private RelativeLayout text_ly;
    private RelativeLayout wave_ly;

    private ImageView img;
    private TextView en_txt;
    private TextView cn_txt;
    private ImageView wave1;
    private ImageView wave2;
    private ImageView wave3;
    private ImageView center_anim;
    private Button skip;
    private Button record_btn;

    private ImageView top_star;
    private ImageView left_star;
    private ImageView right_star;
    private CustomGifView wait_view;

    private FrameLayout wait_ly;

    private RelativeLayout star_ly;

    private RelativeLayout back_ly;

    private CourseStruct course;
    private WordStruct word;
    private Bitmap word_img;

    private int isLongTouch = 0;

    private Animation mAnimationSet1, mAnimationSet2, mAnimationSet3;
    private static final int OFFSET = 800;  //每个动画的播放时间间隔
    private static final int MSG_WAVE2_ANIMATION = 2;
    private static final int MSG_WAVE3_ANIMATION = 3;
    private static final int MSG_LONG_TOUCH = 4;
    private static final int MSG_CHANGE_COLOR = 5;

    private ExtAudioRecorder recorder;

    /**
     * 录音失败的提示
     */
    ExtAudioRecorder.RecorderListener listener = new ExtAudioRecorder.RecorderListener() {
        @Override
        public void recordFailed(FailRecorder failRecorder) {
            if (failRecorder.getType() == FailRecorder.FailType.NO_PERMISSION) {
                Toast.makeText(StudyActivity.this, "录音失败，可能是没有给权限", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(StudyActivity.this, "发生了未知错误", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * 设置语音音量大小的图片
     */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    };

    private MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        WeakReference<StudyActivity> wakeref;

        public MyHandler(StudyActivity activity) {
            wakeref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (wakeref.get() == null) {
                Log.e(TAG, "activity is release.");
                return;
            }
            switch (msg.what) {
                case MSG_WAVE2_ANIMATION:
                    wakeref.get().wave2.startAnimation(wakeref.get().mAnimationSet2);
                    break;
                case MSG_WAVE3_ANIMATION:
                    wakeref.get().wave3.startAnimation(wakeref.get().mAnimationSet3);
                    break;
                case MSG_LONG_TOUCH:
                    if (wakeref.get().isLongTouch == 1) {
                        wakeref.get().isLongTouch = 2;
                        wakeref.get().setAudioOutputFile();
                        wakeref.get().recorder.prepare();
                        wakeref.get().recorder.start();
                        wakeref.get().showWaveAnimation();
                    }
                    break;
                case MSG_CHANGE_COLOR:
                    wakeref.get().curColor += 1;
                    if (wakeref.get().curColor == wakeref.get().backColors.length) {
                        wakeref.get().curColor = 0;
                    }
                    wakeref.get().back_ly.setBackgroundResource(wakeref.get().backColors[wakeref.get().curColor]);
                    sendEmptyMessageDelayed(MSG_CHANGE_COLOR, 1000);
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study);
        setData();
        initAudio();
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
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

    private void setAudioOutputFile() {
        File file = new File(Environment.getExternalStorageDirectory(),
                FilesManager.APP_PATH + FilesManager.RECORD_PATH + "/"
                        + String.valueOf(word.getId()) + FilesManager.RECORD_FILE_STRING + ".wav");
        recorder.setOutputFile(file.getAbsolutePath());
    }

    private void checkAudioStatus() {
        if (recorder == null) {
            initAudio();
        } else {
            if (recorder.getState() != ExtAudioRecorder.State.INITIALIZING) {
                recorder.reset();
            }
        }
        setAudioOutputFile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recorder == null || recorder.getState() == ExtAudioRecorder.State.ERROR) {
            initAudio();
        }
        if (mediaPlayer == null) {
            initMediePlay();
        }
    }

    private void setData() {
        course = CourseStruct.getInsance(getApplicationContext());
        word = course.getNextStudyWord();
        try {
            word_img = AssertsFileUtils.getImageFromAssetsFile(this, word.getPic_name());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {
        layout = (RelativeLayout)findViewById(R.id.layout);
//        layout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch ((event.getAction())){
//                    case MotionEvent.ACTION_DOWN:
//                        isLongTouch = 1;
//                        mHandler.sendEmptyMessageDelayed(MSG_LONG_TOUCH,1000);
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        Log.e(TAG,"ACTION_UP");
//                        if(isLongTouch == 2){
//                            isLongTouch = 0;
//                            int time = recorder.stop();
//                            if(time > 0){
//                                stopRecordAnim();
//                                evaluatingHttps();
//                            }else{
//                                String st2 = getResources().getString(R.string.The_recording_time_is_too_short);
//                                Toast.makeText(StudyActivity.this, st2, Toast.LENGTH_SHORT).show();
//                            }
//                            recorder.reset();
//                            return false;
//                        }
//                        if(img.getVisibility() == View.VISIBLE){
//                            Rotate3dAnimation rotate = new Rotate3dAnimation(0, 90, img.getWidth() / 2, img.getHeight() / 2,
//                                    0, true, Rotate3dAnimation.DIRECTION.Y);
//                            rotate.setDuration(500);
//                            rotate.setFillAfter(true);
//                            rotate.setInterpolator(new AccelerateInterpolator());
//                            rotate.setAnimationListener(new switchToText());
//                            layout.startAnimation(rotate);
//                            try {
//                                spellWordsInEn();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }else{
//                            Rotate3dAnimation rotate = new Rotate3dAnimation(360, 270, img.getWidth() / 2, img.getHeight() / 2,
//                                    0, true, Rotate3dAnimation.DIRECTION.Y);
//                            rotate.setDuration(500);
//                            rotate.setFillAfter(true);
//                            rotate.setInterpolator(new AccelerateInterpolator());
//                            rotate.setAnimationListener(new switchImg());
//                            layout.startAnimation(rotate);
//                            try {
//                                spellWordsInCn();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        isLongTouch = 0;
//                        break;
//                }
//                return true;
//            }
//        });
        img = (ImageView)findViewById(R.id.img);
        img.setImageBitmap(word_img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Rotate3dAnimation rotate = new Rotate3dAnimation(0, 90, img.getWidth() / 2, img.getHeight() / 2,
                        0, true, Rotate3dAnimation.DIRECTION.Y);
                rotate.setDuration(500);
                rotate.setFillAfter(true);
                rotate.setInterpolator(new AccelerateInterpolator());
                rotate.setAnimationListener(new switchToText());
                layout.startAnimation(rotate);
                try {
                    spellWordsInEn();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        text_ly = (RelativeLayout)findViewById(R.id.text_ly);
        text_ly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Rotate3dAnimation rotate = new Rotate3dAnimation(360, 270, img.getWidth() / 2, img.getHeight() / 2,
                        0, true, Rotate3dAnimation.DIRECTION.Y);
                rotate.setDuration(500);
                rotate.setFillAfter(true);
                rotate.setInterpolator(new AccelerateInterpolator());
                rotate.setAnimationListener(new switchImg());
                layout.startAnimation(rotate);
                try {
                    spellWordsInCn();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        en_txt = (TextView) findViewById(R.id.en_txt);
        en_txt.setText(word.getEn_text());
        en_txt.setTextColor(getResources().getColor(backColors[curColor]));
        cn_txt = (TextView) findViewById(R.id.cn_txt);
        cn_txt.setText(word.getCn_text());

        skip = (Button) findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skipThisWord();
                //startActivity(new Intent(StudyActivity.this,SentenceActivity.class));
            }
        });

        record_btn = (Button) findViewById(R.id.record_btn);
        record_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isLongTouch = 1;
                        mHandler.sendEmptyMessageDelayed(MSG_LONG_TOUCH, 1000);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.e(TAG, "ACTION_UP");
                        if (isLongTouch == 2) {
                            isLongTouch = 0;
                            int time = recorder.stop();
                            if (time > 0) {
                                evaluatingHttps();
                            } else {
                                String st2 = getResources().getString(R.string.The_recording_time_is_too_short);
                                Toast.makeText(StudyActivity.this, st2, Toast.LENGTH_SHORT).show();
                            }
			    stopRecordAnim();
                            recorder.reset();
                            return false;
                        }
                        isLongTouch = 0;
                        break;
                }
                return true;
            }
        });

        wave_ly = (RelativeLayout) findViewById(R.id.wave_ly);
        wave1 = (ImageView) findViewById(R.id.wave1);
        wave2 = (ImageView) findViewById(R.id.wave2);
        wave3 = (ImageView) findViewById(R.id.wave3);
        mAnimationSet1 = AnimationUtils.loadAnimation(this, R.anim.wave_anim);
        mAnimationSet2 = AnimationUtils.loadAnimation(this, R.anim.wave_anim);
        mAnimationSet3 = AnimationUtils.loadAnimation(this, R.anim.wave_anim);

        center_anim = (ImageView) findViewById(R.id.center_anim);
        center_anim.setBackgroundResource(R.drawable.frame_anim);

        top_star = (ImageView) findViewById(R.id.top_star);
        left_star = (ImageView) findViewById(R.id.left_star);
        right_star = (ImageView) findViewById(R.id.right_star);
        star_ly = (RelativeLayout) findViewById(R.id.star_ly);
        star_ly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        wait_ly = (FrameLayout) findViewById(R.id.wait_ly);
        wait_view = (CustomGifView) findViewById(R.id.wait_view);
        wait_view.setMovieResource(R.drawable.progress);

        back_ly = (RelativeLayout) findViewById(R.id.back_ly);
        //GradientDrawable drawable = (GradientDrawable) back_ly.getBackground();
        //drawable.setColor(getResources().getColor(backColors[curColor]));
        back_ly.setBackgroundResource(backImgs[curColor]);
        //mHandler.sendEmptyMessageDelayed(MSG_CHANGE_COLOR,1000);
    }

    class switchToText implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            img.setVisibility(View.GONE);
            text_ly.setVisibility(View.VISIBLE);
            Rotate3dAnimation rotate = new Rotate3dAnimation(270, 360, layout.getWidth() / 2, layout.getHeight() / 2,
                    0, true, Rotate3dAnimation.DIRECTION.Y);
            rotate.setDuration(300);
            rotate.setFillAfter(true);
            rotate.setInterpolator(new AccelerateInterpolator());
            layout.startAnimation(rotate);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    class switchImg implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            text_ly.setVisibility(View.GONE);
            img.setVisibility(View.VISIBLE);
            Rotate3dAnimation rotate = new Rotate3dAnimation(90, 0, layout.getWidth() / 2, layout.getHeight() / 2,
                    0, true, Rotate3dAnimation.DIRECTION.Y);
            rotate.setDuration(300);
            rotate.setFillAfter(true);
            rotate.setInterpolator(new AccelerateInterpolator());
            layout.startAnimation(rotate);
            try {
                spellWordsInEn();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    private void showWaveAnimation() {
        wave_ly.setVisibility(View.VISIBLE);
        wave1.startAnimation(mAnimationSet1);
        mHandler.sendEmptyMessageDelayed(MSG_WAVE2_ANIMATION, OFFSET);
        mHandler.sendEmptyMessageDelayed(MSG_WAVE3_ANIMATION, OFFSET * 2);
        AnimationDrawable ad = (AnimationDrawable) center_anim.getBackground();
        ad.start();
    }

    private void stopRecordAnim() {
        wave1.clearAnimation();
        wave2.clearAnimation();
        wave3.clearAnimation();
        center_anim.clearAnimation();
        wave_ly.setVisibility(View.GONE);

    }

    private MediaPlayer mediaPlayer;

    private void initMediePlay() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
    }

    private void spellWordsInEn() throws IOException {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
        AssetFileDescriptor fd =
                AssertsFileUtils.readMediaFileFromAsserts(StudyActivity.this, word.getPronounce_en());
        mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();
            }
        });
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private void playComeOn() throws IOException {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
        AssetFileDescriptor fd =
                AssertsFileUtils.readMediaFileFromAsserts(StudyActivity.this,COME_ON);
        mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();
            }
        });
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private void spellWordsInCn() throws IOException {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
        AssetFileDescriptor fd =
                AssertsFileUtils.readMediaFileFromAsserts(StudyActivity.this, word.getPronounce_cn());
        mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();
            }
        });
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private void skipThisWord() {
        boolean hasNext = false;
        int cur = course.wordList.indexOf(word);
        WordStruct skipw = course.wordList.get(cur);
        course.wordList.remove(cur);
        course.wordList.add(skipw);
        for (WordStruct item : course.wordList) {
            if (item.getId() != word.getId() && item.getScore() == -1) {
                word = item;
                hasNext = true;
                break;
            }
        }
        if (!hasNext) {
            Toast.makeText(this, getResources().getString(R.string.txt_skip_end), Toast.LENGTH_LONG).show();
        } else {
            try {
                word_img = AssertsFileUtils.getImageFromAssetsFile(this, word.getPic_name());
            } catch (IOException e) {
                e.printStackTrace();
            }
            img.setImageBitmap(word_img);
            int lastcolor = curColor;
            curColor += 1;
            if (curColor == backColors.length) {
                curColor = 0;
            }
            en_txt.setText(word.getEn_text());
            en_txt.setTextColor(getResources().getColor(backColors[curColor]));
            cn_txt.setText(word.getCn_text());
            //GradientDrawable drawable = (GradientDrawable) back_ly.getBackground();
            //drawable.setColor(getResources().getColor(backColors[curColor]));
            back_ly.setBackgroundResource(backImgs[curColor]);
        }
    }

    private void evaluatingHttps() {
        final int word_id = word.getId();
        final File file = new File(Environment.getExternalStorageDirectory(),
                FilesManager.APP_PATH + FilesManager.RECORD_PATH + "/" + String.valueOf(word_id) + FilesManager.RECORD_FILE_STRING + ".wav");
        try {
            JSONObject pl = new JSONObject();
            pl.put("eid", "AC94D60238B457FCBD6E7FC5000D2B82");
            pl.put("sn", String.valueOf(System.currentTimeMillis()));
            pl.put("dailyId", CourseStruct.getInsance(getApplicationContext()).daiylyId);
            pl.put("wordId", word_id);
            pl.put("time", 10);
            byte[] AESStr = AESUtil.encryptBytes(pl.toString().getBytes(), "A44ILFTpggKuon8M", "A44ILFTpggKuon8M");
            final String out = Base64.encodeToString(AESStr, Base64.NO_WRAP) + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            HttpsConnectionListener lis = new HttpsConnectionListener() {
                @Override
                public void onFinished(String result) {
                    if (result == null) {
                        Log.e("xxxx", "result is null or empty.");
                        wait_ly.setVisibility(View.GONE);
                        return;
                    }
                    try {
                        Log.e("xxxx", result);
                        JSONObject pl = new JSONObject(result);
                        String msg = (String) pl.get("msg");
                        if (msg.equals("success")) {
                            int score = pl.getInt("score");
                            wait_ly.setVisibility(View.GONE);
                            if (score > 0) {
                                star_ly.setVisibility(View.VISIBLE);
                                word.setScore(score);
                                shwoAnim(score);
                            } else {
                                //0 score add to wrong note
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(String cause) {
                    Log.e("xxxx", cause);
                    wait_ly.setVisibility(View.GONE);
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

    private void shwoAnim(int score) {
        setStarGone();
        if (score > 0) {
            top_star.setVisibility(View.VISIBLE);
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
            if (score == 1) {
                Animation.AnimationListener lis = new scoreAnimListener();
                anim.setAnimationListener(lis);
            }
            top_star.startAnimation(anim);
        }
        if (score > 1) {
            final Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
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
            final Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
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
                        Intent it = new Intent(StudyActivity.this, SentenceActivity.class);
                        startActivity(it);
                        finish();
                    } else {
                        star_ly.setVisibility(View.INVISIBLE);
			word = course.getNextStudyWord();
                        int finishsum = course.getFinishedWordsSize();
                        if(course.wordList.size() - finishsum == 2){
                             try {
                                    playComeOn();
                             } catch (IOException e) {
                                    e.printStackTrace();
                             }
                                }
                                if (word != null) {
                                    try {
                                        word_img = AssertsFileUtils.getImageFromAssetsFile(StudyActivity.this, word.getPic_name());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    img.setImageBitmap(word_img);
                                    curColor += 1;
                                    if (curColor == backColors.length) {
                                        curColor = 0;
                                    }
                                    en_txt.setText(word.getEn_text());
                                    en_txt.setTextColor(getResources().getColor(backColors[curColor]));
                                    cn_txt.setText(word.getCn_text());
                                    back_ly.setBackgroundResource(backImgs[curColor]);
                                    //GradientDrawable drawable = (GradientDrawable) back_ly.getBackground();
                                    //drawable.setColor(getResources().getColor(backColors[curColor]));
                                } else {
                                    //finish word study,start sentence activity

                                }
                    }
                }
            }, 500);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    private void setStarGone() {
        top_star.setVisibility(View.GONE);
        left_star.setVisibility(View.GONE);
        right_star.setVisibility(View.GONE);
    }
}
