package com.chuk3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Created by Admin on 17/08/2017.
 */

public class TouchView extends View {

    public static LinkedList<Movable> shapes = new LinkedList<>();
    public static LinkedList<Movable> toppings = new LinkedList<>();


    private float mPosX;
    private float mPosY;

    private float mLastTouchX;
    private float mLastTouchY;

    private float heightScreen;
    private float widthScreen;

    private static final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    private MoveCommand moveCommand = null;

    public static SizedStack<Command> commandStack = new SizedStack<Command>(100);


    public TouchView(Context context) {
        this(context, null, 0);
    }

    public TouchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        setLayoutParams();
    }

    public void setLayoutParams(){

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int)widthScreen, (int)heightScreen);

        lp.addRule(Gravity.CENTER);

        setLayoutParams(lp);
    }


    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(toppings.isEmpty()){
            for(Movable movable:shapes){
                movable.draw(canvas);
            }
        }else{
            for (Movable movable: toppings){
                movable.draw(canvas, 1);
            }
        }

    }

    public View drawToppings(){
        for (Movable movable:shapes){
            if(movable instanceof ToppingShape || movable instanceof ToppingText|| movable instanceof ToppingHole){
                toppings.add(movable);
            }
        }
        invalidate();
        return this;
    }

    public void removeToppings(){
        while (!toppings.isEmpty()) {
            toppings.remove(0);
        }
        invalidate();
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        mScaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                if(!shapes.isEmpty()){

                    Movable movable = Movable.getCurrent_movable(ev, mScaleFactor, getContext());

                    if (movable != null) {
                        moveCommand = new MoveCommand(movable, getWidth(), getHeight());
                        DesignActivity.vButton.setVisibility(VISIBLE);
                        DesignActivity.showDeleteAndRotate();
                        invalidate();

                        final float x = ev.getX();
                        final float y = ev.getY();
                        mLastTouchX = x;
                        mLastTouchY = y;
                        mActivePointerId = ev.getPointerId(0);
                    }

                }

                break;

            }

            case MotionEvent.ACTION_MOVE: {
                Movable movable = Movable.current_movable;

                if (movable != null) {
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                    if (pointerIndex >= 0) {

                        final float x = ev.getX(pointerIndex);
                        final float y = ev.getY(pointerIndex);

                        if (!mScaleDetector.isInProgress()) {

                            final float dx = x - mLastTouchX;
                            final float dy = y - mLastTouchY;

                            if(moveCommand != null){

                                float xpos = movable.getPosX();
                                float ypos = movable.getPosY();

                                moveCommand.setNewX(xpos+dx);
                                moveCommand.setNewY(ypos+dy);
                                moveCommand.execute();
                                invalidate();
                            }
                        }
                        mLastTouchX = x;
                        mLastTouchY = y;

                        break;
                    }
                }
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;

                if (moveCommand!= null && moveCommand.isExecute()) {
                    commandStack.push(moveCommand);
                    moveCommand = null;

                }

                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                if (moveCommand!= null && moveCommand.isExecute()) {
                    commandStack.push(moveCommand);
                    moveCommand = null;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);

                if (pointerIndex < 0) {
                    Log.e("ash", "Got ACTION_UP event but have an invalid active pointer id.");
                    return false;
                }

                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                onSecondaryPointerUp(ev);

                break;
            }
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastTouchX = ev.getX(newPointerIndex);
            mLastTouchY = ev.getY(newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {


        private ScaleCommand scaleCommand = null;



        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {

            if(Movable.current_movable != null){
                scaleCommand = new ScaleCommand(Movable.current_movable);
            }

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

            if(scaleCommand != null){
                if (scaleCommand.isExecute()) {
                    commandStack.push(scaleCommand);
                    scaleCommand = null;

                    //activate moveCommand for case x  y  exceed from borders of view after scale
                    MoveCommand moveCommand = new MoveCommand(Movable.current_movable, getWidth(), getHeight());
                    moveCommand.setNewX(Movable.current_movable.getPosX());
                    moveCommand.setNewY(Movable.current_movable.getPosY());
                    moveCommand.execute();
                }
            }
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {


            try{
                mScaleFactor *= detector.getScaleFactor();

                mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
                if(scaleCommand!= null){
                    scaleCommand.setNewScaleFactor(mScaleFactor);
                    scaleCommand.execute();
                }

            }catch (IndexOutOfBoundsException e){

            }

            return true;
        }
    }


    public void executeAddCommand(Context context, int resourceId, String text, MovableType type) {
        AddCommand addCommand = new AddCommand(context, resourceId, text, mPosX, mPosY, type);
        boolean isExecute = addCommand.execute();
        if (isExecute) {
            commandStack.push(addCommand);
        }
    }


    public void executeAngleCommand(Movable movable, float newAngle) {
        AngleCommand angleCommand = new AngleCommand(movable, newAngle);
        boolean isExecute = angleCommand.execute();
        if (isExecute) {
            commandStack.push(angleCommand);
            //activate moveCommand for case x  y  exceed from borders of view after rotation
            MoveCommand moveCommand = new MoveCommand(Movable.current_movable, getWidth(), getHeight());
            moveCommand.setNewX(Movable.current_movable.getPosX());
            moveCommand.setNewY(Movable.current_movable.getPosY());
            moveCommand.execute();
        }
    }

    public void undo(){
        if (!commandStack.isEmpty()) {
            Command command = commandStack.pop();
            command.undo();
            invalidate();
        }

    }

    public void init(){

        heightScreen = getResources().getDisplayMetrics().heightPixels;
        widthScreen = getResources().getDisplayMetrics().widthPixels;

        mPosX = widthScreen / 2.5f;
        mPosY = heightScreen / 5.3f;

        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

    }

}