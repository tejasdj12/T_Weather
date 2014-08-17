package vu.de.tejasjadhav.tweather;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;


public class MainActivity extends ActionBarActivity {
	public static String OPEN_WEATHER_APPID = "";
	public static String OPEN_WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather?APPID=" + OPEN_WEATHER_APPID + "&id=";
	public static String OPEN_WEATHER_SEARCH_URL = "http://api.openweathermap.org/data/2.5/find?type=like&APPID=" + OPEN_WEATHER_APPID + "&q=";

	private TextView lblCurrentCity;
	private TextView lblLastUpdated;

	private TextView lblWeatherMain;
	private TextView lblWeatherDesc;
	private ImageView imgWeatherStatus;

	private TextView lblCurrentTemperature;
	private TextView lblFeelsLikeTemperature;

	private TextView lblWindSpeed;
	private TextView lblWindDirection;

	private SharedPreferences preferences;

	private MenuItem refreshMenu;
	private int cityId;
	private String currentCity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		long lastUpdatedDate = preferences.getLong("last_updated", 0);
		cityId = preferences.getInt("city_id", 0);
		currentCity = preferences.getString("city_name", "");

		if (cityId == 0 || currentCity.equals("")) {
			Intent selectCityIntent = new Intent(this, SetCityActivity.class);
			startActivityForResult(selectCityIntent, 100);
		}

		lblCurrentCity = (TextView) findViewById(R.id.lblCurrentCity);
		lblCurrentCity.setText(currentCity);
		lblLastUpdated = (TextView) findViewById(R.id.lblLastUpdated);
		lblLastUpdated.setText("Last Updated: " + timestampToFriendly(lastUpdatedDate));

		lblWeatherMain = (TextView) findViewById(R.id.lblWeatherMain);
		lblWeatherDesc = (TextView) findViewById(R.id.lblWeatherDesc);
		imgWeatherStatus = (ImageView) findViewById(R.id.imgWeatherStatus);

		lblCurrentTemperature = (TextView) findViewById(R.id.lblCurrentTemp);
		lblFeelsLikeTemperature = (TextView) findViewById(R.id.lblFeelsLikeTemp);

		lblWindDirection = (TextView) findViewById(R.id.lblWindDirection);
		lblWindSpeed = (TextView) findViewById(R.id.lblWindSpeed);

		Typeface thinType = Typeface.createFromAsset(getAssets(), "fonts/thin.ttf");
		Typeface lightType = Typeface.createFromAsset(getAssets(), "fonts/light.ttf");

		lblWeatherMain.setTypeface(thinType);
		lblWeatherDesc.setTypeface(lightType);
		lblCurrentTemperature.setTypeface(thinType);
		lblFeelsLikeTemperature.setTypeface(lightType);
		lblCurrentCity.setTypeface(lightType);
		lblLastUpdated.setTypeface(lightType);
		lblWindSpeed.setTypeface(thinType);
		lblWindDirection.setTypeface(lightType);

		final Handler mHandler = new Handler();
		final Runnable updateMinutes = new Runnable() {
			@Override
			public void run() {
				long lastUpdatedLong = preferences.getLong("last_updated", 0);
				lblLastUpdated.setText("Last updated " + timestampToFriendly(lastUpdatedLong));
				mHandler.postDelayed(this, 60000);
			}
		};

