package com.interviewai.backend.integration.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CvTextExtractorTest {

    private final CvTextExtractor extractor = new CvTextExtractor();

    @Test
    void readsPdfDocxTablesAndLegacyWordDocuments() throws Exception {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 720);
                content.showText("Synthetic Java experience.");
                content.endText();
            }
            document.save(pdf);
        }
        assertEquals("Synthetic Java experience.", extractor.extract(file("cv.pdf", pdf.toByteArray())));

        ByteArrayOutputStream docx = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("Synthetic candidate");
            document.createTable(1, 1).getRow(0).getCell(0).setText("Java and SQL");
            document.write(docx);
        }
        String text = extractor.extract(file("cv.docx", docx.toByteArray()));
        assertTrue(text.contains("Synthetic candidate"));
        assertTrue(text.contains("Java and SQL"));

        ByteArrayOutputStream doc = new ByteArrayOutputStream();
        try (InputStream template = getClass().getResourceAsStream("/fixtures/empty.doc");
                HWPFDocument document = new HWPFDocument(template)) {
            document.getRange().insertBefore("Synthetic Word resume with Java experience.\r");
            document.write(doc);
        }
        assertTrue(extractor.extract(file("cv.doc", doc.toByteArray())).contains("Synthetic Word resume"));
    }

    @Test
    void rejectsCorruptUnsupportedEmptyAndOversizedUploads() throws Exception {
        for (String name : List.of("cv.pdf", "cv.docx", "cv.doc", "cv.txt")) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> extractor.extract(file(name, "invalid".getBytes(StandardCharsets.UTF_8))));
            assertEquals(400, error.getStatusCode().value(), name);
        }
        ByteArrayOutputStream empty = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            document.write(empty);
        }
        assertThrows(ResponseStatusException.class, () -> extractor.extract(file("cv.docx", empty.toByteArray())));
        ResponseStatusException tooLarge = assertThrows(ResponseStatusException.class,
                () -> extractor.extract(file("cv.pdf", new byte[5 * 1024 * 1024 + 1])));
        assertEquals(413, tooLarge.getStatusCode().value());
    }

    @Test
    void truncatesAtFourThousandCharactersWithoutSplittingUnicode() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("x".repeat(3999) + "😀" + "more text");
            document.write(output);
        }
        String text = extractor.extract(file("cv.docx", output.toByteArray()));
        assertEquals(3999, text.length());
    }

    private MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("cv", name, "application/octet-stream", content);
    }
}
