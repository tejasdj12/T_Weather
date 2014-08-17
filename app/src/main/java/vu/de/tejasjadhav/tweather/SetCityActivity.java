package vu.de.tejasjadhav.tweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
import java.net.URLEncoder;
import java.util.ArrayList;

public class SetCityActivity extends ActionBarActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
	private SharedPreferences preferences;

	private EditText txtCityName;
	private Button btnSearchCity;

	private Typeface lightType;

	private CityListAdapter adapter;
	private ArrayList<CityModel> cityList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_city_actvity);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		txtCityName = (EditText) findViewById(R.id.txtCityName);
		btnSearchCity = (Button) findViewById(R.id.btnSearchCity);
		ListView lstCityList = (ListView) findViewById(R.id.lstCityList);

		lightType = Typeface.createFromAsset(getAssets(), "fonts/light.ttf");

		txtCityName.setTypeface(lightType);
		btnSearchCity.setTypeface(lightType);

		cityList = new ArrayList<CityModel>();
		adapter = new CityListAdapter(cityList);

		lstCityList.setAdapter(adapter);
		lstCityList.setOnItemClickListener(this);

		btnSearchCity.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if (view == btnSearchCity) {
			new CitySearchTask(this, txtCityName.getText().toString()).execute();
			txtCityName.setText("");
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		int cityId = cityList.get(i).cityId;
		String cityName = cityList.get(i).cityName;

		preferences.edit().putInt("city_id", cityId).putString("city_name", cityName).apply();

		Bundle result = new Bundle();
		result.putInt("city_id", cityId);
		result.putString("city_name", cityName);

		Intent data = new Intent();
		data.putExtras(result);

		setResult(RESULT_OK, data);
		finish();
	}

	public static class CityModel {
		public String country;
		public String cityName;
		public int cityId;

		public CityModel(int id, String name, String cnt) {
			cityId = id;
			cityName = name;
			country = cnt;
		}
	}

	public class CitySearchTask extends AsyncTask {
		private Context mContext;
		private ProgressDialog progressDialog;
		private String mCityName;

		public CitySearchTask(Context context, String cityName) {
			mContext = context;
			mCityName = cityName;
		}

		public void onPreExecute() {
			progressDialog = new ProgressDialog(mContext);
			progressDialog.setTitle("Searching");
			progressDialog.setMessage("Please wait while search is in progress");
			progressDialog.show();
		}

		@Override
		protected Object doInBackground(Object[] objects) {
			String serverResponse;

			try {
				HttpClient httpClient = new DefaultHttpClient();
				String url = MainActivity.OPEN_WEATHER_SEARCH_URL + URLEncoder.encode(mCityName, "UTF-8");

				HttpGet httpGet = new HttpGet(url);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();

				serverResponse = httpClient.execute(httpGet, responseHandler);
				Log.d("Server response", serverResponse);
			} catch (IOException e) {
				Log.d("Server response error", e.getMessage());
				serverResponse = "0";
			}

			return serverResponse;
		}

		public void onPostExecute(Object data) {

			if (progressDialog.isShowing()) {
				progressDialog.dismiss();
			}

			try {
				if (Integer.parseInt(data.toString()) == 0) {
					Toast.makeText(mContext, "Refresh failed!", Toast.LENGTH_SHORT).show();
					return;
				}
			} catch (NumberFormatException ignored) {
			}

			try {
				cityList.clear();

				JSONObject serverResponse = new JSONObject(data.toString());
				JSONArray cityListJSON = serverResponse.getJSONArray("list");

				for (int i = 0; i < cityListJSON.length(); i++) {
					int id = cityListJSON.getJSONObject(i).getInt("id");
					String name = cityListJSON.getJSONObject(i).getString("name");
					String country = cityListJSON.getJSONObject(i).getJSONObject("sys").getString("country");

					if (cityListJSON.length() == 1) {
						preferences.edit().putInt("city_id", id).putString("city_name", name).apply();

						Bundle result = new Bundle();
						result.putInt("city_id", id);
						result.putString("city_name", name);

						Intent resultData = new Intent();
						resultData.putExtras(result);

						setResult(RESULT_OK, resultData);
						finish();
					}

					cityList.add(new CityModel(id, name, country));
				}

				adapter.setListData(cityList);
				adapter.notifyDataSetChanged();
			} catch (JSONException e) {
				Log.d("JSON error", e.getMessage());
			}

		}
	}

	public class CityListAdapter extends BaseAdapter {
		private ArrayList<CityModel> mList;

		public CityListAdapter(ArrayList<CityModel> list) {
			mList = list;
		}

		public void setListData(ArrayList<CityModel> data) {
			mList = data;
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int i) {
			return mList.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			View itemView;
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			itemView = inflater.inflate(R.layout.city_list_item, viewGroup, false);

			TextView lblCityName = (TextView) itemView.findViewById(R.id.lblCityNameListItem);
			TextView lblCountry = (TextView) itemView.findViewById(R.id.lblCountryCodeListItem);

			lblCityName.setTypeface(lightType);
			lblCountry.setTypeface(lightType);

			lblCityName.setText(mList.get(i).cityName);
			lblCountry.setText(mList.get(i).country);

			return itemView;
		}
	}
}
