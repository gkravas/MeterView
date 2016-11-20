package com.gkravas.meterview;

import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Locale;

public final class MeterView extends View {

    private static final String TAG = MeterView.class.getSimpleName();

    // drawing tools
    private RectF rimRect;
    private Paint rimPaint;
    private Paint rimCirclePaint;

    private RectF faceRect;
    private Paint facePaint;

    private Paint scalePaint;
    private RectF scaleRect;

    private Paint valuePaint;
    private Paint valueRectPaint;

    private Paint logoPaint;
    private Bitmap logo;
    private Matrix logoMatrix;
    private float logoScale;

    private Paint handPaint;
    private Path handPath;
    private Paint handScrewPaint;

    private Paint backgroundPaint;
    // end drawing tools

    private Bitmap background; // holds the cached static part

    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 20;
    private static final int DEFAULT_IMAGE_RES_ID = 0;

    private static final int DEFAULT_FACE_COLOR = Color.WHITE;
    private static final int DEFAULT_SCALE_COLOR = Color.parseColor("#616161");
    private static final int DEFAULT_RIM_COLOR = Color.parseColor("#BDBDBD");
    private static final int DEFAULT_VALUE_COLOR = Color.parseColor("#000000");

    private static final int SUB_VALUE_LENGTH = 10;
    private static final float MAX_VISIBLE_VALUES = DEFAULT_MAX_VALUE;

    private float minValue;
    public float getMinValue() { return minValue; }
    public void setMinValue(float minValue) { this.minValue = minValue; }

    private float maxValue;
    public float getMaxValue() { return maxValue; }
    public void setMaxValue(float maxValue) { this.maxValue = maxValue; }

    private int logoResId;
    public int getLogo() { return logoResId; }
    public void setLogo(@DrawableRes int logoResId) {
        this.logoResId = logoResId;
        if (isImageAvailable()) {
            initLogo();
        }
    }


    private int faceColor;
    public int getFaceColor() { return faceColor; }
    public void setFaceColor(int backgroundColor) {
        this.faceColor = backgroundColor;
        facePaint.setColor(backgroundColor);
    }

    private int scaleColor;
    public int getScaleColor() { return scaleColor; }
    public void setScaleColor(int scaleColor) {
        this.scaleColor = scaleColor;
        scalePaint.setColor(scaleColor);
    }

    private int rimColor;
    public int getRimColor() { return rimColor; }
    public void setRimColor(int rimColor) {
        this.rimColor = rimColor;
        rimPaint.setColor(rimColor);
    }

    private int valueColor;
    public int getValueColor() { return valueColor; }
    public void setValueColor(int valueColor) {
        this.valueColor = valueColor;
        valuePaint.setColor(valueColor);
    }

    private static final int startingDegreeOffset = -140;
    private static final int totalDegrees = 290;
    private static final float logoScaleFactor = 0.25f;


    private float handPosition = 0f;
    private ValueAnimator va;

    public MeterView(Context context) {
        this(context, null);
    }

