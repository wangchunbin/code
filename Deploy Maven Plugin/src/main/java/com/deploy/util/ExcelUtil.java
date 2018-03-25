package com.deploy.util;

import java.io.FileInputStream;
import java.util.Date;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * excel工具类
 * 
 * @author WangChunBin
 *
 */
public class ExcelUtil {
	/**
	 * 根据execl文件后缀创建不同的Workbook对象
	 * 
	 * @param fis
	 * @param suffix
	 * @return
	 * @throws Exception
	 */
	public static Workbook getWorkbook(FileInputStream fis, String suffix) throws Exception {
		if ("xls".equals(suffix)) {
			return new HSSFWorkbook(fis);
		} else if ("xlsx".equals(suffix)) {
			return new XSSFWorkbook(fis);
		} else {
			return null;
		}
	}

	/**
	 * 返回execl单元格值
	 * 
	 * @param cell
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Object getCellFormatValue(Cell cell) {
		Object cellvalue = "";
		if (cell != null) {
			switch (cell.getCellType()) {
			case Cell.CELL_TYPE_NUMERIC:
			case Cell.CELL_TYPE_FORMULA: {
				if (DateUtil.isCellDateFormatted(cell)) {
					Date date = cell.getDateCellValue();
					cellvalue = date;
				} else {
					cellvalue = String.valueOf(cell.getNumericCellValue());
				}
				break;
			}
			case Cell.CELL_TYPE_STRING:
				cellvalue = cell.getRichStringCellValue().getString();
				break;
			default:
				cellvalue = "";
			}
		} else {
			cellvalue = "";
		}
		return cellvalue;
	}

	/**
	 * 判断execl行是否是空行
	 * 
	 * @param row
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static boolean isRowEmpty(Row row) {
		if (row == null) {
			return true;
		}
		for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
			Cell cell = row.getCell(c);
			if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK)
				return false;
		}
		return true;
	}
}
