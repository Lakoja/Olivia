package de.ulrich;

import java.util.LinkedList;
import java.util.List;

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
	private List<GeoPoint> geoTargets = new LinkedList<GeoPoint>();
	private float orientation = -1;
	
	private Paint targetCirclePaint;
	private Paint targetBackPaint;
	private Paint positionCirclePaint;
	private Paint positionBackPaint;
	private Paint orientationPaint;

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
			
		orientationPaint = new Paint(positionBackPaint);
		orientationPaint.setColor(Color.argb(90, 140, 75, 0)); // brown
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
	
	public boolean hasTargets() {
		return geoTargets.size() > 0;
	}
	
	public List<GeoPoint> getTargets() {
		return geoTargets;
	}
	
	public void setTargets(List<GeoPoint> targetsNew) {
		if (!targetsEqual(targetsNew)) {
			geoTargets = targetsNew;
			requestRedraw();
		}
	}
	
	public void setOrientation(float ori) {
		if (ori != orientation) {
			boolean redraw = Math.floor(ori) != Math.floor(orientation);
			
			orientation = ori;
			
			if (redraw)
				requestRedraw();
		}
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
		
		if (hasTargets()) {
			for (GeoPoint geoTarget : geoTargets) {
				placePoint(geoTarget, false, canvas, projection, 
						targetCirclePaint, targetBackPaint);
			}
		}
		
		if (orientation >= 0) {
			placeOrientation(canvas, projection);
		}
		
		float radius = circleRadius/2;
		float x = father.getWidth() / 2.0f;
		float y = father.getHeight() / 2.0f;
		RectF centerCircle = new RectF(x-radius, y-radius, x+radius, y+radius);
		canvas.drawOval(centerCircle, positionCirclePaint);

	}


	@Override
	public int size() {
		int count = 0;
		if (geoPosition != null)
			count++;
		count += geoTargets.size();
		if (orientation >= 0)
			count++;
		count++; // map center
		return count;
	}
	
	private boolean targetsEqual(List<GeoPoint> targetsNew) {
		if (targetsNew.size() != geoTargets.size())
			return false;
		
		for (int i=0; i<geoTargets.size(); i++) {
			if (!targetsNew.get(i).equals(geoTargets.get(i)))
				return false;
		}
		
		return true;
	}

	private void placePoint(GeoPoint geo, boolean isPosition, 
			Canvas canvas, Projection projection,
			Paint foreground, Paint background) {
				
		Point targetPoint = projection.toPixels(geo, null);

		Point mapSize = new Point(father.getWidth(), father.getHeight());
		
		boolean isOutside = false;
		
		// TODO map view can change size!
		if (outside(targetPoint, mapSize)) {
			
			// is outside map area; calculate intersection with map border
			isOutside = true;
			
			targetPoint = intersectionWithBorder(targetPoint, mapSize);
		}
		
		boolean showAccuracy = false;
		
		float circleRadiusBack = circleRadius*2;
		if (isPosition && positionAccuracy > 0 && !isOutside) {
			float projectedRadius = projection.metersToPixels(
					positionAccuracy, father.getMapPosition().getZoomLevel());
			
			int quarterWidth = mapSize.x / 4;
			
			if (projectedRadius > quarterWidth)
				projectedRadius = quarterWidth;
			
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
		} else {
			RectF outerArea = new RectF(
					targetPoint.x-2*circleRadiusBack, targetPoint.y-2*circleRadiusBack, 
					targetPoint.x+2*circleRadiusBack, targetPoint.y+2*circleRadiusBack);
			
			Paint outerPaint = new Paint(background);
			outerPaint.setStrokeWidth(circleRadiusBack);
			outerPaint.setStyle(Paint.Style.STROKE);
					
			canvas.drawOval(outerArea, outerPaint);
		}
		
	}
	
	private Point intersectionWithBorder(Point targetPoint, Point mapSize) {
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
		
		return targetPoint;
	}
	

	private void placeOrientation(Canvas canvas, Projection projection) {
		Point wedgeCenter = new Point(father.getWidth() / 2, father.getHeight() / 2);
		if (geoPosition != null) {
			Point geoOnMap = projection.toPixels(geoPosition, null);
			Point mapSize = new Point(father.getWidth(), father.getHeight());
			if (!outside(geoOnMap, mapSize))
				wedgeCenter = geoOnMap;
		}
		
		float wedgeRadius = 10*circleRadius;
		
		RectF pieArea = new RectF(
				wedgeCenter.x-wedgeRadius, wedgeCenter.y-wedgeRadius, 
				wedgeCenter.x+wedgeRadius, wedgeCenter.y+wedgeRadius);
		
		// -90: 0° for drawArc is 3 o'clock
		canvas.drawArc(pieArea, orientation - 35 - 90, 70, true, orientationPaint);
	}
	
	private boolean outside(Point pos, Point size) {
		return pos.x < 0 || pos.x >= size.x || pos.y < 0 || pos.y >= size.y;
	}
}
