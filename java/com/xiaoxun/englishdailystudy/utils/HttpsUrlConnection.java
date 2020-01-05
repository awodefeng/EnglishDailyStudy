package com.xiaoxun.englishdailystudy.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class HttpsUrlConnection extends AsyncTask<String, Integer, String> {

    public static final String URL_FILE_UPLOAD = "https://english.xunkids.com/englishstudy/device/wordscore";
    public static final String URL_COURSE = "https://english.xunkids.com/englishstudy/device/coursepkg";
    public static final String URL_WRONG_WORD_UPLOAD = "https://english.xunkids.com/englishstudy/device/mistakes";

    public static final int TYPE_POST_FILE = 1;
    public static final int TYPE_GET_FILE = 2;
    public static final int TYPE_GET_COURSE = 3;
    public static final int TYPE_GET_ZIP_RESOOURCE = 4;
    public static final int TYPE_POST_WRONG_WORDS = 5;

    private int type;
    private String content;
    private String urlStr;
    private String path;
    private HttpsConnectionListener lis;

    public  HttpsUrlConnection(int type, String content, String url, String path,HttpsConnectionListener lis) {
        this.type = type;
        this.content = content;
        this.urlStr = url;
        this.path = path;
        this.lis = lis;
    }

    private String getFilenameFromPath(){
        if(path!=null && path.contains("/")) {
            String[] list = path.split("/");
            return list[list.length - 1];
        }
        return "uploadFile";
    }

    @Override
    protected String doInBackground(String... objects) {
        String result = null;
        if (type == TYPE_POST_FILE) {
            result = DoHttpsUpload();
        }else if(type == TYPE_GET_COURSE){
            result = DoHttpDownloadCourse();
        }else if(type == TYPE_GET_ZIP_RESOOURCE){
            Log.e("xxxx","TYPE_GET_ZIP_RESOOURCE");
            result = DoHttpsdownloadZip();
        }else if(type == TYPE_POST_WRONG_WORDS){
	    result = DoHttpUploadWrongWords();
        }else{
            result = DoHttpsdownload();
        }
        return result;
    }

    private String DoHttpsUpload() {
        final String NEWLINE = "\r\n"; // 换行，或者说是回车
        final String PREFIX = "--"; // 固定的前缀
        final String BOUNDARY = "xunaudio";
        HttpsURLConnection httpConn = null;
        BufferedInputStream bis = null;
        DataOutputStream dos = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            URL urlObj = new URL(urlStr);

            httpConn = (HttpsURLConnection) urlObj.openConnection();
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setUseCaches(false);
            httpConn.setRequestProperty("Connection", "Keep-Alive");
            httpConn.setRequestProperty("Accept", "*/*");
            httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            httpConn.setRequestProperty("Cache-Control", "no-cache");
            httpConn.setConnectTimeout(10000);
            httpConn.setRequestProperty("xx-text", content);

            httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            httpConn.connect();

            dos = new DataOutputStream(httpConn.getOutputStream());
            dos.writeBytes(PREFIX + BOUNDARY + NEWLINE);// 像请求体中写分割线，就是前缀+分界线+换行
            // 格式是:Content-Disposition: form-data; name="请求参数名"; filename="文件名"
            // 我这里吧请求的参数名写成了audio，是死的，实际应用要根据自己的情况修改
            // 不要忘了换行
            dos.writeBytes("Content-Disposition: form-data; " + "name=\"" + "audio" + "\"" + "; filename=\"" + getFilenameFromPath() + "\"" + NEWLINE);
            // 换行，重要！！不要忘了
            dos.writeBytes(NEWLINE);

            // 上传文件的内容
            File fp = new File(path);
            FileInputStream fileInput = new FileInputStream(fp);
            int bytesRead;
            byte[] buffer_up = new byte[2048];
            while((bytesRead = fileInput.read(buffer_up)) != -1) {
                dos.write(buffer_up, 0, bytesRead);
            }
            dos.writeBytes(NEWLINE); // 最后换行

            dos.writeBytes(PREFIX + BOUNDARY + PREFIX + NEWLINE);//最后的分割线，与前面的有点不一样是前缀+分界线+前缀+换行，最后多了一个前缀
            dos.flush();

            byte[] buffer = new byte[1024];
            if (httpConn.getResponseCode() == 200) {
                bis = new BufferedInputStream(httpConn.getInputStream());
                int c = 0;
                while ((c = bis.read(buffer)) != -1) {
                    baos.write(buffer, 0, c);
                    baos.flush();
                }
                return new String(baos.toByteArray(), "utf-8");
            }else{
                Log.e("xxxx","rc = " + httpConn.getResponseCode());
                lis.onError(String.valueOf(httpConn.getResponseCode()));
            }
        } catch (Exception e) {
            lis.onError(e.toString());
            e.printStackTrace();
        } finally {
            try {
                if (dos != null)
                    dos.close();
                if (bis != null)
                    bis.close();
                if (baos != null)
                    baos.close();
                httpConn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String DoHttpDownloadCourse(){
        HttpsURLConnection httpConn = null;
        BufferedInputStream bis = null;
        DataOutputStream dos = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        URL urlObj = null;
        try {
            urlObj = new URL(urlStr);
            httpConn = (HttpsURLConnection) urlObj.openConnection();
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setUseCaches(false);
            httpConn.setRequestProperty("Connection", "Keep-Alive");
            httpConn.setRequestProperty("Accept", "*/*");
            httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            httpConn.setRequestProperty("Cache-Control", "no-cache");
            httpConn.setConnectTimeout(10000);
            //httpConn.setRequestProperty("xx-text", content);
            httpConn.setRequestProperty("Content-Type", "application/octet-stream");
            httpConn.connect();

            dos = new DataOutputStream(httpConn.getOutputStream());
            Log.e("xxxx","DataOutputStream : " + content);
            dos.writeBytes(new String(content.getBytes("UTF-8")));
            dos.flush();

            byte[] buffer = new byte[1024];
            if (httpConn.getResponseCode() == 200) {
                bis = new BufferedInputStream(httpConn.getInputStream());
                int c = 0;
                while ((c = bis.read(buffer)) != -1) {
                    baos.write(buffer, 0, c);
                    baos.flush();
                }
                return new String(baos.toByteArray(), "utf-8");
            }else{
                Log.e("xxxx","rc = " + httpConn.getResponseCode());
                lis.onError(String.valueOf(httpConn.getResponseCode()));
            }

        } catch (Exception e) {
            lis.onError(e.toString());
            e.printStackTrace();
        } finally {
            try {
                if (dos != null)
                    dos.close();
                if (bis != null)
                    bis.close();
                if (baos != null)
                    baos.close();
                httpConn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String DoHttpsdownload() {
        HttpsURLConnection httpConn = null;
        BufferedInputStream bis = null;
        DataOutputStream dos = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            URL urlObj = new URL(urlStr);

            httpConn = (HttpsURLConnection) urlObj.openConnection();
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setUseCaches(false);
            httpConn.setRequestProperty("Connection", "Keep-Alive");
            httpConn.setRequestProperty("Accept", "*/*");
            httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            httpConn.setRequestProperty("Cache-Control", "no-cache");
            httpConn.connect();

            dos = new DataOutputStream(httpConn.getOutputStream());

            // 上传文件的内容
            dos.writeBytes(content); // 最后换行

            dos.flush();

            byte[] buffer = new byte[1024];
            int rc = httpConn.getResponseCode();
            if (httpConn.getResponseCode() == 200) {
                bis = new BufferedInputStream(httpConn.getInputStream());
                int c = 0;
                while ((c = bis.read(buffer)) != -1) {
                    baos.write(buffer, 0, c);
                    baos.flush();
                }
                return new String(baos.toByteArray(), "utf-8");
            }else{
                Log.e("xxxx","rc = " + httpConn.getResponseCode());
                //lis.onError(String.valueOf(httpConn.getResponseCode()));
            }
        } catch (Exception e) {
            //lis.onError(e.toString());
            e.printStackTrace();
        } finally {
            try {
                if (dos != null)
                    dos.close();
                if (bis != null)
                    bis.close();
                if (baos != null)
                    baos.close();
                httpConn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    private String DoHttpsdownloadZip() {
        HttpsURLConnection httpConn = null;
        BufferedInputStream bis = null;
        DataOutputStream dos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            URL urlObj = new URL(urlStr);

            httpConn = (HttpsURLConnection) urlObj.openConnection();
            httpConn.setConnectTimeout(5 * 1000);
            httpConn.setReadTimeout(5 * 1000);
            httpConn.connect();

//            dos = new DataOutputStream(httpConn.getOutputStream());
//
//            // 上传文件的内容
//            dos.writeBytes(content); // 最后换行
//
//            dos.flush();

            byte[] buffer = new byte[1024];
            int rc = httpConn.getResponseCode();
            if (rc == 200) {

                file = new File(this.path);
                if(file.exists()){
                    file.delete();
                }
                file.createNewFile();
                fos = new FileOutputStream(file);

                bis = new BufferedInputStream(httpConn.getInputStream());
                int c = 0;
                while ((c = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, c);
                }
                fos.flush();
                return file.getAbsolutePath();
            }else{
                Log.e("xxxx","rc = " + httpConn.getResponseCode());
                lis.onError(String.valueOf(httpConn.getResponseCode()));
            }
        } catch (Exception e) {
            lis.onError(e.toString());
            e.printStackTrace();
        } finally {
            try {
                if (dos != null)
                    dos.close();
                if (bis != null)
                    bis.close();
                if (fos != null)
                    fos.close();
                httpConn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String DoHttpUploadWrongWords(){
        HttpsURLConnection httpConn = null;
        BufferedInputStream bis = null;
        DataOutputStream dos = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        URL urlObj = null;
        try {
            urlObj = new URL(urlStr);
            httpConn = (HttpsURLConnection) urlObj.openConnection();
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setUseCaches(false);
            httpConn.setRequestProperty("Connection", "Keep-Alive");
            httpConn.setRequestProperty("Accept", "*/*");
            httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            httpConn.setRequestProperty("Cache-Control", "no-cache");
            httpConn.setConnectTimeout(10000);
            //httpConn.setRequestProperty("xx-text", content);
            httpConn.setRequestProperty("Content-Type", "application/octet-stream");
            httpConn.connect();

            dos = new DataOutputStream(httpConn.getOutputStream());
            Log.e("xxxx","DataOutputStream : " + content);
            dos.writeBytes(new String(content.getBytes("UTF-8")));
            dos.flush();

            byte[] buffer = new byte[1024];
            if (httpConn.getResponseCode() == 200) {
                bis = new BufferedInputStream(httpConn.getInputStream());
                int c = 0;
                while ((c = bis.read(buffer)) != -1) {
                    baos.write(buffer, 0, c);
                    baos.flush();
                }
                return path;
            }else{
                Log.e("xxxx","rc = " + httpConn.getResponseCode());
                lis.onError(path);
            }

        } catch (Exception e) {
            lis.onError(path);
            e.printStackTrace();
        } finally {
            try {
                if (dos != null)
                    dos.close();
                if (bis != null)
                    bis.close();
                if (baos != null)
                    baos.close();
                httpConn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        lis.onFinished(s);
    }

    private String byteArrayToString(byte[] array) {
        String ret = "";
        try {
            ret = new String(array, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
