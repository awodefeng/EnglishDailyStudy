package com.xiaoxun.englishdailystudy.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import com.xiaoxun.sdk.XiaoXunNetworkManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class CourseStruct {

    private static final int ERROR_NETWORK = 0;
    private static final int ERROR_COURSE_GET = 1;
    private static final int ERROR_ZIP_GET = 2;
    private static final int ERROR_UNZIP = 3;
    private static final int ERROR_UNKNOWN = 4;

    private static CourseStruct ins = null;

    private Context ctxt;

    public ArrayList<WordStruct> wordList;
    public ArrayList<SentenceStruct> sentenceList;

    public int curWord;
    public int daiylyId = 0;
    public String zipUrl;

    public String updateTime;

    public boolean needAnim = true;

    private updateCourseListener uListener;

    private XiaoXunNetworkManager netservice;

    public static CourseStruct getInsance(Context context) {
        if (ins == null) {
            ins = new CourseStruct(context);
        }
        return ins;
    }

    public static void clearCourse() {
        ins = null;
    }

    public WordStruct getNextStudyWord() {
        for (WordStruct item : wordList) {
            if (item.getScore() == -1) {
                return item;
            }
        }
        return null;
    }

    public CourseStruct(Context context) {
        wordList = new ArrayList<>();
        sentenceList = new ArrayList<>();
        ctxt = context;
	netservice = (XiaoXunNetworkManager)ctxt.getSystemService("xun.network.Service");
//        updateTime = readUpdateTimeSp();
//        initCourseTest();
    }

    public int getFinishedWordsSize() {
        int num = 0;
        for (WordStruct item : wordList) {
            if (item.getScore() != -1) {
                num++;
            }
        }
        return num;
    }

    public int getUnableLearnWordsSize() {
        int num = 0;
        for (WordStruct item : wordList) {
            if (item.getScore() == -2) {
                num++;
            }
        }
        return num;
    }

    public WordStruct getWordbyId(int id) {
        for (WordStruct item : wordList) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public WordStruct getWordbyEntxt(String en) {
        for (WordStruct item : wordList) {
            if (item.getEn_text().equals(en)) {
                return item;
            }
        }
        return null;
    }

    public int getFinishedSentencesSize() {
        int num = 0;
        for (SentenceStruct item : sentenceList) {
            if (item.getScore() != -1) {
                num++;
            }
        }
        return num;
    }

    public void init(updateCourseListener lis) {
        uListener = lis;
        updateTime = readUpdateTimeSp();
        if (FilesManager.getCourseFile() == null || checkUpdates()) {//true 
            //Log.e("xxxx", "need update course.");
	    needAnim = true;
	    if(!isNetworkAvailable()){
		uListener.onError(ERROR_NETWORK);
	    }else{
            	clearResourceFiles();
	    	clearLocCourseSp();
            	getCourseFromCloud();
	    }
        } else {
	    needAnim = false;
            initLocCourse();
        }
    }

    public void initCourse() {
        wordList.clear();
        sentenceList.clear();
        StringBuilder sb = new StringBuilder("");
        File courseFp = FilesManager.getCourseFile();
        if (courseFp != null && courseFp.exists()) {
            try {
                FileInputStream input = new FileInputStream(courseFp);
                byte[] temp = new byte[4096];
                int len = 0;
                while ((len = input.read(temp)) > 0) {
                    sb.append(new String(temp, 0, len));
                }
                input.close();

                String dataStr = sb.toString();
                if (dataStr.length() != 0) {
                    JSONObject obj = new JSONObject(dataStr);
                    JSONObject course = obj.getJSONObject("course");
                    daiylyId = course.getInt("dailyId");
                    zipUrl = course.getString("zip");
                    JSONArray words = course.getJSONArray("words");
                    for (int i = 0; i < words.length(); i++) {
                        JSONObject item = (JSONObject) words.get(i);
                        WordStruct word = new WordStruct();
                        word.setId((Integer) item.get("id"));
                        word.setCn_text((String) item.get("chinese"));
                        word.setEn_text((String) item.get("english"));
                        word.setPic_name((String) item.get("pic"));
                        word.setPronounce_en((String) item.get("audioEnglish"));
                        word.setPronounce_cn((String) item.get("audioChinese"));
                        word.setScore(-1);
			//word.setEn2_text((String)item.get("en2"));
                        wordList.add(word);
                    }
                    JSONObject sen = (JSONObject) course.get("sentence");
                    SentenceStruct sentence = new SentenceStruct();
                    sentence.setId((Integer) sen.get("id"));
                    sentence.setSentence_txt((String) sen.get("sentence"));
                    sentence.setPronounce((String) sen.get("file"));
                    sentence.setScore(-1);
                    sentenceList.add(sentence);
		    saveCourseToLocSp();
                    uListener.onFinished();
                }
            } catch (Exception e) {
                e.printStackTrace();
		clearResourceFiles();
	    	clearLocCourseSp();
		clearUpdateTimeSp();
		uListener.onError(ERROR_UNKNOWN);
            }
        }
    }

    public void initCourseTest() {
        try {
            String dataStr = AssertsFileUtils.readFileFromAssets(ctxt, "dataFile");
            if (dataStr != null && dataStr.length() != 0) {
                JSONObject obj = new JSONObject(dataStr);
                JSONObject course = obj.getJSONObject("course");
                daiylyId = course.getInt("dailyId");
                JSONArray words = course.getJSONArray("words");
                for (int i = 0; i < words.length(); i++) {
                    JSONObject item = (JSONObject) words.get(i);
                    WordStruct word = new WordStruct();
                    word.setId((Integer) item.get("id"));
                    word.setCn_text((String) item.get("chinese"));
                    word.setEn_text((String) item.get("english"));
                    word.setPic_name((String) item.get("pic"));
                    word.setPronounce_en((String) item.get("audioEnglish"));
                    word.setPronounce_cn((String) item.get("audioChinese"));
                    word.setScore(-1);
		    //word.setEn2_text((String)item.get("en2"));
                    wordList.add(word);
                }
                JSONObject sen = (JSONObject) course.get("sentence");
                SentenceStruct sentence = new SentenceStruct();
                sentence.setId((Integer) sen.get("id"));
                sentence.setSentence_txt((String) sen.get("sentence"));
                sentence.setPronounce((String) sen.get("file"));
                sentence.setScore(-1);
                sentenceList.add(sentence);
            }
            uListener.onFinished();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readUpdateTimeSp() {
        SharedPreferences sp = ctxt.getSharedPreferences(FilesManager.APP_SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(FilesManager.SP_COURSE_UPDATE_TIME, "");
    }

    private void writeUpdateTimeSp() {
        Calendar cal = Calendar.getInstance();
        String time = String.valueOf(cal.getTimeInMillis());
        SharedPreferences sp = ctxt.getSharedPreferences(FilesManager.APP_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(FilesManager.SP_COURSE_UPDATE_TIME, time);
        editor.commit();
    }

    private void clearUpdateTimeSp(){
	SharedPreferences sp = ctxt.getSharedPreferences(FilesManager.APP_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(FilesManager.SP_COURSE_UPDATE_TIME, "");
        editor.commit();
    }

    private String readLocCourseSp() {
        SharedPreferences sp = ctxt.getSharedPreferences(FilesManager.APP_SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(FilesManager.SP_COURSE, "");
    }

    private void writeLocCourseSp(JSONObject pl) {
        SharedPreferences sp = ctxt.getSharedPreferences(FilesManager.APP_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(FilesManager.SP_COURSE, pl.toString());
        editor.commit();
    }

    private void clearLocCourseSp(){
	SharedPreferences sp = ctxt.getSharedPreferences(FilesManager.APP_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(FilesManager.SP_COURSE, "");
        editor.commit();
    }


    public boolean checkUpdates() {
        if (daiylyId == -1) {
            return true;
        } else {
            if (updateTime == null || updateTime.equals("")) {
                return true;
            } else {
                Calendar now = Calendar.getInstance();
                Calendar last = Calendar.getInstance();
                last.setTimeInMillis(Long.valueOf(updateTime));
                if (last.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private void updateCourse(String dataStr) {
        JSONObject obj = null;
        try {
            obj = new JSONObject(dataStr);
            JSONObject course = obj.getJSONObject("course");
            daiylyId = course.getInt("dailyId");
            JSONArray words = course.getJSONArray("words");
            for (int i = 0; i < words.length(); i++) {
                JSONObject item = (JSONObject) words.get(i);
                WordStruct word = new WordStruct();
                word.setId((Integer) item.get("id"));
                word.setCn_text((String) item.get("chinese"));
                word.setEn_text((String) item.get("english"));
                word.setPic_name((String) item.get("pic"));
                word.setPronounce_en((String) item.get("audioEnglish"));
                word.setPronounce_cn((String) item.get("audioChinese"));
                word.setScore(-1);
		//word.setEn2_text((String) item.get("en2"));
                wordList.add(word);
            }
            zipUrl = (String) course.get("zip");
            JSONObject sen = (JSONObject) course.get("sentence");
            SentenceStruct sentence = new SentenceStruct();
            if (sen == null || sen.equals("")) {
                sentence.setId(111);
                sentence.setSentence_txt("good good study!");
                sentence.setPronounce("");
                sentence.setScore(-1);
            } else {
                sentence.setId((Integer) sen.get("id"));
                sentence.setSentence_txt((String) sen.get("sentence"));
                sentence.setPronounce((String) sen.get("file"));
                sentence.setScore(-1);
            }
            sentenceList.add(sentence);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String toUtf8() {
        String ret = null;
        try {
            ret = new String("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void getCourseFromCloud() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("sn", String.valueOf(System.currentTimeMillis()));
            obj.put("eid", netservice.getWatchEid());// "AC94D60238B457FCBD6E7FC5000D2B82"
            byte[] AESStr = AESUtil.encryptBytes(obj.toString().getBytes(), netservice.getAESKey(), netservice.getAESKey());//"A44ILFTpggKuon8M"
            final String out = Base64.encodeToString(AESStr, Base64.NO_WRAP) + netservice.getSID() ;//"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            //Log.e("xxxx", "AESSTRING : " + out);
	    //Log.e("xxxx","eid = " + netservice.getWatchEid() + " sid = " + netservice.getSID());
            HttpsConnectionListener lis = new HttpsConnectionListener() {
                @Override
                public void onFinished(String result) {
                    if (result == null || result.equals("")) {
                        //Log.e("xxxx", "getCourseFromCloud : result is null.");
                        return;
                    }
                    //Log.e("xxxx", "getCourseFromCloud : " + result);
                    writeUpdateTimeSp();
                    updateCourse(result);
                    saveCourseToLoc(result);
                    getCourseResourse();
                }

                @Override
                public void onError(String cause) {
                    //Log.e("xxxx", "getCourseFromCloud onError: " + cause);
		    uListener.onError(ERROR_COURSE_GET);
                }
            };
            HttpsUrlConnection con = new HttpsUrlConnection(HttpsUrlConnection.TYPE_GET_COURSE, out,
                    HttpsUrlConnection.URL_COURSE, null, lis);
            con.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveCourseToLoc(String result) {
        File fp = FilesManager.getCourseFile();
        try {
            if (fp == null) {
                fp = FilesManager.creatCourseFile();
            }
            if (fp.exists()) {
                fp.delete();
            }
            fp = FilesManager.creatCourseFile();
            FileOutputStream output = new FileOutputStream(fp);
            output.write(result.getBytes());
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearResourceFiles() {
        File fp = FilesManager.getResourceFile();
        if (fp != null) {
            FilesManager.deleteFiles(fp);
        }
    }

    private void getCourseResourse() {
        final File fp = FilesManager.getResourceFile();
        HttpsConnectionListener lis = new HttpsConnectionListener() {
            @Override
            public void onFinished(String result) {
                if (result == null || result.equals("")) {
                    //Log.e("xxxx", "getCourseResourse : result is null.");
                    return;
                }
                //download resource finished.
                 boolean ret = FilesManager.upZipFile(result, fp.getAbsolutePath());
                if(ret) {
                    initCourse();
                }else{
                    //Log.e("xxxx", "zip failed.");
                    clearResourceFiles();
                    clearLocCourseSp();
                    clearUpdateTimeSp();
                    uListener.onError(ERROR_UNZIP);
                }
            }

            @Override
            public void onError(String cause) {
                //Log.e("xxxx", "getCourseResourse onError: " + cause);
		clearResourceFiles();
	    	clearLocCourseSp();
		clearUpdateTimeSp();
		uListener.onError(ERROR_ZIP_GET);
            }
        };
        HttpsUrlConnection con = new HttpsUrlConnection(HttpsUrlConnection.TYPE_GET_ZIP_RESOOURCE, null,
                zipUrl, fp.getAbsolutePath() + "/resource.zip", lis);
        con.execute();
    }

    public interface updateCourseListener {
        void onFinished();
	void onError(int course);
    }

    public void initLocCourse() {
        wordList.clear();
        sentenceList.clear();
        String courseStr = readLocCourseSp();
        if (courseStr != null && !courseStr.equals("")) {
            try {
                JSONObject pl = new JSONObject(courseStr);
                daiylyId = pl.getInt("dailyId");
                JSONArray words = pl.getJSONArray("words");
                for (int i = 0; i < words.length(); i++) {
                    JSONObject item = (JSONObject) words.get(i);
                    WordStruct word = new WordStruct();
                    word.setId((Integer) item.get("id"));
                    word.setCn_text((String) item.get("chinese"));
                    word.setEn_text((String) item.get("english"));
                    word.setPic_name((String) item.get("pic"));
                    word.setPronounce_en((String) item.get("audioEnglish"));
                    word.setPronounce_cn((String) item.get("audioChinese"));
                    word.setScore((Integer) item.get("score"));
		    //word.setEn2_text((String) item.get("en2"));
                    wordList.add(word);
                }
                JSONObject sen = (JSONObject) pl.get("sentence");
                SentenceStruct sentence = new SentenceStruct();
                sentence.setId((Integer) sen.get("id"));
                sentence.setSentence_txt((String) sen.get("sentence"));
                sentence.setPronounce((String) sen.get("file"));
                sentence.setScore((Integer) sen.get("score"));
                sentenceList.add(sentence);
                uListener.onFinished();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveCourseToLocSp() {
        JSONObject pl = new JSONObject();
        try {
            pl.put("dailyId", daiylyId);
            JSONArray words = new JSONArray();
            for (int i = 0; i < wordList.size(); i++) {
                JSONObject obj = new JSONObject();
                obj.put("id", wordList.get(i).getId());
                obj.put("chinese", wordList.get(i).getCn_text());
                obj.put("english", wordList.get(i).getEn_text());
                obj.put("pic", wordList.get(i).getPic_name());
                obj.put("audioEnglish", wordList.get(i).getPronounce_en());
                obj.put("audioChinese", wordList.get(i).getPronounce_cn());
                obj.put("score", wordList.get(i).getScore());
                words.put(obj);
            }
            pl.put("words", words);

            //for(int i=0;i<sentenceList.size();i++)

            JSONObject sentence = new JSONObject();
            sentence.put("id", sentenceList.get(0).getId());
            sentence.put("sentence", sentenceList.get(0).getSentence_txt());
            sentence.put("file", sentenceList.get(0).getPronounce());
            sentence.put("score", sentenceList.get(0).getScore());

            pl.put("sentence", sentence);

            writeLocCourseSp(pl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) ctxt.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkinfo = manager.getActiveNetworkInfo();
        return networkinfo != null && networkinfo.isAvailable();
    }
}
