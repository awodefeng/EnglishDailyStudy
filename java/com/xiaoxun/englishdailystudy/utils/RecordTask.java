package com.xiaoxun.englishdailystudy.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class RecordTask extends AsyncTask {
    private File recordFile;
    public boolean mIsRecording=false;
    public RecordTask(String filename){
        File fp = new File(Environment.getExternalStorageDirectory(),FilesManager.APP_PATH + FilesManager.RECORD_PATH);
        if(fp.exists()){
            recordFile = new File(fp,filename + FilesManager.RECORD_FILE_STRING + ".pcm");
            if(recordFile.exists()){
                recordFile.delete();
            }
            try {
                recordFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int ChechRecordStatus(Context ctxt){
        if(recordFile == null || !recordFile.exists()){
            return -1;
        }
        if(!checkPermission(ctxt)){
            return -2;
        }
        return 0;
    }

    private boolean checkPermission(Context ctxt) {
        int result = ContextCompat.checkSelfPermission(ctxt,
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(ctxt,
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(recordFile)));
            int bufferSize = AudioRecord.getMinBufferSize(16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord record = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, 16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            short[] buffer = new short[bufferSize];
            record.startRecording();
            int r = 0;
            while (mIsRecording) {
                int bufferReadResult = record
                        .read(buffer, 0, buffer.length);
                for (int i = 0; i < bufferReadResult; i++) {
                    dos.writeShort(buffer[i]);
                }
                publishProgress(new Integer(r));
                r++;
            }
            record.stop();
            dos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void startRecord(){
        mIsRecording = true;
        execute();
    }

    public void stopRecord(){
        mIsRecording = false;
    }
}
