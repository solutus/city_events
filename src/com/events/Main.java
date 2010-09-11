package com.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class Main extends MapActivity {

	private static final double E6 = 1000000;
	private MapView mapView;

	public static final String HOST = "http://noowave.heroku.com/events.json";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//  configure map view
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.invalidate(); // update map view

		Button b = (Button) findViewById(R.id.update);
		b.setOnClickListener(new UpdateListener());
	}

	private class UpdateListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			try {
				// request events data from site
				String url = composeUrl();
				HttpGet g = new HttpGet(url);
				HttpEntity entity = new DefaultHttpClient().execute(g)
						.getEntity();
				String response = EntityUtils.toString(entity);
				
				ArrayList<Event> events = parseJSON(response);
				setOverlays(events);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}

	private ArrayList<Event> parseJSON(String str) throws JSONException {
		ArrayList<Event> events = new ArrayList<Event>();
		JSONArray json = new JSONArray(str);
		int length = json.length();
		for (int i = 0; i < length; i++) {
			Event e = new Event();
			JSONObject jEvent = json.getJSONObject(i).getJSONObject("event");
			e.latitude = Double.parseDouble(jEvent.getString("latitude"));
			e.longitude = Double.parseDouble(jEvent.getString("longitude"));
			e.title = jEvent.getString("title");
			e.description = jEvent.getString("description");
			e.date = jEvent.getString("date");
			e.address = jEvent.getString("address");
			events.add(e);
		}
		return events;

	}

	private void setOverlays(ArrayList<Event> events) {
		List<Overlay> mapOverlays = mapView.getOverlays();
		Drawable drawable = getResources().getDrawable(R.drawable.ruby);
		EventLayer layer = new EventLayer(drawable, getApplicationContext());

		for (Event e : events) {
			int lat = (int) (e.latitude * E6);
			int lon = (int) (e.longitude * E6);
			GeoPoint point = new GeoPoint(lat, lon);

			StringBuffer message = new StringBuffer(256);
			message.append("DATE:\n").append(e.date);
			message.append("\nADDRESS:\n").append(e.address);
			message.append("\nDESCRIPTION:\n").append(e.description);

			OverlayItem o = new OverlayItem(point, e.title, message.toString());
			layer.addOverlay(o);
		}
		if (layer.mOverlays.size() > 0) {
			mapOverlays.add(layer);
		}
		mapView.invalidate();  // update map view
	}

        /**
         * Composes URL based on visible maps area coordinates. 
         * @return URL
         */
		String composeUrl() {
			double latitudeSpan = mapView.getLongitudeSpan() / E6;
			double longitudeSpan = mapView.getLatitudeSpan() / E6;
			GeoPoint center = mapView.getMapCenter();
			double latitude = center.getLatitudeE6() / E6 + latitudeSpan / 2;
			double longitude = center.getLongitudeE6() / E6 - longitudeSpan / 2;
			
			StringBuffer b = new StringBuffer(256);
			b.append(HOST).append("?");
			b.append("latitude=").append(latitude);
			b.append("&longitude=").append(longitude);
			b.append("&latitude_span=").append(latitudeSpan);
			b.append("&longitude_span=").append(longitudeSpan);
			return b.toString();

		}


	private class Event {
		double latitude;
		double longitude;
		String title;
		String description;
		String date;
		String address;
	}

	void showDescription(OverlayItem item) {
		new AlertDialog.Builder(this).setTitle(item.getTitle()).setMessage(
				item.getSnippet()).setNeutralButton("Cancel", null).show();
	}

	private class EventLayer extends ItemizedOverlay<OverlayItem> {
		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

		@Override
		protected boolean onTap(int index) {
			OverlayItem item = mOverlays.get(index);
			showDescription(item);
			return true;
		}

		public EventLayer(Drawable defaultMarker, Context context) {
			super(boundCenterBottom(defaultMarker));
		}

		public void addOverlay(OverlayItem overlay) {
			mOverlays.add(overlay);
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}