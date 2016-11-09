package com.shinelw.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * colorful arc progress bar
 * Created by shinelw on 12/4/15.
 */

public class ColorArcProgressBar extends View
{
    private final int DEGREE_PROGRESS_DISTANCE = dipToPx(8);//弧形与外层刻度的距离

    private int mWidth;
    private int mHeight;

    private int diameter = 500;  //直径
    private float centerX;  //圆心X坐标
    private float centerY;  //圆心Y坐标

    private Paint allArcPaint;
    private Paint progressPaint;
    private Paint vTextPaint;
    private Paint hintPaint;
    private Paint degreePaint;
    private Paint curSpeedPaint;

    private RectF bgRect;

    private ValueAnimator        progressAnimator;
    private PaintFlagsDrawFilter mDrawFilter;
    private SweepGradient        sweepGradient;//颜色渲染
    private Matrix               rotateMatrix;

    private int[] colors = new int[]{Color.GREEN, Color.YELLOW, Color.RED, Color.RED};

    private float mTouchInvalidateRadius;//触摸失效半径,控件外层都可触摸,当触摸区域小于这个值的时候触摸失效

    private float startAngle   = 135;//开始角度(0°与控件X轴平行)
    private float sweepAngle   = 270;//弧形扫过的区域
    private float currentAngle = 0;
    private float lastAngle;

    private float maxValues     = 60;
    private float currentValues = 0;
    private float bgArcWidth    = dipToPx(10);
    private float progressWidth = dipToPx(10);
    private float textSize      = dipToPx(60);
    private float hintSize      = dipToPx(15);
    private float curSpeedSize  = dipToPx(13);
    private int   aniSpeed      = 200;//动画时长
    private float longDegree    = dipToPx(13);//长刻度
    private float shortDegree   = dipToPx(5);//短刻度


    private int longDegreeColor  = 0xff111111;
    private int shortDegreeColor = 0xff111111;
    private int hintColor        = 0xff676767;
    private int bgArcColor       = 0xff111111;

    private String titleString;
    private String hintString;

    private boolean isShowCurrentSpeed = true;

    private boolean isNeedTitle;
    private boolean isNeedUnit;
    private boolean isNeedDial;
    private boolean isNeedContent;
    private boolean isAutoTextSize = true;

    // sweepAngle / maxValues 的值
    private float k;

    private OnSeekArcChangeListener listener;

    private boolean seekEnable;

    public ColorArcProgressBar(Context context)
    {
        super(context, null);
        initView();
    }

    public ColorArcProgressBar(Context context, AttributeSet attrs)
    {
        super(context, attrs, 0);
        initConfig(context, attrs);
        initView();
    }

    public ColorArcProgressBar(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initConfig(context, attrs);
        initView();
    }

    /**
     * 初始化布局配置
     *
     * @param context
     * @param attrs
     */
    private void initConfig(Context context, AttributeSet attrs)
    {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorArcProgressBar);

        int color1 = a.getColor(R.styleable.ColorArcProgressBar_front_color1, Color.GREEN);
        int color2 = a.getColor(R.styleable.ColorArcProgressBar_front_color2, color1);
        int color3 = a.getColor(R.styleable.ColorArcProgressBar_front_color3, color1);

        bgArcColor = a.getColor(R.styleable.ColorArcProgressBar_bg_arc_color, 0xff111111);
        longDegreeColor = a.getColor(R.styleable.ColorArcProgressBar_degree_color, 0xff111111);
        shortDegreeColor = a.getColor(R.styleable.ColorArcProgressBar_degree_color, 0xff111111);
        hintColor = a.getColor(R.styleable.ColorArcProgressBar_hint_color, 0xff676767);

        colors = new int[]{color1, color2, color3, color3};

        sweepAngle = a.getInteger(R.styleable.ColorArcProgressBar_sweep_angle, 270);
        bgArcWidth = a.getDimension(R.styleable.ColorArcProgressBar_bg_arc_width, dipToPx(10));
        progressWidth = a.getDimension(R.styleable.ColorArcProgressBar_front_width, dipToPx(10));

