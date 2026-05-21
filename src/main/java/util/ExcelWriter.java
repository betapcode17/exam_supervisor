package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import model.bean.Assignment;
import model.bean.Invigilator;
import model.bean.Supervisor;

public class ExcelWriter {

    public static void writeAssignments(String filePath, List<Assignment> assignments,
                                        Map<String, Invigilator> invigilatorMap) throws IOException {
        System.out.println("[EXCEL-DEBUG] writeAssignments called with " + assignments.size() + " assignments");
        System.out.println("[EXCEL-DEBUG] invigilatorMap has " + invigilatorMap.size() + " entries");
        
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            final int maxRowsPerSheet = 20;
            int totalDataRows = assignments.size() * 2;
            System.out.println("[EXCEL-DEBUG] Total data rows to write: " + totalDataRows);
            
            // Tạo sheet đầu tiên
            String sheetName = totalDataRows > maxRowsPerSheet ? "Sheet 1" : "DANHSACHPHANCONG";
            Sheet currentSheet = workbook.createSheet(sheetName);
            int rowIndex = addVietnamHeader(currentSheet, titleStyle);
            int sheetIndex = 1;
            
            // Thêm header cho sheet đầu tiên
            rowIndex = addAssignmentHeaders(currentSheet, rowIndex, headerStyle);

            int dataRowCount = 0;
            int stt = 1;
            int assignmentCount = 0;

            for (Assignment assignment : assignments) {
                assignmentCount++;
                System.out.println("[EXCEL-DEBUG] Assignment " + assignmentCount + ": Room=" + assignment.getPhongThi() + ", GV1=" + assignment.getMaGV1() + ", GV2=" + assignment.getMaGV2());
                
                Invigilator inv1 = invigilatorMap.get(assignment.getMaGV1());
                Invigilator inv2 = invigilatorMap.get(assignment.getMaGV2());
                
                if (inv1 == null) System.out.println("[EXCEL-DEBUG] WARNING: inv1 is null for " + assignment.getMaGV1());
                if (inv2 == null) System.out.println("[EXCEL-DEBUG] WARNING: inv2 is null for " + assignment.getMaGV2());

                // Check if we need a new sheet for BOTH rows of this assignment
                if (dataRowCount + 2 > maxRowsPerSheet) {
                    // Sheet đầy, tạo sheet mới TRƯỚC khi viết assignment này
                    autoSizeColumns(currentSheet, 6);
                    sheetIndex++;
                    sheetName = "Sheet " + sheetIndex;
                    currentSheet = workbook.createSheet(sheetName);
                    rowIndex = addVietnamHeader(currentSheet, titleStyle);
                    rowIndex = addAssignmentHeaders(currentSheet, rowIndex, headerStyle);
                    dataRowCount = 0;
                }
                
                // Ghi dòng giám thị 1
                rowIndex = writeAssignmentRow(currentSheet, rowIndex, stt++, assignment.getMaGV1(), inv1,
                        "X", "", assignment.getPhongThi(), dataStyle);
                dataRowCount++;

                // Ghi dòng giám thị 2 (cùng sheet vì đã check phía trên)
                rowIndex = writeAssignmentRow(currentSheet, rowIndex, stt++, assignment.getMaGV2(), inv2,
                        "", "X", assignment.getPhongThi(), dataStyle);
                dataRowCount++;
            }

            autoSizeColumns(currentSheet, 6);

            System.out.println("[EXCEL-DEBUG] Writing " + assignmentCount + " assignments to file: " + filePath);
            try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
                workbook.write(fos);
                System.out.println("[EXCEL-DEBUG] File written successfully: " + filePath);
            }
            System.out.println("[EXCEL-DEBUG] File size: " + new java.io.File(filePath).length() + " bytes");
        }
    }
    
    private static int addAssignmentHeaders(Sheet sheet, int rowIndex, CellStyle headerStyle) {
        createRow(sheet, rowIndex, 28f);

        createCell(sheet, rowIndex, 0, "STT", headerStyle);
        createCell(sheet, rowIndex, 1, "Mã GV", headerStyle);
        createCell(sheet, rowIndex, 2, "Họ và tên", headerStyle);
        createCell(sheet, rowIndex, 3, "Giám thị 1", headerStyle);
        createCell(sheet, rowIndex, 4, "Giám thị 2", headerStyle);
        createCell(sheet, rowIndex, 5, "Phòng thi", headerStyle);

        return rowIndex + 1;
    }

    public static void writeSupervisors(String filePath, List<Supervisor> supervisors,
                                        Map<String, Invigilator> invigilatorMap) throws IOException {
        System.out.println("[EXCEL-DEBUG] writeSupervisors called with " + supervisors.size() + " supervisors");
        
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            final int maxRowsPerSheet = 20;

            // Tạo sheet đầu tiên
            String sheetName = supervisors.size() > maxRowsPerSheet ? "Sheet 1" : "DANHSACHGIAMSAT";
            Sheet currentSheet = workbook.createSheet(sheetName);
            int rowIndex = addVietnamHeader(currentSheet, titleStyle);

            // Thêm header
            createRow(currentSheet, rowIndex, 28f);
            createCell(currentSheet, rowIndex, 0, "STT", headerStyle);
            createCell(currentSheet, rowIndex, 1, "Mã GV", headerStyle);
            createCell(currentSheet, rowIndex, 2, "Họ và tên", headerStyle);
            createCell(currentSheet, rowIndex, 3, "Phòng thi được giám sát", headerStyle);
            rowIndex++;

            int dataRowCount = 0;
            int sheetIndex = 1;
            int stt = 1;

            for (Supervisor supervisor : supervisors) {
                // Kiểm tra xem có cần tạo sheet mới
                if (dataRowCount >= maxRowsPerSheet) {
                    autoSizeColumns(currentSheet, 4);
                    sheetIndex++;
                    sheetName = "Sheet " + sheetIndex;
                    currentSheet = workbook.createSheet(sheetName);
                    rowIndex = addVietnamHeader(currentSheet, titleStyle);

                    // Thêm header
                    createRow(currentSheet, rowIndex, 28f);
                    createCell(currentSheet, rowIndex, 0, "STT", headerStyle);
                    createCell(currentSheet, rowIndex, 1, "Mã GV", headerStyle);
                    createCell(currentSheet, rowIndex, 2, "Họ và tên", headerStyle);
                    createCell(currentSheet, rowIndex, 3, "Phòng thi được giám sát", headerStyle);
                    rowIndex++;
                    dataRowCount = 0;
                }

                Row row = currentSheet.createRow(rowIndex);
                row.setHeightInPoints(24f);

                Invigilator inv = invigilatorMap.get(supervisor.getMaGV());

                createCell(row, 0, String.format("%02d", stt++), dataStyle);
                createCell(row, 1, supervisor.getMaGV(), dataStyle);
                createCell(row, 2, inv != null ? inv.getHoTen() : "", dataStyle);
                createCell(row, 3, formatSupervisedRoom(supervisor), dataStyle);

                rowIndex++;
                dataRowCount++;
            }

            autoSizeColumns(currentSheet, 4);

            System.out.println("[EXCEL-DEBUG] Writing " + supervisors.size() + " supervisors to file: " + filePath);
            try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
                workbook.write(fos);
                System.out.println("[EXCEL-DEBUG] Supervisor file written successfully: " + filePath);
            }
            System.out.println("[EXCEL-DEBUG] Supervisor file size: " + new java.io.File(filePath).length() + " bytes");
        }
    }

    private static int writeAssignmentRow(Sheet sheet, int rowIndex, int stt, String maGV,
                                          Invigilator invigilator, String giamThi1, String giamThi2,
                                          String phongThi, CellStyle dataStyle) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);

        createCell(row, 0, String.format("%02d", stt), dataStyle);
        createCell(row, 1, maGV, dataStyle);
        createCell(row, 2, invigilator != null ? invigilator.getHoTen() : "", dataStyle);
        createCell(row, 3, giamThi1, dataStyle);
        createCell(row, 4, giamThi2, dataStyle);
        createCell(row, 5, phongThi, dataStyle);

        return rowIndex + 1;
    }

    private static String formatSupervisedRoom(Supervisor supervisor) {
        String fromRoom = supervisor.getFromRoom();
        String toRoom = supervisor.getToRoom();

        if (fromRoom == null && toRoom == null) {
            return "";
        }
        if (fromRoom == null) {
            return toRoom;
        }
        if (toRoom == null || fromRoom.equals(toRoom)) {
            return fromRoom;
        }
        return "Từ " + fromRoom + " đến " + toRoom;
    }

    private static void createRow(Sheet sheet, int rowIndex, float height) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(height);
    }

    private static void createCell(Sheet sheet, int rowIndex, int colIndex, String value, CellStyle style) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        createCell(row, colIndex, value, style);
    }

    private static void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private static int addVietnamHeader(Sheet sheet, CellStyle titleStyle) {
        // Dòng 0: Tiêu đề quốc hiệu dòng 1
        createRow(sheet, 0, 30f);
        createCell(sheet, 0, 0, "Cộng Hòa Xã Hội Chủ Nghĩa Việt Nam", titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // Dòng 1: Tiêu đề quốc hiệu dòng 2
        createRow(sheet, 1, 30f);
        createCell(sheet, 1, 0, "Độc Lập - Tự Do - Hạnh Phúc", titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        return 2; // Trả về hàng tiếp theo (hàng 2)
    }

    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = createBaseStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = createBaseStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = createBaseStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(false);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private static CellStyle createBaseStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(width + 512, 3500));
        }
    }
}
