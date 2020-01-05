package com.xiaoxun.englishdailystudy.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaoxun.englishdailystudy.R;
import com.xiaoxun.englishdailystudy.utils.CourseStruct;
import com.xiaoxun.englishdailystudy.utils.DensityUtil;
import com.xiaoxun.englishdailystudy.utils.ListUtil;

import java.util.ArrayList;

/**
 * 卡片容器
 * Created by glh on 2017-06-08.
 */
public class CardGroupView extends RelativeLayout {

    private Context mContext;

    //指定剩余卡片还剩下多少时加载更多
    private int mLoadSize = 3;
    private int mRemoveSize = 0;
    //是否执行加载更多，加载更多时，卡片依次添加在后面的；而添加卡片时，卡片是依次添加在上面
    private boolean isLoadMore = false;
    //保存当前容器中的卡片
    private ArrayList<View> mCardList = new ArrayList<>();
    //加载更多监听器
    private LoadMore mLoadMore;
    //左右滑动监听器
    private LeftOrRight mLeftOrRight;
    //RemoveTop后更新card监听
    private CardUpdate mCardUpdate;
    //翻转动画结束监听器
    private CardTurnOver mCardTurnOver;
    //卡片划出监听
    private CardThrowOut mCardThrowOut;
    private double margin = 0.01;

    private RelativeLayout ly;
    private RelativeLayout img_ly;
    private LinearLayout txtly;

    public CardGroupView(Context context) {
        this(context, null);
    }

    public CardGroupView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardGroupView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    @Override
    public void addView(View card) {
        if (isLoadMore) {
            this.mCardList.add(ListUtil.getSize(mCardList), card);
        } else {
            this.mCardList.add(card);
        }
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        this.addView(card, 0, layoutParams);
        card.setOnTouchListener(onTouchListener);
        if (!isLoadMore) {
            this.setLayoutParams(card, mCardList.size());
        }
    }

    /**
     * 设置卡片LayoutParams
     *
     * @param card 卡片
     */
    private void setLayoutParams(View card, int index) {
        LayoutParams params = new LayoutParams(card.getLayoutParams());
        params.topMargin = (int) (DensityUtil.getDisplayMetrics(mContext).heightPixels * margin) + getResources().getDimensionPixelSize(
                R.dimen.card_item_margin) * index;
        params.bottomMargin = (int) (DensityUtil.getDisplayMetrics(mContext).heightPixels * margin) - getResources().getDimensionPixelSize(
                R.dimen.card_item_margin) * index;
        params.leftMargin = (int) (DensityUtil.getDisplayMetrics(mContext).widthPixels * margin) + 10;
        params.rightMargin = (int) (DensityUtil.getDisplayMetrics(mContext).widthPixels * margin);
        card.setLayoutParams(params);
        //Log.e("xxxx","top:" + params.topMargin + " bottom:" + params.topMargin + " left:" + params.leftMargin
        //        + " right:" + params.rightMargin);
    }

    /**
     * 每次移除时需要重置剩余卡片的位置
     */
    private void resetLayoutParams() {
        for (int i = 0; i < mCardList.size(); i++) {
            setLayoutParams(mCardList.get(i), i);
        }
    }

    private int mLastY = 0;
    private int mLastX = 0;
    private int mDownY = 0;
    private int mDownX = 0;
    private int mCardLeft;
    private int mCardTop;
    private int mCardRight;
    private int mCardBottom;
    private boolean mLeftOut = false;
    private boolean mRightOut = false;
    private boolean mOnTouch = true;
    private int mClickStatus = 0;

