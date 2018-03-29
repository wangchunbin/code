package com.deploy.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import com.deploy.util.DatabaseUtil;
import com.deploy.util.ExcelUtil;

/**
 * 数据库初始化插件mojo类
 * 
 * @author WangChunBin
 *
 */
@Mojo(name = "DBInit")
public class DBInitMojo extends AbstractMojo {
	/**
	 * 数据库驱动类
	 */
	@Parameter
	private String driverClassName;

	/**
	 * 数据库连接URL
	 */
	@Parameter
	private String url;

	/**
	 * 数据库连接用户名
	 */
	@Parameter
	private String username;

	/**
	 * 数据库连接密码
	 */
	@Parameter
	private String password;

	/**
	 * SQL脚本及数据execl存放目录
	 */
	@Parameter
	private File dataDir;

	/**
	 * 语句分割符
	 */
	@Parameter
	private String separator;

	@Override
	public String toString() {
		return "[driverClassName=" + driverClassName + ", url=" + url + ", username=" + username + ", password="
				+ password + ", separator=" + separator + ", dataDir=" + (dataDir == null ? null : dataDir.getPath())
				+ "]";
	}

	/**
	 * 插件执行目标方法
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("插件参数：" + this.toString());
		Connection conn = null;
		try {
			// 获取数据库连接
			conn = DatabaseUtil.getConnection(driverClassName, url, username, password);
			// 判断dataDir是否是目录
			if (dataDir != null && dataDir.isDirectory()) {
				// 获取dataDir目录下文件
				File[] files = dataDir.listFiles();
				if (files != null && files.length > 0) {
					final Pattern compile = Pattern.compile("(\\d+)");
					// 按文件名称中数字编号排序，以确定文件处理顺序
					Arrays.sort(files, new Comparator<File>() {
						public int compare(File o1, File o2) {
							Matcher matcher1 = compile.matcher(o1.getName());
							int num1 = -1;
							if (matcher1.find()) {
								num1 = Integer.parseInt(matcher1.group());
							}
							Matcher matcher2 = compile.matcher(o2.getName());
							int num2 = -1;
							if (matcher2.find()) {
								num2 = Integer.parseInt(matcher2.group());
							}
							return num1 - num2;
						}
					});
					// 遍历执行sql或导入execl数据
					for (File file : files) {
						// 获取文件名
						String fileName = file.getName();
						getLog().info("正在处理\"" + fileName + "\"文件...");
						// 获取文件后缀
						String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
						if ("sql".equals(suffix)) {
							// 获取sql脚本内容
							FileInputStream fis = new FileInputStream(file);
							BufferedReader br = new BufferedReader(new InputStreamReader(fis));
							StringBuffer sqlScript = new StringBuffer();
							String line = null;
							while ((line = br.readLine()) != null) {
								//sqlScript.append(line + System.lineSeparator());
								sqlScript.append(line + "\r\n");
							}
							// 关流
							br.close();
							fis.close();
							// 获取sql脚本中内容后，默认使用"\\#END"切割出每条sql
							String[] sqls = sqlScript.toString().trim().split(((separator==null||"".equals(separator))?"#END":separator));
							// 执行sql
							if (sqls != null && sqls.length > 0) {
								for (String sql : sqls) {
									if (sql == null || "".equals(sql.trim())) {
										continue;
									}
									//getLog().info("正在执行sql:" + System.lineSeparator() + sql);
									getLog().info("正在执行sql:" + "\r\n" + sql);
									Statement statement = conn.createStatement();
									statement.executeUpdate(sql);
									getLog().info("sql执行成功！");
									statement.close();
								}
							}
						}
						if ("xls".equals(suffix) || "xlsx".equals(suffix)) {
							FileInputStream fis = new FileInputStream(file);
							// 根据execl文件后缀获取Workbook对象
							Workbook wb = ExcelUtil.getWorkbook(fis, suffix);
							// 获取sheet数量
							int sheetCount = wb.getNumberOfSheets();
							// 遍历sheet
							for (int sheetNumber = 0; sheetNumber < sheetCount; sheetNumber++) {
								Sheet sheet = wb.getSheetAt(sheetNumber);
								getLog().info("正在处理\"" + sheet.getSheetName() + "\"sheet...");
								// 获取sheet第一行填写的要插入的表的名称
								Row firstRow = sheet.getRow(0);
								if (ExcelUtil.isRowEmpty(firstRow)) {
									continue;
								}
								Object obj = ExcelUtil.getCellFormatValue(firstRow.getCell(0));
								if (obj == null) {
									continue;
								}
								String tableName = obj.toString();
								// 获取sheet第二行填写的要插入的表的列名称
								Row secondRow = sheet.getRow(1);
								if (ExcelUtil.isRowEmpty(secondRow)) {
									continue;
								}
								int colNum = secondRow.getPhysicalNumberOfCells();
								String[] columns = new String[colNum];
								for (int i = 0; i < colNum; i++) {
									Object value = ExcelUtil.getCellFormatValue(secondRow.getCell(i));
									columns[i] = (value == null) ? "" : value.toString();
								}
								// 拼接SQL
								String columnsStr = Arrays.toString(columns).replace("[", "").replace("]", "");
								String sql = "insert into " + tableName + "(" + columnsStr + ") values("
										+ columnsStr.replaceAll("\\w+(,\\w+)*", "?") + ")";
								PreparedStatement pre = conn.prepareStatement(sql);
								// 遍历sheet剩余行，设置sql参数值，执行sql
								int rowNum = sheet.getPhysicalNumberOfRows();
								Row row = null;
								for (int i = 2; i < rowNum; i++) {
									row = sheet.getRow(i);
									if (ExcelUtil.isRowEmpty(row)) {
										continue;
									}
									int j = 0;
									while (j < colNum) {
										// 设置sql参数值
										Object value = ExcelUtil.getCellFormatValue(row.getCell(j));
										// 如果值是java.util.Date类型，则需转换成java.sql.Date类型，因为
										// PreparedStatement.setDate(int
										// parameterIndex, Date x)
										// x是java.sql.Date类型
										if (value != null && value instanceof Date) {
											Date date = (Date) value;
											pre.setDate(j + 1, new java.sql.Date(date.getTime()));
										} else {
											pre.setObject(j + 1, value);
										}
										j++;
									}
									// 添加批量sql
									pre.addBatch();
								}
								// 执行批量sql
								pre.executeBatch();
								pre.close();
								getLog().info("处理\"" + sheet.getSheetName() + "\"sheet完成！");
							}
							// 关闭
							wb.close();
							fis.close();
						}
						getLog().info("处理\"" + fileName + "\"文件完成！");
					}
				}
			} else {
				throw new Exception("dataDir参数值未设置或不是一个目录路径！");
			}
		} catch (Exception e) {
			// 打印异常
			e.printStackTrace();
			// 抛出MojoExecutionException，提示maven已经执行失败
			throw new MojoExecutionException("执行失败！");
		} finally {
			// 关闭数据库连接
			DatabaseUtil.closeConnection(conn);
		}
	}

	/**
	 * 测试
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		DBInitMojo mojo = new DBInitMojo();
		mojo.setDataDir(new File("C:\\Users\\das_w\\Desktop\\HisMavenPlugin\\TXDB"));
		mojo.setDriverClassName("oracle.jdbc.driver.OracleDriver");
		mojo.setUrl("jdbc:oracle:thin:@192.168.100.143:1521:DASTEST");
		mojo.setUsername("JAVAHIS");
		mojo.setPassword("Csdas321");
		mojo.execute();
	}
	
	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public File getDataDir() {
		return dataDir;
	}

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}
}
