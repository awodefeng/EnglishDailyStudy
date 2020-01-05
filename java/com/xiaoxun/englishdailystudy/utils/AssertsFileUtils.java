package com.xiaoxun.englishdailystudy.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class AssertsFileUtils {

    public static final String Demodir = "DemoSrc/";
    public static final String wordsdir = "26words/";

    private AssertsFileUtils() {

    }

    /**
     * 读取asserts目录下的文件
     *
     * @param fileName eg:"updatelog.txt"
     * @return 对应文件的内容
     */
    public static String readFileFromAssets(Context context, String fileName) throws IOException, IllegalArgumentException {
        if (null == context || TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("bad arguments!");
        }

        AssetManager assetManager = context.getAssets();
        InputStream input = assetManager.open(Demodir + fileName);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = input.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }
        output.close();
        input.close();

        return output.toString();
    }

    public static AssetFileDescriptor readMediaFileFromAsserts(Context context, String fileName) throws IOException {
        if (null == context || TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("bad arguments!");
        }
        AssetManager assetManager = context.getAssets();
        AssetFileDescriptor fd = assetManager.openFd(Demodir + fileName);
        return fd;
    }

    public static AssetFileDescriptor readWordMediaFileFromAsserts(Context context, String fileName) throws IOException {
        if (null == context || TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("bad arguments!");
        }
        AssetManager assetManager = context.getAssets();
        AssetFileDescriptor fd = assetManager.openFd(Demodir + wordsdir + fileName);
        return fd;
    }

    /**
     * 列出Asserts文件夹下的所有文件
     *
     * @return asserts目录下的文件名列表
     */
    public static List<String> getAssertsFiles(Context context) throws IllegalArgumentException {
        if (null == context) {
            throw new IllegalArgumentException("bad arguments!");
        }

        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (null == files) ? null : Arrays.asList(files);
    }

    public static Bitmap getImageFromAssetsFile(Context context, String fileName) throws IOException {
        Bitmap image = null;
        AssetManager am = context.getAssets();
        InputStream is = am.open(Demodir + fileName);
        image = BitmapFactory.decodeStream(is);
        is.close();


        return image;

    }
}
