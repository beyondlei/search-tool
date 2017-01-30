package edu.kit.aifb.ma.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Property {

	static Properties prop = new Properties();

	static {
		FileInputStream in;
		try {
			// change to relative path
//			in = new FileInputStream("/local/users/lzh/configs/configuration.properties");
			in = new FileInputStream("/Users/leizhang/Documents/workspace/asearcher/src/main/resources/configuration.properties");
//			in = new FileInputStream("/home/michael/ABSSearcher/config/configuration.properties");
			prop.load(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getValue(String key) {
		return prop.getProperty(key);
	}

}