        seekEnable = a.getBoolean(R.styleable.ColorArcProgressBar_is_seek_enable, false);
        isNeedTitle = a.getBoolean(R.styleable.ColorArcProgressBar_is_need_title, false);
        isNeedContent = a.getBoolean(R.styleable.ColorArcProgressBar_is_need_content, false);
        isNeedUnit = a.getBoolean(R.styleable.ColorArcProgressBar_is_need_unit, false);
        isNeedDial = a.getBoolean(R.styleable.ColorArcProgressBar_is_need_dial, false);

        hintString = a.getString(R.styleable.ColorArcProgressBar_string_unit);
        titleString = a.getString(R.styleable.ColorArcProgressBar_string_title);

        currentValues = a.getFloat(R.styleable.ColorArcProgressBar_current_value, 0);
        maxValues = a.getFloat(R.styleable.ColorArcProgressBar_max_value, 60);

        setCurrentValues(currentValues);
        setMaxValues(maxValues);

        a.recycle();

    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
//    {
//        int width  = (int) (2 * (longDegree + DEGREE_PROGRESS_DISTANCE) + progressWidth + diameter);
//        int height = (int) (2 * (longDegree + DEGREE_PROGRESS_DISTANCE) + progressWidth + diameter);
//        Log.v("ColorArcProgressBar", "onMeasure: width:"+width+" height:"+height);
//        setMeasuredDimension(width, height);
//    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        Log.v("ColorArcProgressBar", "onSizeChanged: mWidth:" + mWidth + " mHeight:" + mHeight);

        diameter = (int) (Math.min(mWidth, mHeight) - 2 * (longDegree + DEGREE_PROGRESS_DISTANCE + progressWidth / 2));

        Log.v("ColorArcProgressBar", "onSizeChanged: diameter:" + diameter);

        //弧形的矩阵区域
        bgRect = new RectF();
        bgRect.top = longDegree + DEGREE_PROGRESS_DISTANCE + progressWidth / 2;
        bgRect.left = longDegree + DEGREE_PROGRESS_DISTANCE + progressWidth / 2;
        bgRect.right = diameter + (longDegree + progressWidth / 2 + DEGREE_PROGRESS_DISTANCE);
        bgRect.bottom = diameter + (longDegree + progressWidth / 2 + DEGREE_PROGRESS_DISTANCE);

        Log.v("ColorArcProgressBar", "initView: " + diameter);

        //圆心
        centerX = (2 * (longDegree + DEGREE_PROGRESS_DISTANCE + progressWidth / 2) + diameter) / 2;
        centerY = (2 * (longDegree + DEGREE_PROGRESS_DISTANCE + progressWidth / 2) + diameter) / 2;

        sweepGradient = new SweepGradient(centerX, centerY, colors, null);

        mTouchInvalidateRadius = Math.max(mWidth, mHeight) / 2 - longDegree - DEGREE_PROGRESS_DISTANCE - progressWidth * 2;

