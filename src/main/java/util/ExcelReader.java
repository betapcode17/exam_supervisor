package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import model.bean.Invigilator;
import model.bean.Room;

public class ExcelReader {
    private String filePath;

    public ExcelReader(String filePath) {
        this.filePath = filePath;
    }

    public List<Invigilator> readInvigilators() throws IOException {
        List<Invigilator> invigilators = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0); // Sheet 0: Danh sách cán bộ coi thi
            
            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // Cột 0: TT, Cột 1: MA_GV, Cột 2: HO_TEN, Cột 3: NGAY_SINH, Cột 4: DON_VI_CONG_TAC
                Integer tt = getCellValueAsInteger(row.getCell(0));
                String maGV = getCellValueAsString(row.getCell(1));
                String hoTen = getCellValueAsString(row.getCell(2));
                Date ngaySinh = getCellValueAsDate(row.getCell(3));
                String donVi = getCellValueAsString(row.getCell(4));
                
                if (tt != null && tt > 0 && maGV != null && !maGV.isEmpty()) {
                    invigilators.add(new Invigilator(tt, maGV, hoTen, ngaySinh, donVi));
                }
            }
        }
        return invigilators;
    }

    public List<Room> readRooms() throws IOException {
        List<Room> rooms = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(1); // Sheet 1: Danh sách phòng thi
            
            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // Cột 0: TT, Cột 1: PHONG_THI, Cột 2: DIA_DIEM
                Integer tt = getCellValueAsInteger(row.getCell(0));
                String phongThi = getCellValueAsString(row.getCell(1));
                String diaDiem = getCellValueAsString(row.getCell(2));
                
                if (tt != null && tt > 0 && phongThi != null && !phongThi.isEmpty()) {
                    rooms.add(new Room(tt, phongThi, diaDiem));
                }
            }
        }
        return rooms;
    }

    public java.util.List<String> readSheetAsText(int sheetIndex) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = WorkbookFactory.create(fis)) {
            if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
                return lines;
            }
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            DataFormatter formatter = new DataFormatter();
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) { lines.add(""); continue; }
                StringBuilder sb = new StringBuilder();
                int lastCell = row.getLastCellNum() <= 0 ? 0 : row.getLastCellNum();
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = row.getCell(c);
                    String val = "";
                    if (cell != null) {
                        val = formatter.formatCellValue(cell);
                    }
                    if (c > 0) sb.append('\t');
                    sb.append(val != null ? val : "");
                }
                lines.add(sb.toString());
            }
        }
        return lines;
    }

    public int getNumberOfSheets() throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = WorkbookFactory.create(fis)) {
            return workbook.getNumberOfSheets();
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            default:
                return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Date getCellValueAsDate(Cell cell) {
        if (cell == null) return new Date();
        
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getDateCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                return sdf.parse(cell.getStringCellValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Date();
    }
}
