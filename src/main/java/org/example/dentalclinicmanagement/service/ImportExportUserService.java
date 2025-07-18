package org.example.dentalclinicmanagement.service;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

public interface ImportExportUserService {
    void exportCsv(Writer out) throws IOException;

    byte[] exportXlsx() throws IOException;

    ImportReport importFile(InputStream in, String fileName) throws IOException;
}
