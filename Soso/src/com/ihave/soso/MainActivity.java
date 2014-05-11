package com.ihave.soso;

import java.util.ArrayList;

import com.ihave.service.ServletService;
import com.tencent.mapapi.map.GeoPoint;
import com.tencent.mapapi.map.LocationListener;
import com.tencent.mapapi.map.LocationManager;
import com.tencent.mapapi.map.MapActivity;
import com.tencent.mapapi.map.MapController;
import com.tencent.mapapi.map.MapView;
import com.tencent.mapapi.map.Overlay;
import com.tencent.mapapi.map.Projection;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends MapActivity {
	MapView mMapView = null;
	MapController mMapController;
	LocationManager locManager = null;
	LocationListener locListener = null;
	Button btnLocationManger = null;
	TextView textView = null;
	LocationOverlay locMyOverlay = null;
	MyPathOverlay myPathOverlay = null;
	public ArrayList<GeoPoint> traceOfMe = new ArrayList<GeoPoint>(60);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mMapView = (MapView) findViewById(R.id.map);
		mMapView.setBuiltInZoomControls(true);
		mMapController = mMapView.getController();
		final GeoPoint pointcenter = new GeoPoint((int) (27.641 * 1E6),
				(int) (113.868 * 1E6));
		mMapController.setCenter(pointcenter);
		mMapController.setZoom(15);
		textView = (TextView) findViewById(R.id.trackinfo);
		btnLocationManger = (Button) this.findViewById(R.id.locButton);
		btnLocationManger.getBackground().setAlpha(100);
		btnLocationManger.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (locManager == null) {
					locManager = LocationManager.getInstance();

					locManager.requestLocationUpdates(locListener);
					locManager.enableProvider(MainActivity.this);
				}
			}
		});

		locListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				if (location == null) {
					return;
				}
				if (locMyOverlay == null) {
					// ����һ��λͼ����
					Bitmap bmpMarker = null;
					Resources res = MainActivity.this.getResources();
					// ��Resources���getResources()������ȡres�ļ����µ�R.drawable.XXX��λͼ�ļ�����bmpMarker
					bmpMarker = BitmapFactory.decodeResource(res,
							R.drawable.mark_location);
					// locMyOverlay
					// ��д��Overlay�࣬��bmpMarkerλͼ����Overlay�����ڵ�ͼ�ϼ������Overlay
					locMyOverlay = new LocationOverlay(bmpMarker);
					mMapView.getOverlays().add(locMyOverlay);
				}
				double iLongi = location.getLongitude();
				double iLatitu = location.getLatitude();
				// ����д��Overlay�����setGeoCoords()���������ݸ����overlayλ����Ϣ���Լ�setAccuracy()�������ݾ�ȷ��Χ
				locMyOverlay.setGeoCoords(iLongi, iLatitu);
				locMyOverlay.setAccuracy(location.getAccuracy());
				mMapView.invalidate();
				//����γ�ȷ���������servlet
				try {
					ServletService.sendRequest(iLatitu,iLongi);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				GeoPoint geoAnimationTo = new GeoPoint((int) (iLatitu * 1e6),
						(int) (iLongi * 1e6));
				// ����ǰ���µ�λ�ô洢���켣 �����б��� �洢ΪGeoPoint��
				traceOfMe.add(geoAnimationTo);
				mMapView.getController().animateTo(geoAnimationTo);
			}
		};
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (locManager != null) {
			locManager.removeUpdates(locListener);
			locManager.disableProvider();
			locManager.release();
			locManager = null;
		}
		if (locMyOverlay != null) {
			this.mMapView.getOverlays().remove(locMyOverlay);
		}
		super.onDestroy();
	}

	// �ڵ�ͼ�����λ��ͼ��-Բ��;���Ȧ��
	class LocationOverlay extends Overlay {

		GeoPoint geoPoint;
		Bitmap bmpMarker;
		float fAccuracy = 0f;

		public LocationOverlay(Bitmap mMarker) {
			bmpMarker = mMarker;
		}

		public void setGeoCoords(double dLongti, double dLatitu) {
			if (geoPoint == null) {
				geoPoint = new GeoPoint((int) (dLatitu * 1E6),
						(int) (dLongti * 1E6));
			} else {
				geoPoint.setLatitudeE6((int) (dLatitu * 1E6));
				geoPoint.setLongitudeE6((int) (dLongti * 1E6));
			}
		}

		public void setAccuracy(float fAccur) {
			fAccuracy = fAccur;
		}

		// ��дOverlay��draw()����������һ��Ȧ,���һ�����λ��ͼ
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			if (geoPoint == null) {
				return;
			}
			Projection mapProjection = mapView.getProjection();
			Paint paint = new Paint();
			// �Ѹ�����GeoPoint�任�������MapView���Ͻǵ���Ļ��������㡣
			Point ptLocataion = mapProjection.toPixels(geoPoint, null);
			paint.setColor(Color.BLUE);
			// �趨��λ���µľ��ȷ�Χ��͸���ȺͿ����,�뾶ȡLocationListener��location.getAccuracy()�ľ���
			// ����LocationOverlay.setAccuracy(float fAccur)���ݣ� ���������ȷ�Χ.
			paint.setAlpha(7);
			paint.setAntiAlias(true);
			float fRadius = mapProjection.metersToEquatorPixels(fAccuracy);
			canvas.drawCircle(ptLocataion.x, ptLocataion.y, fRadius, paint);
			// �趨��Ȧ�Ƿ��������ߣ���͸���Ķȣ�������Ȧ���
			// paint.setStyle(Style.STROKE);
			// paint.setAlpha(1);
			// canvas.drawCircle(ptLocataion.x, ptLocataion.y, fRadius, paint);
			// �����λ���λͼ��Ϊ�գ����ڶ�λ�㻭����λ��λͼ,������켣
			if (bmpMarker != null) {
				paint.setAlpha(255);
				canvas.drawBitmap(bmpMarker,
						ptLocataion.x - bmpMarker.getWidth() / 2, ptLocataion.y
								- bmpMarker.getHeight() / 2, paint);
			}
			super.draw(canvas, mapView, shadow);
		}
	}

	// �켣Overlay
	class MyPathOverlay extends Overlay {
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
				long when) {
			// TODO Auto-generated method stub
			Path tracePath = new Path();
			GeoPoint geoPt = null;
			int iPtSize = traceOfMe.size();
			Point ptPixel = null;
			for (int i = 0; i < iPtSize; i++) {
				geoPt = traceOfMe.get(i);
				if (geoPt == null) {
					continue;
				}
				ptPixel = mMapView.getProjection().toPixels(geoPt, null);
				if (ptPixel == null) {
					continue;
				}
				if (i == 0) {
					tracePath.moveTo(ptPixel.x, ptPixel.y);
				} else {
					tracePath.lineTo(ptPixel.x, ptPixel.y);
				}
			}

			Paint pathPaint = new Paint();
			pathPaint.setStyle(Style.STROKE);
			pathPaint.setStrokeWidth(6);
			pathPaint.setARGB(50, 10, 128, 128);
			pathPaint
					.setPathEffect(new ComposePathEffect(new DashPathEffect(
							new float[] { 10, 8, 8, 8 }, 50),
							new CornerPathEffect(100)));
			pathPaint.setAntiAlias(true);

			canvas.drawPath(tracePath, pathPaint);

			return super.draw(canvas, mapView, shadow, when);
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			// TODO Auto-generated method stub
			super.draw(canvas, mapView, shadow);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.record:
			if (myPathOverlay == null) {
				mMapView.getOverlays().add(myPathOverlay);
			}
			StringBuffer stringText = new StringBuffer(256);
			GeoPoint geoPiont = null;
			int iPtSize = traceOfMe.size();
			for (int i = 0; i < iPtSize; i++) {
				geoPiont = traceOfMe.get(i);
				stringText.append(geoPiont.toString() + "\n");
			}
			textView.setText(stringText.toString());
			break;
		case R.id.quit:
			finish();
			break;
		}
		return true;
	}

}
