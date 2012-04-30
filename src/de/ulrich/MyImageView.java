package de.ulrich;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.ImageView;

public class MyImageView extends ImageView {
	
	public MyImageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(2.0f);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
	}

	private Paint paint;
	private float rotation;



	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

        // Construct a wedge-shaped path
		Path path = new Path();
		path.moveTo(-getWidth()/4.0f, getHeight()/4.0f);
		path.lineTo(0, -getHeight()/4.0f);
		path.lineTo(getWidth()/4.0f, getHeight()/4.0f);

        canvas.translate(getWidth()/2.0f, getHeight()/2.0f);       
        canvas.rotate(-rotation);
        
        canvas.drawPath(path, paint);
    }

	public void setOrientation(float value) {
		if (value != rotation) {
			rotation = value;
			invalidate();
		}
	}
}
