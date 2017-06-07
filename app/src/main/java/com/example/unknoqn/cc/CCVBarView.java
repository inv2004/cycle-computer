package com.example.unknoqn.cc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by unknoqn on 5/24/2017.
 */

public class CCVBarView extends View {
    int v;
    int prev_v;
    AttributeSet attrs;

    public CCVBarView(Context context, AttributeSet g_attrs) {
        super(context, g_attrs);
        attrs = g_attrs;
        v = 100;
    }

    public void setValue(int g_v) {
        Log.d("VBar: value", String.valueOf(g_v));
        prev_v = v;
        v = g_v;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        String str = super.isEnabled() ? String.format("%02d", v) : "n/a";
        Paint p1 = new Paint();
        Rect bounds1 = new Rect();
        p1.setTextAlign(Paint.Align.CENTER);
        float scaledSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                35, getResources().getDisplayMetrics());
        p1.setTextSize(scaledSizeInPixels);
        p1.getTextBounds(str, 0, str.length(), bounds1);
        int str_h = bounds1.height();

        int s;
            if(v > 0) {
                s = v * (h - str_h) / 100;
            } else {
                s = +10;
            }

        p1.setColor(Color.WHITE);
        canvas.drawText(str, w/2, h-s+10, p1);

/*        if (prev_v > v) {
            canvas.drawText("v", 40, canvas.getHeight() - size + 60 + 50, p);
        }
*/
        Paint p2 = new Paint();
        p2.setColor(Color.BLUE);
        canvas.drawRect(20 * w / 100, h-s+20, 80 * w / 100, h, p2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec) - 20;
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec) - 20;
        this.setMeasuredDimension(parentWidth, parentHeight);
//        this.setLayoutParams(new LinearLayout.LayoutParams(parentWidth / 2, parentHeight / 2));
//        super.onMeasure(widthMeasureSpec / 2, heightMeasureSpec / 2);
    }
}