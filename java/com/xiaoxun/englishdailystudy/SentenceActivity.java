package com.xiaoxun.englishdailystudy;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.xiaoxun.englishdailystudy.utils.AssertsFileUtils;
import com.xiaoxun.englishdailystudy.utils.CourseStruct;
import com.xiaoxun.englishdailystudy.utils.SentenceStruct;

import java.io.IOException;
import android.media.AudioManager;
import android.content.Context;

public class SentenceActivity extends Activity {

    private CourseStruct course;
    private SentenceStruct sentenceStruct;


    private ScrollView sentence_ly;
    private Button skip;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentence);
	am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        course = CourseStruct.getInsance(getApplicationContext());
        TextView sentence_txt = (TextView)findViewById(R.id.sentence_txt);
        sentenceStruct = course.sentenceList.get(0);
        sentence_txt.setText(formatSentence(sentenceStruct.getSentence_txt()));

        sentence_ly = (ScrollView)findViewById(R.id.sentence_ly);


        skip = (Button)findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMediaPlayer();
                startActivity(new Intent(SentenceActivity.this,EndActivity.class));
		finish();
            }
        });
        initMediaplay();
    }

    private void initMediaplay(){
        mediaPlayer = new MediaPlayer();
        AssetFileDescriptor fd = null;
        try {
            //fd = AssertsFileUtils.readMediaFileFromAsserts(SentenceActivity.this,sentenceStruct.getPronounce());
            //mediaPlayer.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            String path = Study2Activity.getVoiceFromResourceFir(sentenceStruct.getPronounce());
	    mediaPlayer.setDataSource(path);
	    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.reset();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            skip.setText(getResources().getString(R.string.txt_skipto_finish));
                        }
                    },1000);
                }
            });
            mediaPlayer.prepare();
            startMediaPlayer();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private String formatSentence(String sentence){
        StringBuffer sb = new StringBuffer();
        String[] s = sentence.split("_");
        sb.append(s[0]);
        sb.append("\n");
        sb.append(s[1]);
        return sb.toString();
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
