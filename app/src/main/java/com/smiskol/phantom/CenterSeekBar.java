package com.smiskol.phantom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

public class CenterSeekBar extends AppCompatSeekBar {

    private Rect rect;
    private Paint paint;
    private int seekbar_height;

    public CenterSeekBar(Context context) {
        super(context);

    }

    public CenterSeekBar(Context context, AttributeSet attrs) {

        super(context, attrs);
        rect = new Rect();
        paint = new Paint();
        seekbar_height = 6;
    }

    public CenterSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        rect.set(getThumbOffset() + 5,
                (getHeight() / 2) - (seekbar_height / 2) + 1,
                getWidth() - getThumbOffset() - 5,
                (getHeight() / 2) + (seekbar_height / 2));

        paint.setColor(Color.parseColor("#e2e2e2"));
        canvas.drawRect(rect, paint);

        if (this.getProgress() > getMax() / 2) {
            rect.set(getWidth() / 2,
                    (getHeight() / 2) - (seekbar_height / 2) + 1,
                    interp(getProgress(), getMax()/2, getMax(), getWidth()/2, getWidth()-25),
                    getHeight() / 2 + (seekbar_height / 2));

            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            canvas.drawRect(rect, paint);
        }

        if (this.getProgress() < getMax() / 2) {
            rect.set(interp(getProgress(), getMax()/2, 0, getWidth()/2, 25),
                    (getHeight() / 2) - (seekbar_height / 2) + 1,
                    getWidth() / 2,
                    getHeight() / 2 + (seekbar_height / 2));

            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            canvas.drawRect(rect, paint);
        }

        super.onDraw(canvas);
    }
    public Integer interp(int value, int from1, int to1, int from2, int to2) {
        return (int) Math.round(((Double.valueOf(value)) - (Double.valueOf(from1))) / ((Double.valueOf(to1)) - (Double.valueOf(from1))) * ((Double.valueOf(to2)) - (Double.valueOf(from2))) + (Double.valueOf(from2)));
    }
}