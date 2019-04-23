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
    private Paint paint ;
    private int seekbar_height;

    public CustomSeekBar(Context context) {
        super(context);

    }

    public CustomSeekBar(Context context, AttributeSet attrs) {

        super(context, attrs);
        rect = new Rect();
        paint = new Paint();
        seekbar_height = 6;
    }

    public CustomSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {

        rect.set(0 + getThumbOffset()+5,
                (getHeight() / 2) - (seekbar_height/2)-2,
                getWidth()- getThumbOffset()-5,
                (getHeight() / 2) + (seekbar_height/2));

        paint.setColor(Color.parseColor("#CCCCCC"));

        canvas.drawRect(rect, paint);



        if (this.getProgress() > 50) {


            rect.set(getWidth() / 2,
                    (getHeight() / 2) - (seekbar_height/2),
                    getWidth() / 2 + (getWidth() / 100) * (getProgress() - 50),
                    getHeight() / 2 + (seekbar_height/2));

            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            canvas.drawRect(rect, paint);

        }

        if (this.getProgress() < 50) {

            rect.set(getWidth() / 2 - ((getWidth() / 100) * (50 - getProgress())),
                    (getHeight() / 2) - (seekbar_height/2),
                    getWidth() / 2,
                    getHeight() / 2 + (seekbar_height/2));

            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            canvas.drawRect(rect, paint);

        }

        super.onDraw(canvas);
    }
}