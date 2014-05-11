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
					// 定义一个位图对象
					Bitmap bmpMarker = null;
					Resources res = MainActivity.this.getResources();
					// 用Resources类的getResources()方法调取res文件夹下的R.drawable.XXX的位图文件传给bmpMarker
					bmpMarker = BitmapFactory.decodeResource(res,
							R.drawable.mark_location);
					// locMyOverlay
					// 重写了Overlay类，将bmpMarker位图传给Overlay，并在地图上加载这个Overlay
					locMyOverlay = new LocationOverlay(bmpMarker);
					mMapView.getOverlays().add(locMyOverlay);
				}
				double iLongi = location.getLongitude();
				double iLatitu = location.getLatitude();
				// 用重写的Overlay对象的setGeoCoords()方法，传递给这个overlay位置信息，以及setAccuracy()方法传递精确范围
				locMyOverlay.setGeoCoords(iLongi, iLatitu);
				locMyOverlay.setAccuracy(location.getAccuracy());
				mMapView.invalidate();
				//将经纬度发给服务器servlet
				try {
					ServletService.sendRequest(iLatitu,iLongi);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				GeoPoint geoAnimationTo = new GeoPoint((int) (iLatitu * 1e6),
						(int) (iLongi * 1e6));
				// 将当前更新的位置存储到轨迹 数据列表中 存储为GeoPoint类
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

	// 在地图上添加位置图层-圆点和精度圈。
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

		// 复写Overlay的draw()方法，来画一个圈,并且画出定位点图
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			if (geoPoint == null) {
				return;
			}
			Projection mapProjection = mapView.getProjection();
			Paint paint = new Paint();
			// 把给定的GeoPoint变换到相对于MapView左上角的屏幕像素坐标点。
			Point ptLocataion = mapProjection.toPixels(geoPoint, null);
			paint.setColor(Color.BLUE);
			// 设定定位点下的精度范围的透明度和抗锯齿,半径取LocationListener中location.getAccuracy()的精度
			// （用LocationOverlay.setAccuracy(float fAccur)传递） 并画出精度范围.
			paint.setAlpha(7);
			paint.setAntiAlias(true);
			float fRadius = mapProjection.metersToEquatorPixels(fAccuracy);
			canvas.drawCircle(ptLocataion.x, ptLocataion.y, fRadius, paint);
			// 设定外圈是非填充的留边，和透明的度，并画出圈外边
			// paint.setStyle(Style.STROKE);
			// paint.setAlpha(1);
			// canvas.drawCircle(ptLocataion.x, ptLocataion.y, fRadius, paint);
			// 如果定位点的位图不为空，则在定位点画出定位点位图,并描出轨迹
			if (bmpMarker != null) {
				paint.setAlpha(255);
				canvas.drawBitmap(bmpMarker,
						ptLocataion.x - bmpMarker.getWidth() / 2, ptLocataion.y
								- bmpMarker.getHeight() / 2, paint);
			}
			super.draw(canvas, mapView, shadow);
		}
	}

	// 轨迹Overlay
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
