package org.example.dentalclinicmanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.example.dentalclinicmanagement.dto.ImportReport;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.ImportExportUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportExportUserServiceImpl implements ImportExportUserService {

    private final UserRepository userRepo;
    private final PasswordEncoder  encoder;

    /*------------------  ІМПОРТ  ------------------*/
    @Override
    public ImportReport importFile(InputStream in, String fileName) throws IOException {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();

        var rows = switch (ext) {
            case "csv"  -> CsvUtil.readUsers(in);
            case "xls", "xlsx" -> ExcelUtil.readUsers(in);
            default -> throw new IllegalArgumentException("Unsupported format: " + ext);
        };

        int created = 0, updated = 0, skipped = 0;

        for (var row : rows) {
            String phone = row.phone().trim();
            if (phone.isEmpty()) { skipped++; continue; }

            User user = userRepo.findByPhone(phone)
                    .orElseGet(User::new);

            /* базові поля */
            user.setPhone(phone);
            user.setFirstName(row.firstName());
            user.setLastName(row.lastName());
            user.setBirthDate(row.birthDate());

            /* ----------  e-mail  ---------- */
            String email = (row.email() == null || row.email().isBlank())
                    ? phone.replace("+","") + "@import.local"
                    : row.email().trim();
            user.setEmail(email);                       //  ←  **ніколи не порожній**

            /* якщо новий */
            if (user.getUserId() == null) {
                user.setUserName(phone);
                user.setPassword(encoder.encode(UUID.randomUUID().toString()));
                user.setRole(roleRepo.findByRoleName(AppRole.ROLE_USER).orElseThrow());
                user.setEnabled(true);
                created++;
            } else { updated++; }

            userRepo.save(user);
        }
        return new ImportReport(fileName, created, updated, skipped);
    }

    /*------------------  ЕКСПОРТ  ------------------*/
    @Transactional(readOnly = true)
    @Override
    public void exportCsv(Writer out) throws IOException {
        CsvUtil.writeUsers(userRepo.findAll(), out);
    }

    @Transactional(readOnly = true)
    @Override
    public byte[] exportXlsx() throws IOException {
        return ExcelUtil.writeUsers(userRepo.findAll());
    }

}
