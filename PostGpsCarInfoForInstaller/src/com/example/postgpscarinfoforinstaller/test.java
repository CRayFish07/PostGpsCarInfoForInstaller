package com.example.postgpscarinfoforinstaller;

import java.util.Map;

import com.example.postgpscarinfoforinstaller.util.GoogleToBaidu;

public class test {
	 static Double  jd=122.232332;
	static Double wd=28.223333;
	public static void main(String[] args) {
		
		Map<String , Double> map=GoogleToBaidu.Convert_GCJ02_To_BD09(wd,jd);
		System.out.println(map.get("lat"));
		System.out.println(map.get("lng"));
	}
	
}