    OnTouchListener onTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mOnTouch && v.equals(mCardList.get(0))) {
                int rawY = (int) event.getRawY();
                int rawX = (int) event.getRawX();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mClickStatus = 1;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(mClickStatus == 1)
                                    mClickStatus = 0;
                            }
                        }, 300);
                        getLayout();
                        mDownY = mLastY = (int) event.getRawY();
                        mDownX = mLastX = (int) event.getRawX();
                        break;
                    case MotionEvent.ACTION_MOVE:
		    case MotionEvent.ACTION_CANCEL:
                        int offsetY = rawY - mLastY;
                        int offsetX = rawX - mLastX;
                        mCardList.get(0).layout(mCardList.get(0).getLeft() + offsetX, mCardList.get(0).getTop() + offsetY, mCardList.get(0).getRight() + offsetX, mCardList.get(0).getBottom() + offsetY);
                        mRightOut = mCardList.get(0).getLeft() > DensityUtil.getDisplayMetrics(mContext).widthPixels * 3 /4;
//                        mLeftOut = mCardList.get(0).getTop() < DensityUtil.getDisplayMetrics(mContext).heightPixels * 3 / 4;
                        mLastY = rawY;
                        mLastX = rawX;
			int outY =  Math.abs(rawY - mDownY);
			int outX =  Math.abs(rawX - mDownX);
