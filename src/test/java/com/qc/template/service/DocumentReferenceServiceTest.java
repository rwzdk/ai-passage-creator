package com.qc.template.service;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentReferenceServiceTest {

    private final DocumentReferenceService service = new DocumentReferenceService(null);

    @Test
    void extractsPlainTextAndNormalizesWhitespace() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "reference.txt",
                "text/plain",
                "第一段\r\n\r\n\r\n第二段  内容".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        assertEquals("第一段\n\n第二段 内容", service.extractText(file));
    }

    @Test
    void rejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "reference.exe", "application/octet-stream", new byte[]{1}
        );

        assertThrows(IllegalArgumentException.class, () -> service.validateFile(file));
    }

    @Test
    void acceptsSupportedMimeTypeWhenFilenameHasNoExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "reference", "text/plain", "plain text reference".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        assertEquals("plain text reference", service.extractText(file));
    }

    @Test
    void rejectsEmptyExtractedText() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "reference.txt", "text/plain", " \n\t".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        assertThrows(IllegalArgumentException.class, () -> service.extractText(file));
    }

    @Test
    void rejectsFilesLargerThanTwentyMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "reference.txt", "text/plain", new byte[20 * 1024 * 1024 + 1]
        );

        assertThrows(IllegalArgumentException.class, () -> service.validateFile(file));
    }

    @Test
    void extractsDocxParagraphs() throws Exception {
        byte[] bytes;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Word reference content");
            document.write(output);
            bytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "reference.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes);

        assertEquals("Word reference content", service.extractText(file));
    }

    @Test
    void extractsLegacyDocDocument() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/47304.doc")) {
            assertTrue(input != null);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "reference.doc", "application/msword", input
            );

            assertTrue(!service.extractText(file).isBlank());
        }
    }

    @Test
    void extractsMarkdownText() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "reference.md", "text/markdown", "# Markdown reference\n\nimportant content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        assertEquals("# Markdown reference\n\nimportant content", service.extractText(file));
    }

    @Test
    void extractsPdfText() throws Exception {
        byte[] bytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText("PDF reference content");
                content.endText();
            }
            document.save(output);
            bytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "reference.pdf", "application/pdf", bytes);

        assertTrue(service.extractText(file).contains("PDF reference content"));
    }
}
