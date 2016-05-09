package com.daprlabs.cardstack;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * Created by aaron on 4/12/2015.
 */
public class SwipeListener implements View.OnTouchListener {

    private final int flingSlop;
    private final int touchSlop;
    private final GestureDetector gestureDetector;
    private float rotationDegrees = 15f;
    private float initialX;
    private float initialY;

    private int mActivePointerId;
    private float initialXPress;
    private float initialYPress;
    private float parentWidth;

    private View card;
    private SwipeCallback callback;
    private boolean swipeable;
    private boolean isDragging = true;


    public SwipeListener(View card, final SwipeCallback callback, int initialX, int initialY, float rotation) {
        this.card = card;
        this.initialX = initialX;
        this.initialY = initialY;
        this.callback = callback;
        this.parentWidth = ((ViewGroup) card.getParent()).getWidth();
        this.rotationDegrees = rotation;

        ViewConfiguration viewConfiguration = ViewConfiguration.get(card.getContext());
        flingSlop = viewConfiguration.getScaledMinimumFlingVelocity();
        touchSlop = viewConfiguration.getScaledTouchSlop();
        gestureDetector = new GestureDetector(card.getContext(), new GestureListener());
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // We got a recognized fling event, it would call listener by itself, stop here
        boolean consumed = gestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return false;
        }

        return consumed;
//        switch (event.getAction() & MotionEvent.ACTION_MASK) {
//
//            case MotionEvent.ACTION_DOWN:
//                isDragging = false;
//                mActivePointerId = event.getPointerId(0);
//
//
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                //gesture is in progress
//
//                final int pointerIndex = event.findPointerIndex(mActivePointerId);
//                if (pointerIndex != 0) {
//                    break;
//                }
//
//                //calculate distance moved
//                final float dx = event.getX(pointerIndex) - initialXPress;
//                final float dy = event.getY(pointerIndex) - initialYPress;
//
//                // if touch deviated from original point too much, it's definitely not a click
//                if (Math.abs(dx) + Math.abs(dy) > touchSlop) {
//                    isDragging = true;
//                }
//
//                if (!isDragging) {
//                    return false;
//                }
//
//                if (!swipeable) {
//                    return false;
//                }
//
//                float posX = card.getX() + dx;
//                float posY = card.getY() + dy;
//                card.setX(posX);
//                card.setY(posY);
//
//                float distObjectX = posX - initialX;
//                float rotation = rotationDegrees * 2.f * distObjectX / parentWidth;
//                card.setRotation(rotation);
//                callback.onCardMove(rotation);
//                break;
//
//            case MotionEvent.ACTION_UP:
//                if (isDragging) {
//                    callback.onCardReset();
//                } else {
//                    callback.onCardClicked();
//                }
//                break;
//
//            default:
//                return false;
//        }
//        return true;
    }

    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
    }

    /*package*/ interface SwipeCallback {
        void onCardSwipedLeft(float velocityX, float velocityY);

        void onCardSwipedRight(float velocityX, float velocityY);

        void onCardClicked();

        void onCardReset();

        void onCardMove(float value);
    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) {
                return false;
            }
                final float dx = e2.getX() - initialXPress;
                final float dy = e2.getY() - initialYPress;
            float posX = card.getX() + dx;
            float posY = card.getY() + dy;
            card.setX(posX);
            card.setY(posY);

            float distObjectX = posX - initialX;
            float rotation = rotationDegrees * 2.f * distObjectX / parentWidth;
            card.setRotation(rotation);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) {
                return false;
            }
            float dx = e2.getX() - e1.getX();
            Log.d("TEMP", "onFling " + velocityX + " " + velocityY + " slop: " + flingSlop + " dx: " + dx + " touch:" +
                    touchSlop);
            double angle = Math.atan2(velocityY, velocityX) * 180 / Math.PI % 360;
            angle = angle > 0 ? angle : angle + 360;
            Log.d("TEMP", "Angle: " + angle);
            if (angle < 45 || angle > 315) {
                callback.onCardSwipedRight(velocityX, velocityY);
                return true;
            } else if (angle > 135 && angle < 225) {
                callback.onCardSwipedLeft(velocityX, velocityY);
                return true;
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            initialXPress = e.getX();
            initialYPress = e.getY();
            return true;
        }
    }

}
