package org.example.dentalclinicmanagement.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.example.dentalclinicmanagement.dto.SimpleUserDto;
import org.example.dentalclinicmanagement.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvUtil {

    private static final CSVFormat FMT = CSVFormat.DEFAULT
            .builder()
            .setHeader("phone", "first_name", "last_name", "email")
            .setSkipHeaderRecord(true)
            .get();

    public static List<SimpleUserDto> readUsers(InputStream in) throws IOException {
        try (CSVParser p = FMT.parse(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return p.getRecords().stream().map(r -> new SimpleUserDto(
                    r.get("phone"),
                    r.get("first_name"),
                    r.get("last_name"),
                    r.get("email")
            )).toList();
        }
    }

    public static void writeUsers(List<User> users, Writer out) throws IOException {
        try (CSVPrinter pr = new CSVPrinter(out, FMT)) {
            for (User u : users) {
                pr.printRecord(
                        u.getPhoneNumber(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getEmail());
            }
        }
    }
}
