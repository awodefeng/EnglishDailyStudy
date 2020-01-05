package com.xiaoxun.englishdailystudy.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

/**
 * Created by torment on 16-7-10.
 */
public class DialogUtils {
    private static ProgressDialog sLoadingDialog;

    /**
     * 显示Dialog
     *
     * @param context 上下文
     * @param message 提示消息
     */
    public static void showLoadingDialog(Context context, String message) {
        try {
            if (!isLoadingDialogShowing()) {
                sLoadingDialog = ProgressDialog.show(context, "", message);
                sLoadingDialog.setCancelable(true);
            } else {
                sLoadingDialog.setMessage(message);
            }
            sLoadingDialog.show();
        } catch (Exception e) {

        }
    }

    /**
     * 关闭当前显示的Dialog
     */
    public static void closeLoadingDialog() {
        if (isLoadingDialogShowing()) {
            try {
                sLoadingDialog.dismiss();
                sLoadingDialog = null;
            } catch (Exception e) {
                sLoadingDialog = null;
            }
        }
    }

    /**
     * 判断当前Dialog是否为显示状态
     */
    public static boolean isLoadingDialogShowing() {
        return sLoadingDialog != null && sLoadingDialog.isShowing();
    }

    /**
     * 创建一个标准样式的Dialog
     *
     * @param listener
     */
    public static void createStandardDialog(Context context, String title, String message, String negativeMessage, String positiveMessage,
                                            DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setNegativeButton(negativeMessage, null);
        builder.setPositiveButton(positiveMessage, listener);
        builder.show();
    }

    public static void createStandardDialog(Context context, String message, String negativeMessage, String positiveMessage,
                                            DialogInterface.OnClickListener listener) {
        createStandardDialog(context, "", message, negativeMessage, positiveMessage, listener);
    }

    /**
     * 创建一个标准样式的Dialog
     */
    public static void createStandardDialog(Context context, String title, String message, String negativeMessage,
                                            String positiveMessage,
                                            DialogInterface.OnClickListener negativeListener,
                                            DialogInterface.OnClickListener positiveListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(negativeMessage, negativeListener);
        builder.setPositiveButton(positiveMessage, positiveListener);
        builder.show();
    }
}
