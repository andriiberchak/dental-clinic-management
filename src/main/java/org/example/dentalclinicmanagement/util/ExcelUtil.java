package org.example.dentalclinicmanagement.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dentalclinicmanagement.dto.SimpleUserDto;
import org.example.dentalclinicmanagement.model.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

    private static final String[] HEADERS =
            {"phone", "first_name", "last_name", "email"};

    public static List<SimpleUserDto> readUsers(InputStream in) throws IOException {
        List<SimpleUserDto> list = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sh = wb.getSheetAt(0);

            for (Row row : sh) {
                if (row.getRowNum() == 0) continue;

                String phone = getString(row, 0);
                String firstName = getString(row, 1);
                String lastName = getString(row, 2);
                String email = getString(row, 3);

                list.add(new SimpleUserDto(phone, firstName, lastName, email));
            }
        }
        return list;
    }

    public static byte[] writeUsers(List<User> users) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Users");

            Row h = sh.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) h.createCell(i).setCellValue(HEADERS[i]);

            int r = 1;
            for (User u : users) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(u.getPhoneNumber());
                row.createCell(1).setCellValue(u.getFirstName());
                row.createCell(2).setCellValue(u.getLastName());
                row.createCell(3).setCellValue(u.getEmail());
            }
            for (int i = 0; i < HEADERS.length; i++) sh.autoSizeColumn(i);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                wb.write(bos);
                return bos.toByteArray();
            }
        }
    }

    private static String getString(Row row, int idx) {
        Cell c = row.getCell(idx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue()).trim();
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> "";
        };
    }
}
