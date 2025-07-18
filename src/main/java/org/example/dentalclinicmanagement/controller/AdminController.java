package org.example.dentalclinicmanagement.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.ClinicSettingsDto;
import org.example.dentalclinicmanagement.dto.ImportReport;
import org.example.dentalclinicmanagement.service.ClinicSettingsService;
import org.example.dentalclinicmanagement.service.ImportExportUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ImportExportUserService importExportUserService;
    private final ClinicSettingsService settingsService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportReport importUsers(@RequestPart("file") MultipartFile file) throws IOException {
        return importExportUserService.importFile(file.getInputStream(), file.getOriginalFilename());
    }

    @GetMapping("/export")
    public void exportUsers(@RequestParam(defaultValue = "csv") String format,
                            HttpServletResponse resp) throws IOException {

        if ("xlsx".equalsIgnoreCase(format)) {
            resp.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"users-export.xlsx\"");
            resp.getOutputStream().write(importExportUserService.exportXlsx());
        } else {
            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"users-export.csv\"");
            importExportUserService.exportCsv(resp.getWriter());
        }
    }


    @GetMapping("/settings")
    public ResponseEntity<ClinicSettingsDto> getSettings() {
        log.debug("Getting clinic settings");

        ClinicSettingsDto settings = settingsService.getSettings();
        return ResponseEntity.ok(settings);
    }

    @PatchMapping("/settings")
    public ResponseEntity<ClinicSettingsDto> updateSettings(
            @Valid @RequestBody ClinicSettingsDto settings) {

        log.info("Updating clinic settings request");
        ClinicSettingsDto updated = settingsService.updateSettings(settings);
        return ResponseEntity.ok(updated);
    }
}
