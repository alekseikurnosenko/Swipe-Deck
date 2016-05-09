package com.daprlabs.cardstack;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.OvershootInterpolator;
import android.widget.Adapter;
import android.widget.FrameLayout;

import java.util.ArrayList;

/**
 * Created by aaron on 4/12/2015.
 */
public class SwipeDeck extends FrameLayout {

    private static final String TAG = SwipeDeck.class.getSimpleName();
    private static int NUMBER_OF_CARDS;
    private float ROTATION_DEGREES;
    private float CARD_SPACING;
    private boolean RENDER_ABOVE;
    private boolean RENDER_BELOW;
    private int CARD_GRAVITY;
    private boolean hardwareAccelerationEnabled = true;

    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    private SwipeEventCallback eventCallback;
    private Adapter adapter;
    private DataSetObserver observer;
    private int currentPosition = -1;
    private SwipeListener swipeListener;
    private boolean swipeable = true;
    private View topCard;

    public SwipeDeck(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SwipeDeck,
                0, 0);
        try {
            NUMBER_OF_CARDS = a.getInt(R.styleable.SwipeDeck_max_visible, 3);
            ROTATION_DEGREES = a.getFloat(R.styleable.SwipeDeck_rotation_degrees, 15f);
            CARD_SPACING = a.getDimension(R.styleable.SwipeDeck_card_spacing, 15f);
            RENDER_ABOVE = a.getBoolean(R.styleable.SwipeDeck_render_above, true);
            RENDER_BELOW = a.getBoolean(R.styleable.SwipeDeck_render_below, false);
            CARD_GRAVITY = a.getInt(R.styleable.SwipeDeck_card_gravity, 0);
        } finally {
            a.recycle();
        }

        paddingBottom = getPaddingBottom();
        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();
        paddingTop = getPaddingTop();

        //set clipping of view parent to false so cards render outside their view boundary
        //make sure not to clip to padding
        setClipToPadding(false);
        setClipChildren(false);

        this.setWillNotDraw(false);

        //render the cards and card deck above or below everything
        if (RENDER_ABOVE) {
            ViewCompat.setTranslationZ(this, Float.MAX_VALUE);
        }
        if (RENDER_BELOW) {
            ViewCompat.setTranslationZ(this, Float.MIN_VALUE);
        }
    }

    /**
     * Set Hardware Acceleration Enabled.
     */
    public void setHardwareAccelerationEnabled(Boolean accel) {
        this.hardwareAccelerationEnabled = accel;
    }

    public void setAdapter(Adapter adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterDataSetObserver(observer);
        }
        if (adapter != null && adapter.getCount() > 0) {
            currentPosition = 0;
        }
        this.adapter = adapter;

        observer = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                //handle data set changes
                //if we need to add any cards at this point (ie. the amount of cards on screen
                //is less than the max number of cards to display) add the cards.
                int childCount = getChildCount();
                //only perform action if there are less cards on screen than NUMBER_OF_CARDS
                if (childCount < NUMBER_OF_CARDS) {
                    requestLayout();
                }
            }

            @Override
            public void onInvalidated() {
                //reset state, remove views and request layout
                currentPosition = 0;
                removeAllViews();
            }
        };

        adapter.registerDataSetObserver(observer);
        removeAllViews();
    }


    public void setPosition(int position) {
        if (position < adapter.getCount()) {
            currentPosition = position;
            removeAllViews();
        }
    }

    public View getSelectedView() {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Set whether cards in the deck can be swiped.
     *
     * @param swipeable whether cards should be swipeable
     */
    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
        // We may have no listener if we call this before settings adapter
        if (swipeListener != null) {
            swipeListener.setSwipeable(swipeable);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // if we don't have an adapter, we don't need to do anything
        if (adapter == null || adapter.getCount() == 0) {
            currentPosition = -1;
            removeAllViewsInLayout();
            return;
        }

        //pull in views from the adapter at the position the top of the deck is set to
        //stop when you get to for cards or the end of the adapter
        int childCount = getChildCount();
        for (int i = childCount; i < NUMBER_OF_CARDS; ++i) {
            addNextCard();
        }
        positionViews();
        setupTopCard();
        //position the new children we just added and set up the top card with a listener etc
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = widthSize;
        } else {
            //Be whatever you want
            width = widthSize;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = heightSize;
        } else {
            //Be whatever you want
            height = heightSize;
        }
        setMeasuredDimension(width, height);
    }

    private void removeTopCard() {
        // Disable it while we remove it
        topCard.setOnTouchListener(null);
        swipeListener = null;
        removeView(topCard);

        // In order to catch usage of already removed card
        topCard = null;

        //if there are no more children left after top card removal let the callback know
        if (getChildCount() <= 0) {
            if (eventCallback != null) {
                eventCallback.onCardsDepleted();
            }
        } else {
            setupTopCard();
        }
    }

    private void addNextCard() {
        if (currentPosition + 1 < adapter.getCount()) {
            currentPosition++;
            // TODO: Make view recycling work
            // TODO: Instead of removing the view from here and adding it again when it's swiped
            // ... don't remove and add to this instance: don't call removeView & addView in sequence.
            View newBottomChild = adapter.getView(currentPosition, null/*lastRemovedView*/, this);

            if (hardwareAccelerationEnabled) {
                //set backed by an off-screen buffer
                newBottomChild.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            addAndMeasureChild(newBottomChild);
        }
    }

    private void setupTopCard() {
        // get top child
        topCard = getChildAt(getChildCount() - 1);
        if (topCard != null) {

            swipeListener =
                    new SwipeListener(topCard, new SwipeDeckCallback(), paddingLeft, paddingTop, ROTATION_DEGREES);
            swipeListener.setSwipeable(swipeable);

            topCard.setOnTouchListener(swipeListener);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setZTranslations() {
        //this is only needed to add shadows to cardviews on > lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int count = getChildCount();
            for (int i = 0; i < count; ++i) {
                getChildAt(i).setTranslationZ(i * 10);
            }
        }
    }

    /**
     * Adds a view as a child view and takes care of measuring it
     *
     * @param child The view to add
     */
    private void addAndMeasureChild(View child) {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }

        //ensure new card is under the deck at the beginning
        child.setY(paddingTop);

        //every time we add and measure a child refresh the children on screen and order them
        ArrayList<View> children = new ArrayList<>();
        children.add(child);
        for (int i = 0; i < getChildCount(); ++i) {
            children.add(getChildAt(i));
        }

        removeAllViews();

        for (View c : children) {
            addViewInLayout(c, -1, params, true);
            int itemWidth = getWidth() - (paddingLeft + paddingRight);
            int itemHeight = getHeight() - (paddingTop + paddingBottom);
            c.measure(MeasureSpec.EXACTLY | itemWidth, MeasureSpec.EXACTLY | itemHeight); //MeasureSpec.UNSPECIFIED
        }
        setZTranslations();
    }

    /**
     * Positions the children at the "correct" positions
     */
    private void positionViews() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View child = getChildAt(i);

            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            int left = (getWidth() - width) / 2;
            child.layout(left, paddingTop, left + width, paddingTop + height);
            // layout each child slightly above the previous child (we start with the bottom)

            int offset = (int) ((childCount - 1 - i) * CARD_SPACING);

            child.animate().setDuration(160).y(paddingTop + offset);
        }
    }

    public void setEventCallback(SwipeEventCallback eventCallback) {
        this.eventCallback = eventCallback;
    }