		mHandler.postDelayed(updateMinutes, 0);
	}

	public void startRefreshAnimationActionBar() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		ImageView refreshButton = (ImageView) inflater.inflate(R.layout.clockwise_refresh, null);

		Animation clockwiseRotation = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotate);
		clockwiseRotation.setRepeatCount(Animation.INFINITE);
		refreshButton.startAnimation(clockwiseRotation);

		MenuItemCompat.setActionView(refreshMenu, refreshButton);
	}

	public void stopRefreshAnimationActionBar() {
		MenuItemCompat.getActionView(refreshMenu).clearAnimation();
		MenuItemCompat.setActionView(refreshMenu, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		refreshMenu = menu.findItem(R.id.action_refresh);

		new fetchWeatherTask(this, cityId).execute();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_refresh) {
			new fetchWeatherTask(this, cityId).execute();
			return true;
		} else if (id == R.id.action_select_city) {
			Intent selectCityIntent = new Intent(this, SetCityActivity.class);
			startActivityForResult(selectCityIntent, 100);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case 100:
				if (resultCode == RESULT_OK) {
					cityId = data.getIntExtra("city_id", 0);
					String cityName = data.getStringExtra("city_name");

					lblCurrentCity.setText(cityName);
					new fetchWeatherTask(this, cityId).execute();
				} else if (resultCode == RESULT_CANCELED) {
					if (cityId == 0 && currentCity.equals("")) {
						finish();
					}
				}
		}
	}

	public boolean isNight() {
		Calendar calendar = Calendar.getInstance();
		int hour = calendar.get(Calendar.HOUR_OF_DAY);

		return (hour < 5 || hour > 18);
	}

	public Drawable getWeatherIcon(int status) throws IOException {
		String drawableName;

		switch (status) {

			// Hail
			case 906:
				drawableName = "hail.png";
				break;

			// Few Clouds
			case 801:
				drawableName = !isNight() ? "cloudy2.png" : "cloudy2_night.png";
				break;

			// Scattered Clouds
			case 802:
				drawableName = !isNight() ? "cloudy3.png" : "cloudy3_night.png";
				break;

			// Broken Clouds
			case 803:
				drawableName = !isNight() ? "cloudy4.png" : "cloudy4_night.png";
				break;

			// Overcast
			case 804:
				drawableName = "overcast.png";
				break;

			// Mist, Smoke, Haze, Sand
			case 701:
			case 711:
			case 721:
			case 731:
				drawableName = !isNight() ? "mist.png" : "mist_night.png";
				break;

			// Fog, Sand, Dust
			case 741:
			case 751:
			case 761:
				drawableName = !isNight() ? "fog.png" : "fog_night.png";
				break;

			// Light Snow
			case 600:
			case 620:
				drawableName = !isNight() ? "snow1.png" : "snow1_night.png";
				break;

			// Snow Shower
			case 621:
				drawableName = !isNight() ? "snow2.png" : "snow2_night.png";
				break;

			// Heavy Snow Shower
			case 622:
				drawableName = !isNight() ? "snow3.png" : "snow3_night.png";
				break;

			// Snow
			case 601:
				drawableName = "snow4.png";
				break;

			// Heavy Snow
			case 602:
				drawableName = "snow5.png";
				break;

			// Sleet
			case 611:
			case 612:
			case 615:
			case 616:
				drawableName = "sleet.png";
				break;

			// Light Rain
			case 300:
			case 310:
			case 500:
			case 520:
				drawableName = !isNight() ? "shower1.png" : "shower1_night.png";
				break;

			// Moderate Rain
			case 301:
			case 311:
			case 313:
			case 501:
			case 521:
				drawableName = !isNight() ? "shower2.png" : "shower2_night.png";
				break;

			// Heavy Rain
			case 321:
			case 502:
			case 522:
				drawableName = "light_rain.png";
				break;

			// Extreme Rain
			case 302:
			case 312:
			case 503:
			case 504:
			case 511:
			case 531:
				drawableName = "shower3.png";
				break;

			// Light Thunderstorm
			case 200:
			case 210:
			case 230:
				drawableName = !isNight() ? "tstorm1.png" : "tstorm1_night.png";
				break;

			// Moderate Thunderstorm
			case 201:
			case 211:
			case 221:
			case 231:
				drawableName = !isNight() ? "tstorm2.png" : "tstorm2_night.png";
				break;

			// Heavy Thunderstorm
			case 202:
			case 212:
			case 232:
				drawableName = "tstorm3.png";
				break;

			case 800:
			default:
				drawableName = !isNight() ? "sunny.png" : "sunny_night.png";
		}

		return Drawable.createFromStream(getAssets().open("icons/" + drawableName), null);
	}

	public String timestampToFriendly(long timestamp) {
		String unit = "m";

		long differenceInMins = ((System.currentTimeMillis() - timestamp) / 60000);

		if (differenceInMins > 59) {
			differenceInMins = differenceInMins % 60;
			unit = "h";
		}

		if (differenceInMins > 1439) {
			differenceInMins = differenceInMins % 1440;
			unit = "d";
		}

		if (differenceInMins > 10079) {
			differenceInMins = differenceInMins % 10080;
			unit = "w";
		}

		return String.valueOf((differenceInMins == 0) ? "just now" : differenceInMins + unit + " ago");
	}

	public String capitalize(String s) {
		StringBuilder result = new StringBuilder(s.length());
		String[] words = s.split("\\s");
		for (int i = 0, l = words.length; i < l; ++i) {
			if (i > 0) result.append(" ");
			result.append(Character.toUpperCase(words[i].charAt(0)))
					.append(words[i].substring(1));
		}

		return result.toString();
	}

	public int feelsLikeTemperature(int temp, float windspeed, int humidity) {
		double vaporPressure = (humidity / 100) * 6.105 * Math.exp((17.27 * temp) / (237.7 + temp));

		return temp + ((int) (0.33 * vaporPressure)) - ((int) (0.7 * windspeed)) - 4;
	}

	public String degreesToDirection(double degrees) {
		String direction = "N/A";

		if (degrees >= 337.5 || degrees < 22.5) {
			direction = "North";
		} else if (degrees >= 22.5 && degrees < 67.5) {
			direction = "North East";
		} else if (degrees >= 67.5 && degrees < 112.5) {
			direction = "East";
		} else if (degrees >= 112.5 && degrees < 157.5) {
			direction = "South East";
		} else if (degrees >= 157.5 && degrees < 202.5) {
			direction = "South";
		} else if (degrees >= 202.5 && degrees < 247.5) {
			direction = "South West";
		} else if (degrees >= 247.5 && degrees < 292.5) {
			direction = "West";
		} else if (degrees >= 292.5 && degrees < 337.5) {
			direction = "North West";
		}

		return direction;
	}

	private class fetchWeatherTask extends AsyncTask {

		private Context mContext;
		private int mCity;

		public fetchWeatherTask(Context context, int city) {
			mContext = context;
			mCity = city;
		}

		@Override
		public void onPreExecute() {
			startRefreshAnimationActionBar();
		}

		@Override
		protected Object doInBackground(Object[] objects) {

			String serverResponse;

			HttpClient httpClient = new DefaultHttpClient();
			String url = OPEN_WEATHER_URL + mCity;

			HttpGet httpGet = new HttpGet(url);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();

			try {
				serverResponse = httpClient.execute(httpGet, responseHandler);
			} catch (IOException e) {
				Log.d("Server response error", e.getMessage());
				return "0";
			}

			return serverResponse;
		}

		@Override
		public void onPostExecute(Object data) {
			stopRefreshAnimationActionBar();

			try {
				if (Integer.parseInt(data.toString()) == 0) {
					Toast.makeText(mContext, "Refresh failed!", Toast.LENGTH_SHORT).show();
					return;
				}
			} catch (NumberFormatException ignored) {
			}

			try {
				JSONObject serverResponse = new JSONObject(data.toString());
				JSONObject basicWeather = serverResponse.getJSONObject("main");

				JSONArray weatherDataArray = serverResponse.getJSONArray("weather");

				JSONObject weatherData = weatherDataArray.getJSONObject(0);

				int currentTemp = (int) (basicWeather.getDouble("temp") - 273.15);
				double windSpeed = serverResponse.getJSONObject("wind").getDouble("speed");
				double windDirection = serverResponse.getJSONObject("wind").getDouble("deg");

				int humidity = basicWeather.getInt("humidity");

				int weatherStatusCode = weatherData.getInt("id");
				String weatherMain = capitalize(weatherData.getString("main"));
				String weatherDesc = capitalize(weatherData.getString("description"));

				lblWeatherMain.setText(weatherMain);
				lblWeatherDesc.setText(weatherDesc);
				lblCurrentTemperature.setText(currentTemp + "°C");

				lblFeelsLikeTemperature.setText("Feels like " + feelsLikeTemperature(currentTemp, (float) windSpeed, humidity) + " °C");

				lblWindSpeed.setText(((int) (windSpeed * 3.6)) + " km/h");
				lblWindDirection.setText("Blowing in " + degreesToDirection(windDirection) + " direction");

				long timestamp = System.currentTimeMillis();
				preferences.edit().putLong("last_updated", timestamp).apply();
				lblLastUpdated.setText("Last updated " + timestampToFriendly(timestamp));

				try {
					imgWeatherStatus.setImageDrawable(getWeatherIcon(weatherStatusCode));
				} catch (IOException e) {
					Log.d("Weather icon error", e.getMessage());
				}
			} catch (JSONException e) {
				Log.d("JSON error", e.getMessage());
			}
		}
	}
}
