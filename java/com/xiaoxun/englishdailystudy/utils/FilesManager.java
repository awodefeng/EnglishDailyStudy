package com.xiaoxun.englishdailystudy.utils;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FilesManager {
    public static final String APP_PATH = "/xiaoxunEng";
    public static final String RECORD_PATH = "/record";
    public static final String RECORD_FILE_STRING = "_children";

    public static final String APP_COURSE = "/course";
    public static final String COURSE_FILE_NAME = "/course.txt";

    public static final String APP_SOURCE_DIR = APP_COURSE + "/resources";

    public static final String APP_SP_NAME = "ENGLISDAILYSTUDY";
    public static final String SP_COURSE_UPDATE_TIME = "update_time";
    public static final String SP_COURSE = "course";

    public static void initDir(){
        File fp = new File(Environment.getExternalStorageDirectory(),APP_PATH);
        if(!fp.exists()){
            fp.mkdirs();
        }

        File fpr = new File(fp,RECORD_PATH);
        if(!fpr.exists()){
            fpr.mkdirs();
        }

        File fpc = new File(fp,APP_COURSE);
        if(!fpc.exists()){
            fpc.mkdirs();
        }

        File fps = new File(fp,APP_SOURCE_DIR);
        if(!fps.exists()){
            fps.mkdirs();
        }
    }

    public static File getRecordByName(String filename){
        File ret = null;
        File fp = new File(Environment.getExternalStorageDirectory(),APP_PATH + RECORD_PATH);
        if(fp.exists()){
            ret = new File(fp,filename);
        }
        return ret;
    }

    public static File getCourseFile(){
        File fp = new File(Environment.getExternalStorageDirectory(),APP_PATH + APP_COURSE + COURSE_FILE_NAME);
        if(fp.exists()){
            return fp;
        }else{
            return null;
        }
    }

    public static File creatCourseFile() throws IOException {
        File fp = new File(Environment.getExternalStorageDirectory(),APP_PATH + APP_COURSE + COURSE_FILE_NAME);
        fp.createNewFile();
        return fp;
    }

    public static File getResourceFile(){
        File fp = new File(Environment.getExternalStorageDirectory(),APP_PATH + APP_SOURCE_DIR);
        if(fp.exists()){
            return fp;
        }else{
            return null;
        }
    }

    public static void deleteFiles(File dir){
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if(f.isFile()) {
                f.delete();
            }
        }
    }

    /**
     * 含子目录的文件压缩
     *
     * @throws Exception
     */
    // 第一个参数就是需要解压的文件，第二个就是解压的目录
    public static boolean upZipFile(String zipFile, String folderPath) {
        ZipFile zfile = null;
        try {
            // 转码为GBK格式，支持中文
            zfile = new ZipFile(zipFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Enumeration zList = zfile.entries();
        ZipEntry ze = null;
        byte[] buf = new byte[1024];
        while (zList.hasMoreElements()) {
            ze = (ZipEntry) zList.nextElement();
            // 列举的压缩文件里面的各个文件，判断是否为目录
            if (ze.isDirectory()) {
                String dirstr = folderPath + ze.getName();
                dirstr.trim();
                File f = new File(dirstr);
                f.mkdir();
                continue;
            }
            OutputStream os = null;
            FileOutputStream fos = null;
            // ze.getName()会返回 script/start.script这样的，是为了返回实体的File
            File realFile = getRealFileName(folderPath, ze.getName());
            try {
                fos = new FileOutputStream(realFile);
            } catch (FileNotFoundException e) {
                Log.e("xxxx", e.getMessage());
                return false;
            }
            os = new BufferedOutputStream(fos);
            InputStream is = null;
            try {
                is = new BufferedInputStream(zfile.getInputStream(ze));
            } catch (IOException e) {
                Log.e("xxxx", e.getMessage());
                return false;
            }
            int readLen = 0;
            // 进行一些内容复制操作
            try {
                while ((readLen = is.read(buf, 0, 1024)) != -1) {
                    os.write(buf, 0, readLen);
                }
            } catch (IOException e) {
                Log.e("xxxx", e.getMessage());
                return false;
            }
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                Log.e("xxxx", e.getMessage());
                return false;
            }
        }
        try {
            zfile.close();
        } catch (IOException e) {
            Log.e("xxxx", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 给定根目录，返回一个相对路径所对应的实际文件名.
     *
     * @param baseDir
     *            指定根目录
     * @param absFileName
     *            相对路径名，来自于ZipEntry中的name
     * @return java.io.File 实际的文件
     */
    public static File getRealFileName(String baseDir, String absFileName) {
        absFileName = absFileName.replace("\\", "/");
        String[] dirs = absFileName.split("/");
        File ret = new File(baseDir);
        String substr = null;
        if (dirs.length > 1) {
            for (int i = 0; i < dirs.length - 1; i++) {
                substr = dirs[i];
                ret = new File(ret, substr);
            }

            if (!ret.exists())
                ret.mkdirs();
            substr = dirs[dirs.length - 1];
            ret = new File(ret, substr);
            return ret;
        } else {
            ret = new File(ret, absFileName);
        }
        return ret;
    }
}
