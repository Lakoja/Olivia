package de.ulrich;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.Display;

public class PositionOverlay extends ItemizedOverlay<OverlayItem> {
	
	private int dpi;
	private float circleRadius;
	private MapView father;
	private GeoPoint geoPosition;
	private float positionAccuracy;
	private GeoPoint geoTarget;
	private Paint targetCirclePaint;
	private Paint targetBackPaint;
	private Paint positionCirclePaint;
	private Paint positionBackPaint;

	public PositionOverlay(Display display, MapView mv) {
		super(null);
		
		father = mv;
		
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		dpi = metrics.densityDpi;
			
		circleRadius = (dpi / 100.0f) * 2.5f; // radius 2.5 on 100dpi device
		if (circleRadius < 2)
			circleRadius = 2;
		
		float strokeWidth = (dpi / 100.0f) * 1;
		if (strokeWidth < 1)
			strokeWidth = 1;
				
		targetCirclePaint = new Paint();
		targetCirclePaint.setColor(Color.RED);
		targetCirclePaint.setStrokeWidth(strokeWidth);
		targetCirclePaint.setAntiAlias(true);
		targetCirclePaint.setStyle(Paint.Style.STROKE);
		
		targetBackPaint = new Paint(targetCirclePaint);
		targetBackPaint.setColor(Color.argb(90, 255, 0, 0));
		targetBackPaint.setStyle(Paint.Style.FILL);
		
		positionCirclePaint = new Paint(targetCirclePaint);
		positionCirclePaint.setColor(Color.BLACK);
		
		positionBackPaint = new Paint(targetBackPaint);
		positionBackPaint.setColor(Color.argb(90, 0, 0, 0));
		
	}
	
	public GeoPoint getPosition() {
		return geoPosition;
	}

	public void setPosition(GeoPoint gp, float accuracyMeters) {
		boolean redraw = false;
		if (!gp.equals(geoPosition))
			redraw = true;
		
		geoPosition = gp;
		positionAccuracy = accuracyMeters;
		
		if (redraw)
			requestRedraw();
	}
	
	public GeoPoint getTarget() {
		return geoTarget;
	}
	
	public void setTarget(GeoPoint gp) {
		boolean redraw = false;
		if (!gp.equals(geoTarget))
			redraw = true;
		
		geoTarget = gp;
		
		if (redraw)
			requestRedraw();
	}

	@Override
	protected OverlayItem createItem(int idx) {
		return new OverlayItem();
	}
	
	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, 
			Projection projection, byte drawZoomLevel) {
		
		if (geoPosition != null) {
			placePoint(geoPosition, true, canvas, projection, 
					positionCirclePaint, positionBackPaint);
		}
		
		if (geoTarget != null) {
			placePoint(geoTarget, false, canvas, projection, 
					targetCirclePaint, targetBackPaint);
		}
	}

	@Override
	public int size() {
		int count = 0;
		if (geoPosition != null)
			count++;
		if (geoTarget != null)
			count++;
		return count;
	}

	private void placePoint(GeoPoint geo, boolean isPosition, 
			Canvas canvas, Projection projection,
			Paint foreground, Paint background) {
				
		Point targetPoint = projection.toPixels(geo, null);

		Point mapSize = new Point(father.getWidth(), father.getHeight()); 
		
		boolean isOutside = false;
		
		// TODO map view can change size!
		if (targetPoint.x < 0 || targetPoint.x >= mapSize.x
				|| targetPoint.y < 0 || targetPoint.y >= mapSize.y) {
			
			// is outside map area; calculate intersection with map border
			isOutside = true;
			
			float centerx = mapSize.x / 2.0f;
			float centery = mapSize.y / 2.0f;
			
			float diffx = targetPoint.x - centerx;
			float diffy = targetPoint.y - centery;
			
			if (diffx == 0) {
				targetPoint.x = (int)centerx;
			
				if (diffy < 0) // above				
					targetPoint.y = 0;
				else
					targetPoint.y = mapSize.y;
			} else if (diffy == 0) {
				targetPoint.y = (int)centery;
				
				if (diffx > 0) // right
					targetPoint.x = mapSize.x;
				else
					targetPoint.x = 0;				
			} else {			
				float ascent = diffy / diffx;
			
				// Intersection with horizontal map borders?
				float interx = centery / ascent;
				if (Math.abs(interx) <= centerx) {
					// is really more above/below than to the side
					
					if (diffy < 0) {// above
						targetPoint.x = (int)(centerx - interx);
						targetPoint.y = 0;
					} else {
						targetPoint.x = (int)(centerx + interx);
						targetPoint.y = mapSize.y;
					}
				} else {
					float intery = centerx * ascent;
					
					if (diffx < 0) {
						targetPoint.x = 0;
						targetPoint.y = (int)(centery - intery);
					} else {
						targetPoint.x = mapSize.x;
						targetPoint.y = (int)(centery + intery);
					}
				}
			}			
		}
		
		boolean showAccuracy = false;
		
		float circleRadiusBack = circleRadius*2;
		if (isPosition && positionAccuracy > 0 && !isOutside) {
			float projectedRadius = projection.metersToPixels(
					positionAccuracy, father.getMapPosition().getZoomLevel());
			if (projectedRadius > circleRadius) {
				circleRadiusBack = projectedRadius;
				showAccuracy = true;
			}
		}
		
		RectF circleAreaWider = new RectF(
				targetPoint.x-circleRadiusBack, targetPoint.y-circleRadiusBack, 
				targetPoint.x+circleRadiusBack, targetPoint.y+circleRadiusBack);
		canvas.drawOval(circleAreaWider, background);
		
		float circleRadiusFront = circleRadius;
		if (showAccuracy)
			circleRadiusFront = circleRadiusBack;
		
		if (!isOutside) {
			RectF circleArea = new RectF(
					targetPoint.x-circleRadiusFront, targetPoint.y-circleRadiusFront, 
					targetPoint.x+circleRadiusFront, targetPoint.y+circleRadiusFront);
			
			if (isPosition) {
				canvas.drawOval(circleArea, foreground);
			} else {
				// is target			
				canvas.drawLine(circleArea.left, circleArea.top, 
						circleArea.right, circleArea.bottom, foreground);
				canvas.drawLine(circleArea.right, circleArea.top, 
						circleArea.left, circleArea.bottom, foreground);
			}
		}
		
	}
}
