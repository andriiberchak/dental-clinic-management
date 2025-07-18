package org.example.dentalclinicmanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.example.dentalclinicmanagement.dto.ImportReport;
import org.example.dentalclinicmanagement.dto.SimpleUserDto;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.ImportExportUserService;
import org.example.dentalclinicmanagement.util.CsvUtil;
import org.example.dentalclinicmanagement.util.ExcelUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImportExportUserServiceImpl implements ImportExportUserService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Override
    public ImportReport importFile(InputStream in, String fileName) throws IOException {
        log.info("Starting import from file: {}", fileName);

        String ext = FilenameUtils.getExtension(fileName).toLowerCase();

        List<SimpleUserDto> rows = switch (ext) {
            case "csv" -> CsvUtil.readUsers(in);
            case "xls", "xlsx" -> ExcelUtil.readUsers(in);
            default -> throw new IllegalArgumentException("Unsupported format: " + ext);
        };

        int created = 0, updated = 0, skipped = 0;

        for (SimpleUserDto row : rows) {
            String phone = row.phone().trim();
            if (phone.isEmpty()) {
                skipped++;
                continue;
            }

            User user = userRepo.findByPhoneNumber(phone)
                    .orElseGet(User::new);

            user.setPhoneNumber(phone);
            user.setFirstName(row.firstName());
            user.setLastName(row.lastName());

            String email = (row.email() == null || row.email().isBlank())
                    ? phone.replace("+", "") + "@import.local"
                    : row.email().trim();
            user.setEmail(email);

            if (user.getId() == null) {
                user.setPhoneNumber(phone);
                user.setPassword(encoder.encode(UUID.randomUUID().toString()));
                user.setRole(Role.USER);
                created++;
            } else {
                updated++;
            }
            userRepo.save(user);
        }
        log.info("Import completed: {} created, {} updated, {} skipped",
                created, updated, skipped);
        return new ImportReport(fileName, created, updated, skipped);
    }

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
