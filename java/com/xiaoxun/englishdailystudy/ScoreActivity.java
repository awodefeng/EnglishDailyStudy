package com.xiaoxun.englishdailystudy;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.xiaoxun.englishdailystudy.utils.AESUtil;
import com.xiaoxun.englishdailystudy.utils.CourseStruct;
import com.xiaoxun.englishdailystudy.view.CustomGifView;
import com.xiaoxun.englishdailystudy.utils.FilesManager;
import com.xiaoxun.englishdailystudy.utils.HttpsConnectionListener;
import com.xiaoxun.englishdailystudy.utils.HttpsUrlConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ScoreActivity extends Activity {

    private static final String URL_FILE_UPLOAD = "https://cloud.imibaby.net/englishstudy/device/wordscore";

    private ImageView top_star;
    private ImageView left_star;
    private ImageView right_star;
    private CustomGifView wait_view;

    private RelativeLayout star_ly;
    private FrameLayout wait_ly;

    private int word_id;
    private int score_type;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        score_type = getIntent().getIntExtra("type", 1);
        word_id = getIntent().getIntExtra("id", 111);
        initViews();
        if (score_type == 1) {
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
                        if(result == null){
                            Log.e("xxxx","result is null or empty.");
                            HandleEvaluatingResultError();
                            return;
                        }
                        try {
                            Log.e("xxxx",result);
                            JSONObject pl = new JSONObject(result);
                            String msg = (String)pl.get("msg");
                            if(msg.equals("success")){
                                score = pl.getInt("score");
                                wait_ly.setVisibility(View.GONE);
                                if(score > 0) {
                                    shwoAnim();
                                }else{

                                }
                                final File file = new File(Environment.getExternalStorageDirectory(),
                                        FilesManager.APP_PATH + FilesManager.RECORD_PATH + "/" + String.valueOf(word_id) +
                                                FilesManager.RECORD_FILE_STRING + ".pcm");

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(String cause) {
                        Log.e("xxxx",cause);
                    }
                };
                HttpsUrlConnection con = new HttpsUrlConnection(HttpsUrlConnection.TYPE_POST_FILE, out, URL_FILE_UPLOAD, file.getAbsolutePath(),lis);
                con.execute();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void initViews() {
        top_star = (ImageView) findViewById(R.id.top_star);
        left_star = (ImageView) findViewById(R.id.left_star);
        right_star = (ImageView) findViewById(R.id.right_star);

        wait_ly = (FrameLayout) findViewById(R.id.wait_ly);
        star_ly = (RelativeLayout)findViewById(R.id.star_ly);
        star_ly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wait_ly.getVisibility() == View.VISIBLE){
                    return;
                }
                if(score == 0){
                    HandleEvaluatingResultError();
                }else{
                    HandleEvaluatingResultSuccess();
                }
            }
        });
        wait_view = (CustomGifView) findViewById(R.id.wait_view);
        wait_view.setMovieResource(R.drawable.progress);
    }

    private void shwoAnim() {
        if (score > 0) {
            top_star.setVisibility(View.VISIBLE);
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
            top_star.startAnimation(anim);
        }
        if (score > 1) {
            final Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    left_star.setVisibility(View.VISIBLE);
                    left_star.startAnimation(anim);
                }
            }, 800);

        }

        if (score > 2) {
            final Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    right_star.setVisibility(View.VISIBLE);
                    right_star.startAnimation(anim);
                }
            }, 1600);

        }
    }

    private void HandleEvaluatingResultError() {
        wait_ly.setVisibility(View.GONE);
        Intent it = new Intent();
        it.putExtra("id", word_id);
        it.putExtra("score", score);
        setResult(0, it);
        finish();
    }

    private void HandleEvaluatingResultSuccess() {
        wait_ly.setVisibility(View.GONE);
        Intent it = new Intent();
        it.putExtra("id", word_id);
        it.putExtra("score", score);
        setResult(1, it);
        finish();
    }

}