//			mRightOut = outY > 100;
                        mLeftOut = outY > 100;
                        if (outY > 4 || outX > 4) {
                            if(mClickStatus == 1) {
                                mClickStatus = 2;
                            }
                        }
			//Log.e("xxxx","offsetY = " + offsetY);
                        break;
                    case MotionEvent.ACTION_UP:
                        //Log.e("xxxx", String.valueOf(mClickStatus));
                        if (mClickStatus == 1) {
                            if (img_ly.getVisibility() == View.VISIBLE) {
                                Rotate3dAnimation rotate = new Rotate3dAnimation(0, 90, ly.getWidth() / 2, ly.getHeight() / 2,
                                        0, true, Rotate3dAnimation.DIRECTION.Y);
                                rotate.setDuration(500);
                                rotate.setFillAfter(true);
                                rotate.setInterpolator(new AccelerateInterpolator());
                                rotate.setAnimationListener(new switchToText());
                                ly.startAnimation(rotate);
                            } else {
                                Rotate3dAnimation rotate = new Rotate3dAnimation(360, 270, ly.getWidth() / 2, ly.getHeight() / 2,
                                        0, true, Rotate3dAnimation.DIRECTION.Y);
                                rotate.setDuration(500);
                                rotate.setFillAfter(true);
                                rotate.setInterpolator(new AccelerateInterpolator());
                                rotate.setAnimationListener(new switchImg());
                                ly.startAnimation(rotate);
                            }
                            mClickStatus = 0;
                        } else {
                            change();
                        }
                        break;

                }
            }
            return true;
        }
    };

    private void getLayout() {
        mCardLeft = mCardList.get(0).getLeft();
        mCardTop = mCardList.get(0).getTop();
        mCardRight = mCardList.get(0).getRight();
        mCardBottom = mCardList.get(0).getBottom();

        ly = (RelativeLayout) mCardList.get(0).findViewById(R.id.ly);
        img_ly = (RelativeLayout) mCardList.get(0).findViewById(R.id.img_ly);
        txtly = (LinearLayout) mCardList.get(0).findViewById(R.id.text_ly);
    }

    private void change() {
        if (mLeftOut) {
           /*
            往左边滑出
             */
            out(true);
        } else if (mRightOut) {
             /*
            往右边滑出
             */
            out(false);

        } else {
            //复位
	   // Log.e("xxxx","reset....");
            reset();
        }
    }

    class CardIndex {
        int left;
        int top;
        int right;
        int bottom;

        CardIndex(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        int getLeft() {
            return left;
        }

        int getTop() {
            return top;
        }

        int getRight() {
            return right;
        }

        int getBottom() {
            return bottom;
        }
    }

    class PointEvaluator implements TypeEvaluator {

        @Override
        public Object evaluate(float fraction, Object startValue, Object endValue) {
            CardIndex startPoint = (CardIndex) startValue;
            CardIndex endPoint = (CardIndex) endValue;
            int left = (int) (startPoint.getLeft() + fraction * (endPoint.getLeft() - startPoint.getLeft()));
            int top = (int) (startPoint.getTop() + fraction * (endPoint.getTop() - startPoint.getTop()));
            int right = (int) (startPoint.getRight() + fraction * (endPoint.getRight() - startPoint.getRight()));
            int bottom = (int) (startPoint.getBottom() + fraction * (endPoint.getBottom() - startPoint.getBottom()));
            return new CardIndex(left, top, right, bottom);
        }

    }

    /**
     * 卡片复位
     */
    private void reset() {
        CardIndex oldCardIndex = new CardIndex(mCardLeft, mCardTop, mCardRight, mCardBottom);
        CardIndex newCardIndex = new CardIndex(mCardList.get(0).getLeft(), mCardList.get(0).getTop(), mCardList.get(0).getRight(), mCardList.get(0).getBottom());
        animator(newCardIndex, oldCardIndex);
    }

    /**
     * 卡片滑出
     *
     * @param left 是否向左滑出
     */
    private void out(boolean left) {
        if(mCardList.size() == 1){
            reset();
        }else {
            if (left) {
            /*
            向左滑出
             */
               // leftOut();
                TopOut();
            } else {
            /*
            向右滑出
             */
                //rightOut();
                TopOut();
            }
        }
    }

    /**
     * 上滑出
     */
    private void TopOut() {
        CardIndex oldCardIndex = new CardIndex(mCardLeft, -mCardBottom, mCardRight, 0);
        CardIndex newCardIndex = new CardIndex(mCardList.get(0).getLeft(), mCardList.get(0).getTop(), mCardList.get(0).getRight(), mCardList.get(0).getBottom());
        animator(newCardIndex, oldCardIndex);
    }

    /**
     * 左滑出
     */
    private void leftOut() {
        CardIndex oldCardIndex = new CardIndex(-mCardRight, mCardTop, 0, mCardBottom);
        CardIndex newCardIndex = new CardIndex(mCardList.get(0).getLeft(), mCardList.get(0).getTop(), mCardList.get(0).getRight(), mCardList.get(0).getBottom());
        animator(newCardIndex, oldCardIndex);
    }

    /**
     * 右滑出
     */
    private void rightOut() {
        CardIndex oldCardIndex = new CardIndex(DensityUtil.getDisplayMetrics(mContext).widthPixels, mCardTop, DensityUtil.getDisplayMetrics(mContext).widthPixels + (mCardRight - mCardLeft), mCardBottom);
        CardIndex newCardIndex = new CardIndex(mCardList.get(0).getLeft(), mCardList.get(0).getTop(), mCardList.get(0).getRight(), mCardList.get(0).getBottom());
        animator(newCardIndex, oldCardIndex);
    }

    private void animator(CardIndex newCard, CardIndex oldCard) {

        ValueAnimator animator = ValueAnimator.ofObject(new PointEvaluator(), newCard, oldCard);
        animator.setDuration(200);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mOnTouch = false;
                        CardIndex value = (CardIndex) animation.getAnimatedValue();
                        mCardList.get(0).layout(value.left, value.top, value.right, value.bottom);
                    }
                });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mRightOut || mLeftOut) {
		    mCardThrowOut.throwOut();
                    removeTopCard();
                    if (mLeftOrRight != null) {
                        mLeftOrRight.leftOrRight(mLeftOut);
                    }
                }
                mOnTouch = true;
            }
        });
        animator.start();
    }


    /**
     * 移除顶部卡片(无动画)
     */
    public void removeTopCard() {
        if (!ListUtil.isEmpty(this.mCardList)) {
            if(mCardList.size() == 1){
                //Toast.makeText(mContext, getResources().getString(R.string.txt_skip_end), Toast.LENGTH_LONG).show();
            }else {
                removeView(this.mCardList.remove(0));
		mRemoveSize++;
                TextView v = (TextView) mCardList.get(0).findViewById(R.id.en_txt);
                String txt = v.getText().toString();
		mCardUpdate.update(txt);
                if (mRemoveSize == mLoadSize || mCardList.size() < 2) {
                    if (mLoadMore != null) {
                        this.isLoadMore = true;
                        this.mLoadMore.load();
                        this.isLoadMore = false;
                        this.resetLayoutParams();
			mRemoveSize = 0;
                    }
                }
            }
        }
    }

    /**
     * 移除顶部卡片（有动画）
     *
     * @param left 向左吗
     */
    public void removeTopCard(boolean left) {
        if (this.mOnTouch) {
            this.mLeftOut = left;
            this.mRightOut = !this.mLeftOut;
            this.getLayout();
            this.out(left);
        }
    }

    /**
     * 当剩余卡片等于size时，加载更多
     */
    public void setLoadSize(int size) {
        this.mLoadSize = size;
    }

    /**
     * 距离左右上下边距的边距（屏幕宽度的百分比）
     *
     * @param margin 屏幕宽度的百分比
     */
    public void setMargin(double margin) {
        this.margin = margin;
    }

    /**
     * 加载更多监听
     *
     * @param listener {@link LoadMore}
     */
    public void setLoadMoreListener(LoadMore listener) {
        this.mLoadMore = listener;
    }

    /**
     * 卡片翻转动画结束监听
     *
     * @param listener {@link CardTurnOver}
     */
    public void setCardTurnOverListener(CardTurnOver listener){
        this.mCardTurnOver = listener;
    }

    /**
     * 卡片更新监听
     *
     * @param listener {@link CardUpdate}
     */
    public  void setCardUpdateListener(CardUpdate listener){
        this.mCardUpdate = listener;
    }
    /**
     * 左右滑动监听
     *
     * @param listener {@link LeftOrRight}
     */
    public void setLeftOrRightListener(LeftOrRight listener) {
        this.mLeftOrRight = listener;
    }

    public void setCardThrowOutlistener(CardThrowOut listener){
	this.mCardThrowOut = listener;
    }

    public interface LoadMore {
        void load();
    }

    public interface LeftOrRight {
        void leftOrRight(boolean left);
    }

    public interface CardUpdate{
        void update(String en);
    }

    public interface CardTurnOver{
        void tounOver(int st);
    }

    public interface CardThrowOut{
	void throwOut();
    }

    class switchToText implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            img_ly.setVisibility(View.GONE);
            txtly.setVisibility(View.VISIBLE);
            Rotate3dAnimation rotate = new Rotate3dAnimation(270, 360, ly.getWidth() / 2, ly.getHeight() / 2,
                    0, true, Rotate3dAnimation.DIRECTION.Y);
            rotate.setDuration(300);
            rotate.setFillAfter(true);
            rotate.setInterpolator(new AccelerateInterpolator());
            ly.startAnimation(rotate);
            mCardTurnOver.tounOver(0);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    class switchImg implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            txtly.setVisibility(View.GONE);
            img_ly.setVisibility(View.VISIBLE);
            Rotate3dAnimation rotate = new Rotate3dAnimation(90, 0, ly.getWidth() / 2, ly.getHeight() / 2,
                    0, true, Rotate3dAnimation.DIRECTION.Y);
            rotate.setDuration(300);
            rotate.setFillAfter(true);
            rotate.setInterpolator(new AccelerateInterpolator());
            ly.startAnimation(rotate);
            mCardTurnOver.tounOver(1);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    public String getTopCardEnTxt(){
        TextView v = (TextView) mCardList.get(0).findViewById(R.id.en_txt);
        String en = v.getText().toString();
        return en;
    }

    public ArrayList<String> restCards(){
        ArrayList<String> list = new ArrayList<>();
        for(int i=0;i<mCardList.size();i++){
            TextView v = (TextView) mCardList.get(i).findViewById(R.id.en_txt);
            String en = v.getText().toString();
            list.add(en);
        }
        return list;
    }

}
