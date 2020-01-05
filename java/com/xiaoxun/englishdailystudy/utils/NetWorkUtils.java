package com.xiaoxun.englishdailystudy.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * Created by huangyouyang on 2018/1/18.
 */

public class NetWorkUtils {

    public static final int NETWORKTYPE_WIFI = 0;
    public static final int NETWORKTYPE_MOBILE = 1;
    public static final int NETWORKTYPE_MOBILE_2G = 2;
    public static final int NETWORKTYPE_MOBILE_3G = 3;
    public static final int NETWORKTYPE_MOBILE_4G = 4;
    public static final int NETWORKTYPE_OTHER = 10;
    public static final int NETWORKTYPE_INVALID = -1;

    /**
     * @param context
     * @return 0 wifi ;1 mobile; 2 other; -1 invalid
     */
    public static int getConnectionType(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return NETWORKTYPE_INVALID;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String type = networkInfo.getTypeName();
            if (type.equalsIgnoreCase("WIFI")) {
                return NETWORKTYPE_WIFI;
            } else if (type.equalsIgnoreCase("MOBILE")) {
                return getMobileType(context);
            } else {
                return NETWORKTYPE_OTHER;
            }
        } else {
            return NETWORKTYPE_INVALID;
        }
    }

    private static int getMobileType(Context context) {

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null)
            return NETWORKTYPE_MOBILE;

        switch (telephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORKTYPE_MOBILE_2G;

            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORKTYPE_MOBILE_3G;

            case TelephonyManager.NETWORK_TYPE_LTE:
                return NETWORKTYPE_MOBILE_4G;

            default:
                return NETWORKTYPE_MOBILE;
        }
    }
}
