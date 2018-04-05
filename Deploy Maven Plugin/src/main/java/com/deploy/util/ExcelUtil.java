package com.deploy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
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
	
	/**
	 * 保存增量清单
	 * 
	 * @param savePath
	 * @param commitMessage
	 * @param diffInfo
	 * @throws Exception
	 */
	public static void saveIncrementalInfo(String savePath, String projectAtGitRepositoryPath, Map<String,String> gitCommitFileVersionInfo, Map<String,String> diffInfo, Map<File, String> jarDiffInfo) throws Exception{
		if(diffInfo!=null&&diffInfo.size()>0){
			@SuppressWarnings("resource")
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet sheet = workbook.createSheet("增量清单");
			sheet.setColumnWidth(0, 256*45+184);
			sheet.setColumnWidth(1, 256*45+184);
			sheet.setColumnWidth(2, 256*45+184);
			HSSFRow row = sheet.createRow(0);
			row.createCell(0).setCellValue("文件");
			row.createCell(1).setCellValue("修改类型");
			row.createCell(2).setCellValue("备注");
			int rowNum = 1;
			for(Entry<String, String> entry : diffInfo.entrySet()){
				if(entry.getKey().contains("WEB-INF/config/system")){// 过滤掉system目录下文件，不写入增量包
					continue;
				}
				HSSFRow temp = sheet.createRow(rowNum);
				String fileName=null;
				String filePath = entry.getKey();
				if (filePath.contains("/WebContent")) {
					fileName = filePath.replace(projectAtGitRepositoryPath + "/WebContent/", "");
				}else if(filePath.contains("/src")){
					String file = filePath.replace(projectAtGitRepositoryPath + "/src/", "").replace(".java", ".class");
					fileName = "WEB-INF/classes/" + file;
				}
				temp.createCell(0).setCellValue(fileName);
				temp.createCell(1).setCellValue(entry.getValue());
				boolean flag = false;
				String msg = null;
				if (gitCommitFileVersionInfo != null && gitCommitFileVersionInfo.size() > 0) {
					for (String key : gitCommitFileVersionInfo.keySet()) {
						if (key.equals(entry.getKey())) {
							flag = true;
							msg = gitCommitFileVersionInfo.get(key);
						}
					}
				}
				if(flag){
					temp.createCell(2).setCellValue(msg);
				}else{
					temp.createCell(2).setCellValue("");
				}
				rowNum++;
			}
			if(jarDiffInfo != null && jarDiffInfo.size() > 0){
				for(Entry<File,String> entry : jarDiffInfo.entrySet()){
					HSSFRow temp = sheet.createRow(rowNum);
					temp.createCell(0).setCellValue("WEB-INF/lib/"+entry.getKey().getName());
					temp.createCell(1).setCellValue(entry.getValue());
					rowNum++;
				}
			}
			File file =new File(savePath);
			file.getParentFile().mkdirs();
			FileOutputStream out = new FileOutputStream(savePath);
			workbook.write(out);
			out.close();
		}
	}
}
