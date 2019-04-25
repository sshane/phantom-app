package com.smiskol.phantom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

public class CustomSeekBar extends AppCompatSeekBar {

    private Rect rect;
    private Paint paint;
    private int seekbar_height;
    private int max_size;

    public CustomSeekBar(Context context) {
        super(context);

    }

    public CustomSeekBar(Context context, AttributeSet attrs) {

        super(context, attrs);
        rect = new Rect();
        paint = new Paint();
        seekbar_height = 6;
        max_size = 200;
    }

    public CustomSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        rect.set(0 + getThumbOffset() + 5,
                (getHeight() / 2) - (seekbar_height / 2) + 1,
                getWidth() - getThumbOffset() - 5,
                (getHeight() / 2) + (seekbar_height / 2));

        paint.setColor(Color.parseColor("#CCCCCC"));
        canvas.drawRect(rect, paint);

        if (this.getProgress() > max_size/2) {
            System.out.println(getProgress());
            System.out.println(getWidth());
            System.out.println("here");
            rect.set(getWidth() / 2,
                    (getHeight() / 2) - (seekbar_height / 2) + 1,
                    (getWidth() / 2 + (getWidth() / max_size) * (getProgress() - max_size/2)) - (getProgress() / 10),
                    getHeight() / 2 + (seekbar_height / 2));

            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            canvas.drawRect(rect, paint);
        }

        if (this.getProgress() < max_size/2) {
            rect.set((getWidth() / 2 - ((getWidth() / max_size) * (max_size/2 - getProgress()))) + ((Math.abs(getProgress() - max_size/2) * 2) / 10),
                    (getHeight() / 2) - (seekbar_height / 2) + 1,
                    getWidth() / 2,
                    getHeight() / 2 + (seekbar_height / 2));

            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            canvas.drawRect(rect, paint);
        }

        super.onDraw(canvas);
    }
}