        if(isAutoTextSize)
        {
            textSize = (float) (diameter * 0.3);
            hintSize = (float) (diameter * 0.1);
            curSpeedSize = (float) (diameter * 0.1);

            vTextPaint.setTextSize(textSize);
            hintPaint.setTextSize(hintSize);
            curSpeedPaint.setTextSize(curSpeedSize);
        }

    }

    private void initView()
    {

//        diameter = 3 * getScreenWidth() / 5;

        //外部刻度线画笔
        degreePaint = new Paint();
        degreePaint.setColor(longDegreeColor);

        //整个弧形画笔
        allArcPaint = new Paint();
        allArcPaint.setAntiAlias(true);
        allArcPaint.setStyle(Paint.Style.STROKE);
        allArcPaint.setStrokeWidth(bgArcWidth);
        allArcPaint.setColor((bgArcColor));
        allArcPaint.setStrokeCap(Paint.Cap.ROUND);

        //当前进度的弧形画笔
        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(progressWidth);
        progressPaint.setColor(Color.GREEN);

        //内容显示文字
        vTextPaint = new Paint();
//        vTextPaint.setTextSize(textSize);
        vTextPaint.setColor(Color.BLACK);
        vTextPaint.setTextAlign(Paint.Align.CENTER);

        //显示单位文字
        hintPaint = new Paint();
//        hintPaint.setTextSize(hintSize);
        hintPaint.setColor(hintColor);
        hintPaint.setTextAlign(Paint.Align.CENTER);

        //显示标题文字
        curSpeedPaint = new Paint();
//        curSpeedPaint.setTextSize(curSpeedSize);
        curSpeedPaint.setColor(hintColor);
        curSpeedPaint.setTextAlign(Paint.Align.CENTER);

        mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        rotateMatrix = new Matrix();

    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        //抗锯齿
        canvas.setDrawFilter(mDrawFilter);

        if(isNeedDial)
        {
            //画刻度线
            for(int i = 0; i < 40; i++)//把整个圆划分成8大份40小份
            {
                if(i > 15 && i < 25)//15~25小份不需要显示
                {
                    canvas.rotate(9, centerX, centerY);//40小份,每份9°
                    continue;
                }
                if(i % 5 == 0)//画长刻度
                {
                    degreePaint.setStrokeWidth(dipToPx(2));
                    degreePaint.setColor(longDegreeColor);
                    canvas.drawLine(centerX, centerY - diameter / 2 - progressWidth / 2 - DEGREE_PROGRESS_DISTANCE, centerX, centerY - diameter / 2 - progressWidth / 2 - DEGREE_PROGRESS_DISTANCE - longDegree, degreePaint);
                }
                else//画短刻度
                {
                    degreePaint.setStrokeWidth(dipToPx(1.4f));
                    degreePaint.setColor(shortDegreeColor);
                    canvas.drawLine(centerX, centerY - diameter / 2 - progressWidth / 2 - DEGREE_PROGRESS_DISTANCE - (longDegree - shortDegree) / 2, centerX, centerY - diameter / 2 - progressWidth / 2 - DEGREE_PROGRESS_DISTANCE - (longDegree - shortDegree) / 2 - shortDegree, degreePaint);
                }
                //每绘制一个小刻度,旋转1/40
                canvas.rotate(9, centerX, centerY);
            }
        }

        //整个弧
        canvas.drawArc(bgRect, startAngle, sweepAngle, false, allArcPaint);

        //设置渐变色
        rotateMatrix.setRotate(130, centerX, centerY);
        sweepGradient.setLocalMatrix(rotateMatrix);
        progressPaint.setShader(sweepGradient);

        //当前进度
        canvas.drawArc(bgRect, startAngle, currentAngle, false, progressPaint);

        if(isNeedContent)
        {
            //drawText的第三个参数代表的是基线坐标,只要x坐标、基线位置、文字大小确定以后，文字的位置就是确定的了。
            canvas.drawText(String.format("%.0f", currentValues), centerX, centerY + textSize / 4, vTextPaint);
        }
        if(isNeedUnit)
        {
            canvas.drawText(hintString, centerX, centerY + textSize, hintPaint);
        }
        if(isNeedTitle)
        {
            canvas.drawText(titleString, centerX, centerY - textSize, curSpeedPaint);
        }

        invalidate();

    }

    /**
     * 设置最大值
     *
     * @param maxValues
     */
    public void setMaxValues(float maxValues)
    {
        this.maxValues = maxValues;
        k = sweepAngle / maxValues;
    }

    /**
     * 设置当前值
     *
     * @param currentValues
     */
    public void setCurrentValues(float currentValues)
    {
        if(currentValues > maxValues)
        {
            currentValues = maxValues;
        }
        if(currentValues < 0)
        {
            currentValues = 0;
        }
        this.currentValues = currentValues;
        lastAngle = currentAngle;
        setAnimation(lastAngle, currentValues * k, aniSpeed);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(seekEnable)
        {
            this.getParent().requestDisallowInterceptTouchEvent(true);//一旦底层View收到touch的action后调用这个方法那么父层View就不会再调用onInterceptTouchEvent了，也无法截获以后的action

            switch(event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    onStartTrackingTouch();
                    updateOnTouch(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    updateOnTouch(event);
                    break;
                case MotionEvent.ACTION_UP:
                    onStopTrackingTouch();
                    setPressed(false);
                    this.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    onStopTrackingTouch();
                    setPressed(false);
                    this.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return true;
        }
        return false;
    }


    private void onStartTrackingTouch()
    {
        if(listener != null)
        {
            listener.onStartTrackingTouch(this);
        }
    }

    private void onStopTrackingTouch()
    {
        if(listener != null)
        {
            listener.onStopTrackingTouch(this);
        }
    }




    private void updateOnTouch(MotionEvent event)
    {
        boolean validateTouch = validateTouch(event.getX(), event.getY());
        if(!validateTouch)
        {
            return;
        }
        setPressed(true);
        double mTouchAngle = getTouchDegrees(event.getX(), event.getY());

        int progress = angleToProgress(mTouchAngle);
        Log.v("ColorArcProgressBar", "updateOnTouch: " + progress);
        onProgressRefresh(progress, true);
    }

    /**
     * 判断触摸是否有效
     *
     * @param xPos x
     * @param yPos y
     * @return is validate touch
     */
    private boolean validateTouch(float xPos, float yPos)
    {
        boolean validate = false;

        float x = xPos - centerX;
        float y = yPos - centerY;

        float touchRadius = (float) Math.sqrt(((x * x) + (y * y)));//触摸半径

        double angle = Math.toDegrees(Math.atan2(y, x) + (Math.PI / 2) - Math.toRadians(225));

        if(angle < 0)
        {
            angle = 360 + angle;
        }
//

        if(touchRadius > mTouchInvalidateRadius && (angle >= 0 && angle <= 280))//其实角度小于270就够了,但是弧度换成角度是不精确的,所以需要适当放大范围,不然有时候滑动不到最大值
        {
            validate = true;
        }

        Log.v("ColorArcProgressBar", "validateTouch: " + angle);
        return validate;
    }

    private double getTouchDegrees(float xPos, float yPos)
    {
        float x = xPos - centerX;//触摸点X坐标与圆心X坐标的距离
        float y = yPos - centerY;//触摸点Y坐标与圆心Y坐标的距离
        // Math.toDegrees convert to arc Angle

        //Math.atan2(y, x)以弧度为单位计算并返回点 y /x 的夹角，该角度从圆的 x 轴（0 点在其上，0 表示圆心）沿逆时针方向测量。返回值介于正 pi 和负 pi 之间。
        //触摸点与圆心的夹角- Math.toRadians(225)是因为我们希望0°从圆弧的起点开始,默认角度从穿过圆心的X轴开始
        double angle = Math.toDegrees(Math.atan2(y, x) + (Math.PI / 2) - Math.toRadians(225));

        if(angle < 0)
        {
            angle = 360 + angle;
        }
        Log.v("ColorArcProgressBar", "getTouchDegrees: " + angle);
//        angle -= mStartAngle;
        return angle;
    }

    private int angleToProgress(double angle)
    {

        int progress = (int) Math.round(valuePerDegree() * angle);

        progress = (progress < 0) ? 0 : progress;
        progress = (progress > maxValues) ? (int) maxValues : progress;
        return progress;
    }

    private float valuePerDegree()
    {
        return maxValues / sweepAngle;
    }

    private void onProgressRefresh(int progress, boolean fromUser)
    {
        updateProgress(progress, fromUser);
    }

    private void updateProgress(int progress, boolean fromUser)
    {

        currentValues = progress;

        if(listener != null)
        {
            listener.onProgressChanged(this, progress, fromUser);
        }

        currentAngle = (float) progress / maxValues * sweepAngle;//计算划过当前的角度

        lastAngle = currentAngle;

        invalidate();
    }


    /**
     * 设置整个圆弧宽度
     *
     * @param bgArcWidth
     */
    public void setArcWidth(int bgArcWidth)
    {
        this.bgArcWidth = bgArcWidth;
    }

    /**
     * 设置进度宽度
     *
     * @param progressWidth
     */
    public void setProgressWidth(int progressWidth)
    {
        this.progressWidth = progressWidth;
    }

    /**
     * 设置速度文字大小
     *
     * @param textSize
     */
    public void setTextSize(int textSize)
    {
        this.textSize = textSize;
    }

    /**
     * 设置单位文字大小
     *
     * @param hintSize
     */
    public void setHintSize(int hintSize)
    {
        this.hintSize = hintSize;
    }

    /**
     * 设置单位文字
     *
     * @param hintString
     */
    public void setUnit(String hintString)
    {
        this.hintString = hintString;
        invalidate();
    }

    /**
     * 设置直径大小
     *
     * @param diameter
     */
    public void setDiameter(int diameter)
    {
        this.diameter = dipToPx(diameter);
    }

    /**
     * 设置标题
     *
     * @param title
     */
    private void setTitle(String title)
    {
        this.titleString = title;
    }

    /**
     * 设置是否显示标题
     *
     * @param isNeedTitle
     */
    private void setIsNeedTitle(boolean isNeedTitle)
    {
        this.isNeedTitle = isNeedTitle;
    }

    /**
     * 设置是否显示单位文字
     *
     * @param isNeedUnit
     */
    private void setIsNeedUnit(boolean isNeedUnit)
    {
        this.isNeedUnit = isNeedUnit;
    }

    /**
     * 设置是否显示外部刻度盘
     *
     * @param isNeedDial
     */
    private void setIsNeedDial(boolean isNeedDial)
    {
        this.isNeedDial = isNeedDial;
    }

    /**
     * 为进度设置动画
     *
     * @param last
     * @param current
     */
    private void setAnimation(float last, float current, int length)
    {
        progressAnimator = ValueAnimator.ofFloat(last, current);
        progressAnimator.setDuration(length);
        progressAnimator.setTarget(currentAngle);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {

            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                currentAngle = (float) animation.getAnimatedValue();
                currentValues = currentAngle / k;
            }
        });
        progressAnimator.start();
    }

    public void setSeekEnable(boolean seekEnable)
    {
        this.seekEnable = seekEnable;
    }

    public void setOnSeekArcChangeListener(OnSeekArcChangeListener listener)
    {
        this.listener = listener;
    }

    /**
     * dip 转换成px
     *
     * @param dip
     * @return
     */
    private int dipToPx(float dip)
    {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dip * density + 0.5f * (dip >= 0 ? 1 : -1));
    }

    /**
     * 得到屏幕宽度
     *
     * @return
     */
    private int getScreenWidth()
    {
        WindowManager  windowManager  = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public interface OnSeekArcChangeListener
    {

        /**
         * Notification that the progress level has changed. Clients can use the
         * fromUser parameter to distinguish user-initiated changes from those
         * that occurred programmatically.
         *
         * @param seekArc  The SeekArc whose progress has changed
         * @param progress The current progress level. This will be in the range
         *                 0..max where max was set by
         *                 {@link ColorArcProgressBar#setMaxValues(float)} . (The default value for
         *                 max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(ColorArcProgressBar seekArc, int progress, boolean fromUser);

        /**
         * Notification that the user has started a touch gesture. Clients may
         * want to use this to disable advancing the SeekBar.
         *
         * @param seekArc The SeekArc in which the touch gesture began
         */
        void onStartTrackingTouch(ColorArcProgressBar seekArc);

        /**
         * Notification that the user has finished a touch gesture. Clients may
         * want to use this to re-enable advancing the SeekBar.
         *
         * @param seekArc The SeekArc in which the touch gesture began
         */
        void onStopTrackingTouch(ColorArcProgressBar seekArc);
    }
}
