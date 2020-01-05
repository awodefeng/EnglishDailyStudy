package com.xiaoxun.englishdailystudy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class WordsLayout extends RelativeLayout {

    public static final int IMAGEVIEW_TYPE_BIG = 0;
    public static final int IMAGEVIEW_TYPE_NORMAL = 1;
    public static final int IMAGEVIEW_TYPE_SMALL = 2;
    public static final int SIZE_BIG = 32;
    public static final int SIZE_NORMAL = 24;
    public static final int SIZE_SMALL = 18;

    private static final int wordSpce = 2;
    private static final int ROWSCHANGENUM = 8;

    private Context mContext;
    private ArrayList<View> mImgList = new ArrayList<>();

    private int startfPos = 0;
    private int startsPos = 0;
    private int rowfTop = 0;
    private int rowsTop = 0;

    public WordsLayout(Context context) {
        super(context);
    }

    public WordsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WordsLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    public void addViews(ArrayList<ImageView> imgs){
        mImgList.addAll(imgs);
        int layoutWidth = getLayoutParams().width;
        int size = imgs.size();
        if(size <= ROWSCHANGENUM){
            rowfTop = 20;
            startfPos = (layoutWidth - imgs.get(0).getLayoutParams().width * size - wordSpce * (size-1)) / 2;
        }else{
            rowfTop = 10;
            rowsTop = rowfTop + imgs.get(0).getLayoutParams().height + 4;
            startfPos = (layoutWidth - imgs.get(0).getLayoutParams().width * ROWSCHANGENUM - wordSpce * (ROWSCHANGENUM - 1)) / 2;
            startsPos = (layoutWidth - imgs.get(0).getLayoutParams().width * (size - ROWSCHANGENUM) - wordSpce * (size - ROWSCHANGENUM - 1))/2;
        }
        for(int i=0;i<size;i++){
            if(i < ROWSCHANGENUM){
                addView(imgs.get(i),setViewLayoutParam(imgs.get(i),i,0));
            }else{
                addView(imgs.get(i),setViewLayoutParam(imgs.get(i),i - ROWSCHANGENUM,1));
            }
        }
    }

    private LayoutParams setViewLayoutParam(ImageView view,int index,int rows){
        LayoutParams params = new LayoutParams(view.getLayoutParams());
        if(rows == 0) {
            params.topMargin = rowfTop;
            params.leftMargin = startfPos + index * (view.getLayoutParams().width + wordSpce);
        }else{
            params.topMargin = rowsTop;
            params.leftMargin = startsPos + index * (view.getLayoutParams().width + wordSpce);
        }

        return params;
    }

    public void startAnimation(){

    }
}
