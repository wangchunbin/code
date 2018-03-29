package com.deploy.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接工具类
 * 
 * @author WangChunBin
 *
 */
public class DatabaseUtil {
	/**
	 * 获取数据库连接
	 * 
	 * @param driverClassName
	 * @param url
	 * @param username
	 * @param password
	 * @return
	 * @throws Exception
	 */
	public static Connection getConnection(String driverClassName, String url, String username, String password) throws Exception {
		Class.forName(driverClassName);
		return DriverManager.getConnection(url, username, password);
	}

	/**
	 * 关闭数据库连接
	 * 
	 * @param conn
	 */
	public static void closeConnection(Connection conn) {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
