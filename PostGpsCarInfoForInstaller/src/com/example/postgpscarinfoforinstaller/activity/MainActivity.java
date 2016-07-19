package com.example.postgpscarinfoforinstaller.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.example.postgpscarinfoforinstaller.bean.CarInfo;
import com.example.postgpscarinfoforinstaller.net.NetHandler;
import com.example.postgpscarinfoforinstaller.net.NetThread;
import com.example.postgpscarinfoforinstaller.util.GoogleToBaidu;
import com.example.postgpscarinfoforinstaller.util.PublicUtil;
import com.example.postgpscarinfoforinstaller.view.ScrollUpdateListView;
import com.example.postgpscarinfoforintaller.R;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends BaseActivity {
	public Spinner textType;
	public EditText textContent;
	public Button submitButton;
	public ScrollUpdateListView redisListView;
	public TextView carInfo;
	public String c_cph, c_sbxlh;
	public int type;

	// 百度地图控件
	private MapView mMapView = null;
	// 百度地图对象
	private BaiduMap bdMap;
	private Marker marker1;
	private double latitude;
	private double longitude;
	// 构建marker图标
	BitmapDescriptor bitmap = BitmapDescriptorFactory
			.fromResource(R.drawable.icon_marka);

	// 定位
	private LocationClient locationClient;
	private BDLocationListener locationListener;

	private float radius;// 定位精度半径，单位是米
	private String addrStr;// 反地理编码
	private String province;// 省份信息
	private String city;// 城市信息
	private String district;// 区县信息
	private float direction;// 手机方向信息
	private int locType;
	// 定位模式 （普通-跟随-罗盘）
	private MyLocationConfiguration.LocationMode currentMode;
	// 定位图标描述
	private BitmapDescriptor currentMarker = null;

	private Boolean isFirstLoc = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
	}

	private void init() {
		textType = (Spinner) findViewById(R.id.textType);
		textContent = (EditText) findViewById(R.id.textContent);
		submitButton = (Button) findViewById(R.id.submitButton);
		redisListView = (ScrollUpdateListView) findViewById(R.id.redisListView);
		carInfo = (TextView) findViewById(R.id.carInfo);
		mMapView = (MapView) findViewById(R.id.bmapview);
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(14.0f);
		bdMap = mMapView.getMap();
		bdMap.setMapStatus(msu);
		currentMode = MyLocationConfiguration.LocationMode.NORMAL;
		bdMap.setMyLocationConfigeration(new MyLocationConfiguration(
				currentMode, true, currentMarker));
		initloc();

		textType.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				// textContent.setText("");
				// Toast.makeText(MainActivity.this,
				// arg2 + "" + textType.getSelectedItem(),
				// Toast.LENGTH_SHORT).show();
				if (arg2 == 0) {
					type = 1;
				}
				if (arg2 == 1) {
					type = 0;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				textContent.setText("");
				Toast.makeText(MainActivity.this, "请选择类型", Toast.LENGTH_SHORT)
						.show();
			}
		});

		submitButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String text = textContent.getText().toString();
				if (type == 0) {
					if ("".equals(text)) {
						Toast.makeText(MainActivity.this, "车牌号不能为空",
								Toast.LENGTH_SHORT).show();
					} else {
						c_cph = text;
						c_sbxlh = "";
						reqData();
					}
				}
				if (type == 1) {
					if ("".equals(text)) {
						Toast.makeText(MainActivity.this, "设备序列号不能为空",
								Toast.LENGTH_SHORT).show();
					} else {
						c_cph = "";
						c_sbxlh = text;
						reqData();
					}
				}
			}
		});
	}

	private void initloc() {
		bdMap.setMyLocationEnabled(true);
		// 1. 初始化LocationClient类
		locationClient = new LocationClient(getApplicationContext());
		// 2. 声明LocationListener类
		locationListener = new MyLocationListener();
		// 3. 注册监听函数
		locationClient.registerLocationListener(locationListener);
		// 4. 设置参数
		LocationClientOption locOption = new LocationClientOption();
		locOption.setLocationMode(LocationMode.Hight_Accuracy);// 设置定位模式
		locOption.setCoorType("bd09ll");// 设置定位结果类型
		locOption.setScanSpan(5000);// 设置发起定位请求的间隔时间,ms
		locOption.setIsNeedAddress(true);// 返回的定位结果包含地址信息
		locOption.setNeedDeviceDirect(true);// 设置返回结果包含手机的方向

		locationClient.setLocOption(locOption);
		// 5. 注册位置提醒监听事件
		// notifyListener = new MyNotifyListener();
		// notifyListener.SetNotifyLocation(longitude, latitude, 3000,
		// "bd09ll");//精度，维度，范围，坐标类型
		// locationClient.registerNotify(notifyListener);
		// 6. 开启/关闭 定位SDK
		locationClient.start();
		// locationClient.stop();
		// 发起定位，异步获取当前位置，因为是异步的，所以立即返回，不会引起阻塞
		// 定位的结果在ReceiveListener的方法onReceive方法的参数中返回。
		// 当定位SDK从定位依据判定，位置和上一次没发生变化，而且上一次定位结果可用时，则不会发生网络请求，而是返回上一次的定位结果。
		// 返回值，0：正常发起了定位 1：service没有启动 2：没有监听函数
		// 6：两次请求时间太短（前后两次请求定位时间间隔不能小于1000ms）
		/*
		 * if (locationClient != null && locationClient.isStarted()) {
		 * requestResult = locationClient.requestLocation(); } else {
		 * Log.d("LocSDK5", "locClient is null or not started"); }
		 */

	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	public void reqData() {
		new NetThread(null) {
			public void run() {
				// 登陆接口
				mHandler.sendEmptyMessage(NetHandler.CALL_CLIENT_redisLog4App);

				Map<String, Object> m = app.mNetService.redisLog4App(c_cph,
						c_sbxlh);

				Message msg = new Message();
				Bundle data = new Bundle();
				data.putString("car", String.valueOf(m.get("car")));
				data.putString("list", String.valueOf(m.get("list")));
				msg.setData(data);
				msg.what = NetHandler.BACK_CLIENT_redisLog4App;
				mHandler.sendMessage(msg);

			}
		}.start();

	}

	private NetHandler mHandler = new NetHandler(this) {
		protected void call_comm(Message msg) {
			if (msg.what == 700) {
				showDialog(CALL_DATA_DISPOSE);
				Log.d("下载关键点", "调用下载数据线程开始！");

			}
			if (msg.what == 701) {
				Bundle b = msg.getData();
				// int result = b.getInt("result");
				String carStr = msg.getData().getString("car");
				String listStr = msg.getData().getString("list");
				// carStr =
				// "{\"C_CPH\":\"陈伟测试1\",\"C_SBXLH\":\"1429220299\",\"C_JGMC\":\"中国邮政集团公司\", \"C_SD\": \"0\",\"C_WD\": \"28.223860\", \"C_JD\": \"112.908602\", \"D_WZSJ\": \"20160526111155\"}";
				// listStr =
				// "[\"[1429220299]2016-04-07 16:29:52 981 TCP下行:2a 58 59 2c 30 30 30 30 30 30 30 30 30 30 2c 44 31 2c 32 30 31 36 30 34 30 37 31 36 32 39 35 32 30 30 30 30 2c 31 30 2c 31 23 \",\"[1429220299]2016-04-07 16:29:46 383 TCP下行:2a 58 59 2c 30 30 30 30 30 30 30 30 30 30 2c 44 31 2c 32 30 31 36 30 34 30 37 31 36 32 39 34 36 30 30 30 30 2c 31 30 2c 31 23\",\"[1429220299]2016-04-07 16:29:46 383 TCP下行:2a 58 59 2c 30 30 30 30 30 30 30 30 30 30 2c 44 31 2c 32 30 31 36 30 34 30 37 31 36 32 39 34 36 30 30 30 30 2c 31 30 2c 31 23\",\"[1429220299]2016-04-07 16:29:46 383 TCP下行:2a 58 59 2c 30 30 30 30 30 30 30 30 30 30 2c 44 31 2c 32 30 31 36 30 34 30 37 31 36 32 39 34 36 30 30 30 30 2c 31 30 2c 31 23\"]";

				Log.i("信息", msg.getData().getString("car"));
				Log.i("信息", carStr);
				if (carStr.equals("{}") && listStr.equals("{}")
						|| null == carStr && null == listStr
						|| carStr.equals("null") && listStr.equals("null")) {
					Toast.makeText(MainActivity.this, "未找到数据",
							Toast.LENGTH_SHORT).show();
					carInfo.setText("未找到数据");
					redisListView.setAdapter(null);
					bdMap.clear();

				} else {

					try {
						// 当数据过来任意一个为空
						if (carStr.equals("{}") || null == carStr
								|| carStr.equals("null")) {

							JSONArray listary = new JSONArray(listStr);
							System.out.println(listary.length());
							List<String> redisList = new ArrayList<String>();
							for (int i = 0; i < listary.length(); i++) {
								String jo = listary.getString(i);
								redisList.add(jo);
							}
							carInfo.setText("未找到数据");
							showReidsInfo(redisList);
							bdMap.clear();
							Toast.makeText(MainActivity.this, "更新数据成功",
									Toast.LENGTH_SHORT).show();
						} else if (listStr.equals("{}") || null == listStr
								|| listStr.equals("null")) {
							JSONObject o = new JSONObject(carStr);
							CarInfo ci = new CarInfo();
							ci.setC_CPH(o.getString("C_CPH"));
							ci.setC_SBXLH(o.getString("C_SBXLH"));
							ci.setC_JGMC(o.getString("C_JGMC"));
							ci.setC_SD(o.getString("C_SD"));
							ci.setD_WZSJ(o.getString("D_WZSJ"));
							ci.setC_JD(o.getString("C_JD"));
							ci.setC_WD(o.getString("C_WD"));
							showCarInfo(ci);
							redisListView.setAdapter(null);
							Toast.makeText(MainActivity.this, "更新数据成功",
									Toast.LENGTH_SHORT).show();
						} else {
							JSONObject o = new JSONObject(carStr);
							CarInfo ci = new CarInfo();
							ci.setC_CPH(o.getString("C_CPH"));
							ci.setC_SBXLH(o.getString("C_SBXLH"));
							ci.setC_JGMC(o.getString("C_JGMC"));
							ci.setC_SD(o.getString("C_SD"));
							ci.setD_WZSJ(o.getString("D_WZSJ"));
							ci.setC_JD(o.getString("C_JD"));
							ci.setC_WD(o.getString("C_WD"));

							JSONArray listary = new JSONArray(listStr);
							System.out.println(listary.length());
							List<String> redisList = new ArrayList<String>();
							for (int i = 0; i < listary.length(); i++) {
								String jo = listary.getString(i);
								redisList.add(jo);
							}

							// showInfo(ci, redisList);
							showCarInfo(ci);
							showReidsInfo(redisList);
							Toast.makeText(MainActivity.this, "更新数据成功",
									Toast.LENGTH_SHORT).show();
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						Toast.makeText(MainActivity.this, "数据解析失败",
								Toast.LENGTH_SHORT).show();
						e.printStackTrace();
						carInfo.setText("数据解析失败");
						redisListView.setAdapter(null);
					}
				}

			}
			removeDialog(CALL_DATA_DISPOSE);
		}

	};

	// protected void showInfo(CarInfo ci, List<String> redisList) {
	// // TODO Auto-generated method stub
	// StringBuffer sb = new StringBuffer();
	// sb.append("车牌号:");
	// sb.append(ci.getC_CPH());
	// sb.append("\t\t\t经度:");
	// sb.append(ci.getC_JD());
	// sb.append("\n设备序列号:");
	// sb.append(ci.getC_SBXLH());
	// sb.append("\t\t\t纬度:");
	// sb.append(ci.getC_WD());
	// sb.append("\n机构名称:");
	// sb.append(ci.getC_JGMC());
	// sb.append("\t\t\t速度:");
	// sb.append(ci.getC_SD() + "km/h");
	// sb.append("\n位置时间:");
	// sb.append(PublicUtil
	// .GetDateForYYYY_MM_DD_HH_MI_SSFromYYYYMMDDHHMISSStr(ci
	// .getD_WZSJ()));
	// carInfo.setText(sb);
	//
	// List<Map<String, Object>> listems = new ArrayList<Map<String, Object>>();
	// for (int i = 0; i < redisList.size(); i++) {
	// Map<String, Object> listem = new HashMap<String, Object>();
	// listem.put("desc", redisList.get(i));
	// listems.add(listem);
	// }
	// // 界面显示数据
	// ListAdapter la = new SimpleAdapter(this, listems, R.layout.simple_item,
	// new String[] { "desc" }, new int[] { R.id.desc });
	// redisListView.setAdapter(la);
	// // 显示地图图标
	// addMarkerOverlay(ci.getC_JD(), ci.getC_WD());
	// }

	protected void showCarInfo(CarInfo ci) {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		sb.append("车牌号:");
		sb.append(ci.getC_CPH());
		sb.append("\t\t\t经度:");
		sb.append(ci.getC_JD());
		sb.append("\n设备序列号:");
		sb.append(ci.getC_SBXLH());
		sb.append("\t\t\t纬度:");
		sb.append(ci.getC_WD());
		sb.append("\n机构名称:");
		sb.append(ci.getC_JGMC());
		sb.append("\t\t\t速度:");
		sb.append(ci.getC_SD() + "km/h");
		sb.append("\n位置时间:");
		sb.append(PublicUtil
				.GetDateForYYYY_MM_DD_HH_MI_SSFromYYYYMMDDHHMISSStr(ci
						.getD_WZSJ()));
		carInfo.setText(sb);

		// 显示地图图标
		addMarkerOverlay(ci.getC_JD(), ci.getC_WD());
	}

	private void showReidsInfo(List<String> redisList) {
		// TODO Auto-generated method stub

		List<Map<String, Object>> listems = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < redisList.size(); i++) {
			Map<String, Object> listem = new HashMap<String, Object>();
			listem.put("desc", redisList.get(i));
			listems.add(listem);
		}
		// 界面显示数据
		ListAdapter la = new SimpleAdapter(this, listems, R.layout.simple_item,
				new String[] { "desc" }, new int[] { R.id.desc });
		redisListView.setAdapter(la);
	}

	/**
	 * 添加标注覆盖物
	 */
	private void addMarkerOverlay(String jd, String wd) {
		bdMap.clear();
		// goolge_to_baidu
		Map<String, Double> map = GoogleToBaidu.Convert_GCJ02_To_BD09(
				Double.parseDouble(wd), Double.parseDouble(jd));
		// map.get("lat")
		// Double.valueOf(String.format("%.10f",map.get("lat"))
		// 定义marker坐标点
		LatLng point = new LatLng(Double.valueOf(String.format("%.10f",
				map.get("lat"))), Double.valueOf(String.format("%.10f",
				map.get("lng"))));

		// 构建markerOption，用于在地图上添加marker
		OverlayOptions options = new MarkerOptions().position(point)// 设置marker的位置
				.icon(bitmap)// 设置marker的图标
				.zIndex(9)// 設置marker的所在層級
				.draggable(true);// 设置手势拖拽
		// 在地图上添加marker，并显示
		bdMap.addOverlay(options);
		//地图上居中车辆点
		MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(point);
		bdMap.setMapStatus(msu);
	}

	class MyLocationListener implements BDLocationListener {
		// 异步返回的定位结果
		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null) {
				return;
			}
			locType = location.getLocType();
			// Toast.makeText(MainActivity.this, "当前定位的返回值是："+locType,
			// Toast.LENGTH_SHORT).show();
			longitude = location.getLongitude();
			latitude = location.getLatitude();
			if (location.hasRadius()) {// 判断是否有定位精度半径
				radius = location.getRadius();
			}
			if (locType == BDLocation.TypeGpsLocation) {//
				// Toast.makeText(
				// MainActivity.this,
				// "当前速度是：" + location.getSpeed() + "~~定位使用卫星数量："
				// + location.getSatelliteNumber(),
				// Toast.LENGTH_SHORT).show();
			} else if (locType == BDLocation.TypeNetWorkLocation) {
				addrStr = location.getAddrStr();// 获取反地理编码(文字描述的地址)
				// Toast.makeText(MainActivity.this, addrStr,
				// Toast.LENGTH_SHORT).show();
			}
			direction = location.getDirection();// 获取手机方向，【0~360°】,手机上面正面朝北为0°
			province = location.getProvince();// 省份
			city = location.getCity();// 城市
			district = location.getDistrict();// 区县
			// Toast.makeText(MainActivity.this,
			// province + "~" + city + "~" + district, Toast.LENGTH_SHORT)
			// .show();
			// 构造定位数据
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(radius)//
					.direction(direction)// 方向
					.latitude(latitude)//
					.longitude(longitude)//
					.build();
			// 设置定位数据
			bdMap.setMyLocationData(locData);
			if (isFirstLoc) {
				isFirstLoc = false;
				LatLng ll = new LatLng(latitude, longitude);
				MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(ll);
				bdMap.animateMapStatus(msu);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy();
		// 回收bitmip资源
		bitmap.recycle();
		locationClient.unRegisterLocationListener(locationListener);
		locationClient.stop();
	}

}
