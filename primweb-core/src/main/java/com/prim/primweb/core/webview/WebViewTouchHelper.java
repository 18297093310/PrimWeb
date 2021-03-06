package com.prim.primweb.core.webview;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import java.util.LinkedList;

/**
 * WebView点击帮助类
 * Created by LinXin on 2017/3/31.
 */
public class WebViewTouchHelper {

    private PrimScrollView mScrollView;
    private AndroidAgentWebView mWebView;
    private X5AgentWebView mX5WebView;
    private LinkedList<SpeedItem> speedItems;
    private float mLastY;
    private VelocityTracker mVelocityTracker;
    private float mMaxVelocity;
    private boolean isDragged = false;

    public WebViewTouchHelper(PrimScrollView scrollView, AndroidAgentWebView webView) {
        this.mScrollView = scrollView;
        this.mWebView = webView;
        init(mScrollView.getContext());
    }

    public WebViewTouchHelper(PrimScrollView scrollView, X5AgentWebView x5WebView) {
        this.mScrollView = scrollView;
        this.mX5WebView = x5WebView;
        init(mScrollView.getContext());
    }

    private void init(Context context) {
        speedItems = new LinkedList<>();
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mMaxVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    /**
     * 计算webView的滑动速度
     *
     * @param deltaY
     * @param scrollY
     * @param scrollRangeY
     */
    public void overScrollBy(int deltaY, int scrollY, int scrollRangeY, boolean isTouched) {
        if (speedItems.size() >= 10) {
            speedItems.removeFirst();
        }
        if (deltaY + scrollY < scrollRangeY) {
            speedItems.add(new SpeedItem(deltaY, SystemClock.uptimeMillis()));
        } else if (!speedItems.isEmpty()) {
            int totalDeltaY = 0;
            for (SpeedItem speedItem : speedItems) {
                totalDeltaY += speedItem.deltaY;
            }
            int totalTime = ((int) (speedItems.getLast().time - speedItems.getFirst().time));
            speedItems.clear();
            if (totalTime > 0 && totalDeltaY != 0) {
                int speed = totalDeltaY / totalTime * 1000;
                boolean webViewCanScrollBottom = canScrollVertically(PrimScrollView.DIRECT_BOTTOM);
                if (!webViewCanScrollBottom && !isTouched) {//必须是在非点击状态下才触发scrollview的fling事件，否则会造成scrollview和webview同时滑动的问题
                    mScrollView.fling(speed);
                }
            }
        }
    }

    /**
     * 处理点击事件
     *
     * @param ev
     * @return
     */
    public boolean onTouchEvent(MotionEvent ev) {
        acquireVelocityTracker(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float nowY = ev.getRawY();
                float deltaY = nowY - mLastY;
                int dy = mScrollView.adjustScrollY((int) -deltaY);
                mLastY = nowY;
                if ((!canScrollVertically(PrimScrollView.DIRECT_BOTTOM) && dy > 0)
                        || (mScrollView.canScrollVertically(PrimScrollView.DIRECT_BOTTOM) && dy < 0)) {
                    mScrollView.customScrollBy(dy);
                    isDragged = true;
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!canScrollVertically(PrimScrollView.DIRECT_BOTTOM) && isDragged) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                    final float curVelocity = mVelocityTracker.getYVelocity(0);
                    mScrollView.fling(-(int) curVelocity);
                }
                mLastY = 0;
                isDragged = false;
                releaseVelocityTracker();
                break;
            case MotionEvent.ACTION_CANCEL:
                releaseVelocityTracker();
                break;
        }
        return true;
    }

    private boolean canScrollVertically(int direct) {
        if (mWebView != null) {
            return mWebView.canScrollVertically(direct);
        }
        return mX5WebView.canScrollVertically(direct);
    }


    private void acquireVelocityTracker(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker == null)
            return;
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    private class SpeedItem {
        int deltaY;
        long time;

        public SpeedItem(int arg1, long arg2) {
            this.deltaY = arg1;
            this.time = arg2;
        }

        @Override
        public String toString() {
            return "SpeedItem{" +
                    "deltaY=" + deltaY +
                    ", time=" + time +
                    '}';
        }
    }
}
