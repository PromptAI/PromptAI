package com.zervice.common.utils;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelUtils {


    public static boolean writeExcelMultiSheet(OutputStream outputStream, List<String> names4AllSheets, List<List<String>> titles4AllSheets,
                                               List<List<Map<String, ?>>> values4AllSheets) throws IOException {
        if (names4AllSheets.size() != titles4AllSheets.size() || names4AllSheets.size() != values4AllSheets.size()) {
            throw new IllegalArgumentException(String.format("sheet 不匹配 (names = %d, titles=%d, values=%d)",
                    names4AllSheets.size(), titles4AllSheets.size(), values4AllSheets.size()));
        }

        Workbook workbook = new HSSFWorkbook();
        // 生成一个表格
        for (int j = 0; j < names4AllSheets.size(); j++) {
            String sheetName = names4AllSheets.get(j);
            Sheet sheet = workbook.createSheet(sheetName);
            // 设置表格默认列宽度为15个字节
            sheet.setDefaultColumnWidth((short) 15);
            // 生成样式
            // 创建标题行
            Row row = sheet.createRow(0);
            // 存储标题在Excel文件中的序号
            Map<String, Integer> titleOrder = new HashMap<>();
            List<String> titles = titles4AllSheets.get(j);
            for (int i = 0; i < titles.size(); i++) {
                Cell cell = row.createCell(i);
                String title = titles.get(i);
                cell.setCellValue(title);
                titleOrder.put(title, i);
            }
            // 写入正文
            List<Map<String, ?>> values = values4AllSheets.get(j);
            Iterator<Map<String, ?>> iterator = values.iterator();
            // 行号
            int index = 1;
            while (iterator.hasNext()) {
                row = sheet.createRow(index);
                Map<String, ?> value = iterator.next();
                for (Map.Entry<String, ?> map : value.entrySet()) {
                    // 获取列名
                    String title = map.getKey();
                    // 根据列名获取序号
                    int i = 0;
                    i = titleOrder.get(title);
                    // 在指定序号处创建cell
                    Cell cell = row.createCell(i);

                    // 获取列的值
                    Object object = map.getValue();
                    // 判断object的类型
                    cell.setCellValue(object.toString());
                }
                index++;
            }
        }


        workbook.write(outputStream);
        return true;
    }

    /**
     * 创建Excel文件
     *
     * @param filepath  filepath 文件全路径
     * @param sheetName 新Sheet页的名字
     * @param titles    表头
     * @param values    每行的单元格
     */
    public static boolean writeExcel(String filepath, String sheetName, List<String> titles,
                                     List<Map<String, ?>> values) throws IOException {
        if (StringUtils.isEmpty(sheetName)) {
            sheetName = "sheet1";
        }
        return writeExcelMultiSheet(new FileOutputStream(filepath), Arrays.asList(sheetName), Arrays.asList(titles), Arrays.asList(values));
    }

    public static void tsv2excel(String tsvFile, Charset charset, String targetExcelFile) throws Exception {
        CsvReadConfig config = new CsvReadConfig();
        config.setContainsHeader(true);
        config.setSkipEmptyRows(true);
        config.setFieldSeparator('\t');
        CsvReader csvReader = new CsvReader(new File(tsvFile), charset , config);

        CsvData d = csvReader.read();

        ExcelUtils.writeExcel(targetExcelFile, "sheet1", d.getHeader(),
                d.getRows().stream().map(CsvRow::getFieldMap).collect(Collectors.toList()));
    }

    public static CsvData readCsvData(File csvFile, Charset charset) throws IOException {
        CsvReadConfig config = new CsvReadConfig();
        config.setContainsHeader(true);
        config.setSkipEmptyRows(true);
        CsvReader csvReader = new CsvReader(csvFile, charset, config);
        return csvReader.read();
    }
}
