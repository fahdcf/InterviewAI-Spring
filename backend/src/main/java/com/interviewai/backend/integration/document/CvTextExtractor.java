package com.interviewai.backend.integration.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Component
public class CvTextExtractor {

    private static final int MAX_CHARACTERS = 4000;

    public String extract(MultipartFile file) {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Choose a CV that is 5 MB or smaller.");
        }
        String name = file.getOriginalFilename();
        String extension = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String text;
        try {
            if (extension.endsWith(".pdf")) {
                text = readPdf(file);
            } else if (extension.endsWith(".docx")) {
                text = readDocx(file);
            } else if (extension.endsWith(".doc")) {
                try (InputStream input = file.getInputStream(); HWPFDocument document = new HWPFDocument(input)) {
                    text = document.getDocumentText();
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a PDF, DOCX, or DOC file.");
            }
        } catch (IOException | IllegalArgumentException | EncryptedDocumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CV could not be read.");
        }

        String trimmed = text.strip();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CV contains no readable text.");
        }
        if (trimmed.length() <= MAX_CHARACTERS) {
            return trimmed;
        }
        int end = Character.isHighSurrogate(trimmed.charAt(MAX_CHARACTERS - 1))
                ? MAX_CHARACTERS - 1 : MAX_CHARACTERS;
        return trimmed.substring(0, end);
    }

    private String readPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(10);
            return stripper.getText(document);
        }
    }

    private String readDocx(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream(); XWPFDocument document = new XWPFDocument(input)) {
            StringBuilder text = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    text.append(paragraph.getText()).append('\n');
                } else if (element instanceof XWPFTable table) {
                    text.append(table.getText()).append('\n');
                }
                if (text.length() >= MAX_CHARACTERS) {
                    break;
                }
            }
            return text.toString();
        }
    }
}
