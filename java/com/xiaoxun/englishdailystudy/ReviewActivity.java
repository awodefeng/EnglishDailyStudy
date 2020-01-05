package com.xiaoxun.englishdailystudy;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.provider.Settings;

import com.xiaoxun.englishdailystudy.R;import android.util.Log;
import com.xiaoxun.englishdailystudy.utils.CourseStruct;
import com.xiaoxun.englishdailystudy.utils.FilesManager;
import com.xiaoxun.englishdailystudy.utils.WordStruct;
import com.xiaoxun.englishdailystudy.view.CardGroupView;
import com.xiaoxun.englishdailystudy.view.WordsLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import android.media.AudioManager;
import android.content.Context;
import android.os.PowerManager;

public class ReviewActivity extends Activity {
    private static final String TAG = "ReviewActivity";

    private MediaPlayer mediaPlayer;

    private CardGroupView card;

    private CourseStruct course;
    private WordStruct word;
    private ArrayList<WordStruct> removedWords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
	am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        initViews();
        initEvent();
        setData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer == null) {
            initMediePlay();
        }
	int wakeTime = getScreenOffTime();
	//Log.e("xxxx","screenOffTime : " + wakeTime);
	setScreenOffTime(3 * wakeTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMediaPlayer();
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

    private void initViews(){
        card = (CardGroupView)findViewById(R.id.card);
        card.setLoadSize(3);
        card.setMargin(0.001);
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

                }
            }
        });
        card.setCardTurnOverListener(new CardGroupView.CardTurnOver() {
            @Override
            public void tounOver(int st) {
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
	//Log.e("xxxx","course.wordList size : " + course.wordList.size());
        for(WordStruct item  : course.wordList){
            card.addView(getCard(item));
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
//        try {
//            Bitmap bm = AssertsFileUtils.getImageFromAssetsFile(Study2Activity.this, word.getPic_name());
//            img.setImageBitmap(bm);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

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

    private void spellWordsInEn() throws IOException {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
//        AssetFileDescriptor fd =
//                AssertsFileUtils.readMediaFileFromAsserts(Study2Activity.this, word.getPronounce_en());
//        mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());

        String path = getVoiceFromResourceFir(word.getPronounce_en());
	mediaPlayer.reset();
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
//        AssetFileDescriptor fd =
//                AssertsFileUtils.readMediaFileFromAsserts(Study2Activity.this, word.getPronounce_cn());
//        mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());

        String path = getVoiceFromResourceFir(word.getPronounce_cn());
	mediaPlayer.reset();        
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
