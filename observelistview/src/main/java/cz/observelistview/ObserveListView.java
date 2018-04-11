package cz.observelistview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * Created by haozhou on 2016/3/29.
 */
public class ObserveListView extends ListView implements AbsListView.OnScrollListener{
    private final static String TAG = "ObservableListView";

    public enum ScrollState {
        UP, DOWN, STOP
    }

    private OnScrollListener outOnScrollListener;

    private OnObserverScrollListener onObserverScrollListener;

    private SparseIntArray childrenHeights;

    private int preFirstVisiblePos;

    private int preSkippedHeight;

    private int preScrollDistance;

    private int scrollDistance;

    private boolean handleFling;

    private boolean disallowParentIntercept;

    private boolean intercepted;

    private boolean reachTop;

    private boolean reachBottom;

    private MotionEvent preMoveEvent;

    private ScrollState scrollState;

    private GestureDetector detector;

    private GestureDetector.SimpleOnGestureListener gestureListener;

    private ViewGroup touchInterceptionViewGroup;

    public ObserveListView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    private void init() {
        super.setOnScrollListener(this);
        gestureListener = new GestureDetector.SimpleOnGestureListener() {
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (onObserverScrollListener != null) {
                    onObserverScrollListener.onFling(scrollState);
                    handleFling = true;
                    return true;
                }
                return false;
            }
        };
        detector = new GestureDetector(getContext(), gestureListener);
        childrenHeights = new SparseIntArray();
        preFirstVisiblePos = 0;
        scrollDistance = 0;
        scrollState = ScrollState.STOP;
        disallowParentIntercept = true;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN :
                if (onObserverScrollListener != null) {
                    onObserverScrollListener.onDown();
                }
                if (disallowParentIntercept) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                // clean the previous move event
                preMoveEvent = null;
                break;
            case MotionEvent.ACTION_UP:
                disallowParentIntercept = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                // View will receive a cancel event when parent intercept
                // motion event, so we thought it as an end.
                if (disallowParentIntercept) {
                    disallowParentIntercept = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    disallowParentIntercept = true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (onObserverScrollListener == null) {
            return super.onTouchEvent(ev);
        }

        detector.onTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                intercepted = false;
                handleFling = false;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!handleFling) {
                    onObserverScrollListener.onUpOrCancel(scrollState);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (preMoveEvent == null) {
                    preMoveEvent = ev;
                }
                float diffY = ev.getY() - preMoveEvent.getY();
                preMoveEvent = MotionEvent.obtainNoHistory(ev);

                reachTop = diffY > 0 && getScrollDistance() - diffY < 0;
                boolean reachBottom = this.reachBottom & diffY < 0;
                // when reach the edge of list, dispatch event to its parent
                if (reachTop || reachBottom) {
                    // Can't scroll anymore.

                    if (intercepted) {
                        // Already dispatched ACTION_DOWN event to parents, so stop here.
                        return false;
                    }

                    final ViewGroup parent;
                    if (touchInterceptionViewGroup == null) {
                        parent = (ViewGroup) getParent();
                    } else {
                        parent = touchInterceptionViewGroup;
                    }
                    // Get offset to parents. If the parent is not the direct parent,
                    // we should aggregate offsets from all of the parents.
                    float offsetX = 0;
                    float offsetY = 0;
                    for (View v = this; v != null && v != parent; ) {
                        View parentOfV;
                        try {
                            parentOfV = (View) v.getParent();
                        } catch (ClassCastException ex) {
                            break;
                        }
                        // convert location to parent coordinate system
                        offsetX += v.getLeft() - parentOfV.getScrollX();
                        offsetY += v.getTop() - parentOfV.getScrollY();
                        v = parentOfV;
                    }
                    final MotionEvent event = MotionEvent.obtainNoHistory(ev);
                    event.offsetLocation(offsetX, offsetY);

                    if (parent.onInterceptTouchEvent(event)) {
                        intercepted = true;

                        // If the parent wants to intercept ACTION_MOVE events,
                        // we pass ACTION_DOWN event to the parent
                        // as if these touch events just have began now.
                        event.setAction(MotionEvent.ACTION_DOWN);

                        // Return this onTouchEvent() first and set ACTION_DOWN event for parent
                        // to the queue, to keep events sequence.
                        post(new Runnable() {
                            @Override
                            public void run() {
                                parent.dispatchTouchEvent(event);
                            }
                        });
                        return false;
                    }
                    // Even when this can't be scrolled anymore,
                    // simply returning false here may cause subView's click,
                    // so delegate it to super.
                    return super.onTouchEvent(ev);
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    public void onScrollStateChanged(AbsListView list, int scrollState) {
        if (outOnScrollListener != null) {
            outOnScrollListener.onScrollStateChanged(list, scrollState);
        }
    }

    public void onScroll(AbsListView list, int firstVisiblePos, int visibleCount, int totalCount) {
        if (totalCount > 0) {
            for (int i = 0, j = firstVisiblePos; i < visibleCount; i++, j++) {
                if (childrenHeights.indexOfKey(j) < 0 || childrenHeights.get(j) != getChildAt(i).getHeight()) {
                    childrenHeights.put(j, getChildAt(i).getHeight());
                }
            }

            int skippedChildrenHeight = 0;
            if (firstVisiblePos > preFirstVisiblePos) {
                for (int i = preFirstVisiblePos; i < firstVisiblePos; i++) {
                    if (childrenHeights.indexOfKey(i) >= 0) {
                        skippedChildrenHeight += childrenHeights.get(i);
                    }
                }
            } else if (firstVisiblePos < preFirstVisiblePos) {
                for (int i = firstVisiblePos; i < preFirstVisiblePos; i++) {
                    if (childrenHeights.indexOfKey(i) >= 0) {
                        skippedChildrenHeight -= childrenHeights.get(i);
                    }
                }
            }

            if (preFirstVisiblePos != firstVisiblePos) {
                if (onObserverScrollListener != null) {
                    onObserverScrollListener.onFirstVisibleItemChanged(firstVisiblePos);
                }
            }

            preFirstVisiblePos = firstVisiblePos;
            preSkippedHeight += skippedChildrenHeight;
            scrollDistance = preSkippedHeight - getChildAt(0).getTop() + getPaddingTop() + getDividerHeight() * firstVisiblePos;

            if (preScrollDistance < scrollDistance) {
                scrollState = ScrollState.UP;
            } else if (preScrollDistance > scrollDistance) {
                scrollState = ScrollState.DOWN;
            } else {
                scrollState = ScrollState.STOP;
            }
            preScrollDistance = scrollDistance;

            if (onObserverScrollListener != null) {
                onObserverScrollListener.onScroll(scrollState, scrollDistance);
            }

            if (firstVisiblePos + visibleCount == totalCount
                    && list.getBottom() == getChildAt(visibleCount - 1).getBottom() + list.getTop()) {
                reachBottom = true;
            } else {
                reachBottom = false;
            }
        }

        if (outOnScrollListener != null) {
            outOnScrollListener.onScroll(list, firstVisiblePos, visibleCount, totalCount);
        }
    }

    public int getScrollDistance() {
        return scrollDistance;
    }

    public void setOnScrollListener(OnScrollListener listener) {
        outOnScrollListener = listener;
    }

    public void setOnObserverScrollListener(OnObserverScrollListener listener) {
        onObserverScrollListener = listener;
    }

    public void setTouchInterceptionViewGroup(ViewGroup viewGroup) {
        touchInterceptionViewGroup = viewGroup;
    }

    public static abstract class OnObserverScrollListener {
        public void onScroll(ScrollState state, int scrollY) {
            Log.d(TAG, "ScrollDistance : " + scrollY);
        }
        public void onFirstVisibleItemChanged(int firstVisibleItem) {
            Log.d(TAG, "FirstVisibleItem : " + firstVisibleItem);
        }
        public void onDown() {
            Log.d(TAG, "Down");
        }
        public void onUpOrCancel(ScrollState state) {
            Log.d(TAG, "UpOrCancel");
        }
        public void onFling(ScrollState state) {
            Log.d(TAG, "Fling");
        }
    }

}