    public MeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MeterView, defStyle, 0);
        initByAttributes(attributes);
        attributes.recycle();
        init();
    }

    protected void initByAttributes(TypedArray attributes) {
        minValue = attributes.getFloat(R.styleable.MeterView_meter_min_value, DEFAULT_MIN_VALUE);
        maxValue = attributes.getFloat(R.styleable.MeterView_meter_max_value, DEFAULT_MAX_VALUE);
        logoResId = attributes.getResourceId(R.styleable.MeterView_meter_logo, DEFAULT_IMAGE_RES_ID);
        faceColor = attributes.getColor(R.styleable.MeterView_meter_faceColor, DEFAULT_FACE_COLOR);
        scaleColor = attributes.getColor(R.styleable.MeterView_meter_scaleColor, DEFAULT_SCALE_COLOR);
        rimColor = attributes.getColor(R.styleable.MeterView_meter_rimColor, DEFAULT_RIM_COLOR);
        valueColor = attributes.getColor(R.styleable.MeterView_meter_valueColor, DEFAULT_VALUE_COLOR);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        Parcelable superState = bundle.getParcelable("superState");
        super.onRestoreInstanceState(superState);

        handPosition = bundle.getFloat("handPosition");
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        Bundle state = new Bundle();
        state.putParcelable("superState", superState);
        state.putFloat("handPosition", handPosition);
        return state;
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        initDrawingTools();
    }

    private void initDrawingTools() {
        rimRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);

        // the linear gradient is a bit skewed for realism
        rimPaint = new Paint();
        rimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        rimPaint.setColor(rimColor);

        rimCirclePaint = new Paint();
        rimCirclePaint.setAntiAlias(true);
        rimCirclePaint.setStyle(Paint.Style.STROKE);
        rimCirclePaint.setColor(Color.argb(0x4f, 0x33, 0x36, 0x33));
        rimCirclePaint.setStrokeWidth(0.005f);

        float rimSize = 0.02f;
        faceRect = new RectF();
        faceRect.set(rimRect.left + rimSize, rimRect.top + rimSize,
                rimRect.right - rimSize, rimRect.bottom - rimSize);

        facePaint = new Paint();
        facePaint.setFilterBitmap(true);
        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setColor(faceColor);

        scalePaint = new Paint();
        scalePaint.setStyle(Paint.Style.STROKE);
        scalePaint.setColor(scaleColor);
        scalePaint.setStrokeWidth(0.005f);
        scalePaint.setAntiAlias(true);

        scalePaint.setTextSize(0.045f);
        scalePaint.setTypeface(Typeface.SANS_SERIF);
        scalePaint.setTextScaleX(0.8f);
        scalePaint.setTextAlign(Paint.Align.CENTER);

        float scalePosition = 0.12f;
        scaleRect = new RectF();
        scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
                faceRect.right - scalePosition, faceRect.bottom - scalePosition);

        valuePaint = new TextPaint();
        valuePaint.setColor(valueColor);
        scalePaint.setTypeface(Typeface.DEFAULT_BOLD);
        valuePaint.setAntiAlias(true);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(0.1f);

        valueRectPaint = new Paint();
        valueRectPaint.setColor(Color.BLACK);
        valueRectPaint.setStyle(Paint.Style.FILL);

        if (isImageAvailable()) {
            initLogo();
        }

        handPaint = new Paint();
        handPaint.setAntiAlias(true);
        handPaint.setColor(ContextCompat.getColor(getContext(), android.R.color.black));
        handPaint.setShadowLayer(0.01f, -0.005f, -0.005f, 0x7f000000);
        handPaint.setStyle(Paint.Style.FILL);

        handPath = new Path();
        handPath.moveTo(0.5f, 0.5f + 0.2f);
        handPath.lineTo(0.5f - 0.010f, 0.5f + 0.2f - 0.007f);
        handPath.lineTo(0.5f - 0.002f, 0.5f - 0.32f);
        handPath.lineTo(0.5f + 0.002f, 0.5f - 0.32f);
        handPath.lineTo(0.5f + 0.010f, 0.5f + 0.2f - 0.007f);
        handPath.lineTo(0.5f, 0.5f + 0.2f);
        handPath.addCircle(0.5f, 0.5f, 0.025f, Path.Direction.CW);
        handPath.close();

        handScrewPaint = new Paint();
        handScrewPaint.setAntiAlias(true);
        handScrewPaint.setColor(0xff493f3c);
        handScrewPaint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setFilterBitmap(true);
    }

    private void initLogo() {
        logoPaint = new Paint();
        logoPaint.setFilterBitmap(true);
        logo = drawableToBitmap(getContext().getDrawable(logoResId));
        logoMatrix = new Matrix();
        logoScale = (1.0f / logo.getWidth()) * logoScaleFactor;
        logoMatrix.setScale(logoScale, logoScale);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
        Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int chosenWidth = chooseDimension(widthMode, widthSize);
        int chosenHeight = chooseDimension(heightMode, heightSize);

        int chosenDimension = Math.min(chosenWidth, chosenHeight);

        setMeasuredDimension(chosenDimension, chosenDimension);
    }

    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else {
            return getPreferredSize();
        }
    }

    private int getPreferredSize() {
        return 300;
    }

    private void drawRim(Canvas canvas) {
        canvas.drawOval(rimRect, rimPaint);
        canvas.drawOval(rimRect, rimCirclePaint);
    }

    private void drawFace(Canvas canvas) {
        canvas.drawOval(faceRect, facePaint);
        canvas.drawOval(faceRect, rimCirclePaint);
    }

    private void drawScale(Canvas canvas) {
        canvas.drawOval(scaleRect, scalePaint);
        canvas.save(Canvas.MATRIX_SAVE_FLAG);

        float y1 = scaleRect.top;
        float y2 = y1 - 0.020f;
        float visibleNumStep = maxValue * SUB_VALUE_LENGTH / MAX_VISIBLE_VALUES;
        float degreeStep = (float)totalDegrees / MAX_VISIBLE_VALUES;

        canvas.rotate(startingDegreeOffset, 0.5f, 0.5f);
        for (float i = 0; i < maxValue * SUB_VALUE_LENGTH; i += visibleNumStep) {
            drawScaledText(canvas, String.format(Locale.ENGLISH, "%s", (int) (i / SUB_VALUE_LENGTH)), 0.50f, y2 * 0.9f, scalePaint, 1f);
            canvas.drawLine(0.5f, y1, 0.5f, y2 * 0.93f, scalePaint);
            canvas.rotate(degreeStep, 0.5f, 0.5f);
        }
        canvas.restore();
    }

    private float valueToAngle(float value) {
        return startingDegreeOffset + value * totalDegrees / (float) maxValue;
    }

    private void drawValue(Canvas canvas) {
        String text = String.format(Locale.ENGLISH, "%2.1f", handPosition);
        float width = (float) canvas.getWidth();
        float height = (float) canvas.getHeight();
        float centerX = 0.5f * width;
        float centerY = 0.83f * height;

        Rect textBounds = new Rect();
        valuePaint.getTextBounds(text, 0, text.length(), textBounds);
        float offset = 5f;

        float textWidth = (float)textBounds.width();
        float textHeight = (float)textBounds.height();

        float x1 = centerX - textWidth * 0.5f;
        float y2 = centerY + textHeight * 0.5f;
        canvas.save();
        drawScaledText(canvas, text, x1 / width, (y2 + offset) / height, valuePaint, 1f);
        canvas.restore();
    }

    public static void drawScaledText(Canvas canvas, String text, float x, float y, Paint paint, float scale) {
        float originalStrokeWidth = paint.getStrokeWidth();
        float originalTextSize = paint.getTextSize();
        float textScaling = 10f/originalTextSize;
        paint.setStrokeWidth(originalStrokeWidth * textScaling);
        paint.setTextSize(originalTextSize * textScaling);
        canvas.save();
        canvas.scale(scale/textScaling, scale/textScaling);
        canvas.drawText(text, x * textScaling, y * textScaling, paint);
        canvas.restore();
        paint.setStrokeWidth(originalStrokeWidth);
        paint.setTextSize(originalTextSize);
    }

    private void drawLogo(Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.translate(0.5f - logo.getWidth() * logoScale / 2.0f,
                0.5f - logo.getHeight() * logoScale / 2.0f);

        canvas.drawBitmap(logo, logoMatrix, logoPaint);
        canvas.restore();
    }

    private void drawHand(Canvas canvas) {
        drawValue(canvas);
        float handAngle = valueToAngle(handPosition);
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(handAngle, 0.5f, 0.5f);
        canvas.drawPath(handPath, handPaint);
        canvas.restore();
        canvas.drawCircle(0.5f, 0.5f, 0.01f, handScrewPaint);
    }

    private void drawBackground(Canvas canvas) {
        if (background == null) {
            Log.w(TAG, "Background not created");
        } else {
            regenerateBackground();
            canvas.drawBitmap(background, 0, 0, backgroundPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);

        float scale = (float) getWidth();
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.scale(scale, scale);

        if (isImageAvailable() && logo != null) {
            drawLogo(canvas);
        }
        drawHand(canvas);

        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "Size changed to " + w + "x" + h);

        regenerateBackground();
    }

    private void regenerateBackground() {
        // free the old bitmap
        if (background != null) {
            background.recycle();
        }

        background = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas backgroundCanvas = new Canvas(background);
        float scale = (float) getWidth();
        backgroundCanvas.scale(scale, scale);

        drawRim(backgroundCanvas);
        drawFace(backgroundCanvas);
        drawScale(backgroundCanvas);
    }

    private void setValueInternal(float value) {
        if (value < minValue) {
            value = minValue;
        } else if (value > maxValue) {
            maxValue = value;
        }
        this.handPosition = value;
        invalidate();
    }

    public void setValue(float progress, long duration, long startDelay) {
        if (progress > maxValue) {
            maxValue = progress;
        }

        if (va != null) {
            va.cancel();
            va.removeAllUpdateListeners();
            va = null;
        }
        va = ValueAnimator.ofFloat(handPosition, progress);
        va.setInterpolator(new AccelerateDecelerateInterpolator());
        va.setDuration(duration);
        va.setStartDelay(startDelay);
        va.setEvaluator(new FloatEvaluator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setValueInternal((Float) valueAnimator.getAnimatedValue());
            }
        });
        va.start();
    }

    public void setValue(float progress) {
        if (Math.abs(progress - handPosition) < 0.1f) {
            return;
        }
        setValue(progress, (long)(Math.abs(progress - handPosition) * 25), 0);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private boolean isImageAvailable() {
        return logoResId != DEFAULT_IMAGE_RES_ID;
    }
}
