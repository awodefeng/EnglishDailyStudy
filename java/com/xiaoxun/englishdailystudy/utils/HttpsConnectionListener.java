package com.xiaoxun.englishdailystudy.utils;

public interface HttpsConnectionListener {
    void onFinished(String result);
    void onError(String cause);
}