//    public void swipeTopCardLeft(int duration) {
//        if (topCard == null) {
//            return;
//        }
//        animateOffScreenLeft(topCard, duration);
//
//        int positionInAdapter = nextAdapterCard - getChildCount();
//        removeTopCard();
//        if (eventCallback != null) {
//            eventCallback.onCardSwipedLeft(positionInAdapter);
//        }
//    }
//
//    public void swipeTopCardRight(int duration) {
//        if (topCard == null) {
//            return;
//        }
//        animateOffScreenRight(topCard, duration);
//
//        int positionInAdapter = nextAdapterCard - getChildCount();
//        removeTopCard();
//        if (eventCallback != null) {
//            eventCallback.onCardSwipedRight(positionInAdapter);
//        }
//    }

    public void animateCardReset(View card) {
        card.animate()
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .x(paddingLeft)
                .y(paddingRight)
                .rotation(0);
    }

    private void animateOffScreen(View card, float velocityX, float velocityY) {
        setEnabled(false);
        card.animate()
                .setDuration(200)
                .xBy(velocityX)
                .yBy(velocityY)
                .alpha(0)
                .setListener(new AnimatorEndListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setEnabled(true);
                        removeTopCard();
                    }
                });
        //addNextCard();
    }

    public ViewPropertyAnimator animateOffScreenLeft(View card, int duration) {
        return card.animate()
                .setDuration(duration)
                .x(getWidth() * -1)
                .y(0)
                .rotation(-30);
    }

    public ViewPropertyAnimator animateOffScreenRight(View card, int duration) {
        return card.animate()
                .setDuration(duration)
                .x(getWidth() * -1)
                .y(0)
                .rotation(30);
    }

    public interface SwipeEventCallback {
        //returning the object position in the adapter
        void onCardSwipedLeft(int position);

        void onCardSwipedRight(int position);

        void onCardsDepleted();

        void onCardClicked(int position);

        void onCardMove(float value);
    }

    private class SwipeDeckCallback implements SwipeListener.SwipeCallback {
        @Override
        public void onCardSwipedLeft(float velocityX, float velocityY) {
            animateOffScreen(topCard, velocityX, velocityY);
            int positionInAdapter = currentPosition - getChildCount();
            if (eventCallback != null) {
                eventCallback.onCardSwipedLeft(positionInAdapter);
            }
        }

        @Override
        public void onCardSwipedRight(float velocityX, float velocityY) {
            animateOffScreen(topCard, velocityX, velocityY);
            int positionInAdapter = currentPosition - getChildCount();
            if (eventCallback != null) {
                eventCallback.onCardSwipedRight(positionInAdapter);
            }
        }

        @Override
        public void onCardClicked() {
            int positionInAdapter = currentPosition - getChildCount();
            if (eventCallback != null) {
                eventCallback.onCardClicked(positionInAdapter);
            }
        }

        @Override
        public void onCardReset() {
            animateCardReset(topCard);
        }

        @Override
        public void onCardMove(float value) {
            if (eventCallback != null) {
                eventCallback.onCardMove(value);
            }

        }
    }


}


