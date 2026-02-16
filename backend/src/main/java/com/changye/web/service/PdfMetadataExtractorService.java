package com.changye.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class PdfMetadataExtractorService {

    private static final Pattern DOI_PATTERN = Pattern.compile(
            "(10\\.\\d{4,9}/[-._;()/:A-Z0-9]+)",
            Pattern.CASE_INSENSITIVE
    );

    public ExtractedPdfMetadata extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ExtractedPdfMetadata(null, null);
        }

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            String doi = extractDoi(document);
            String title = extractTitle(document);
            return new ExtractedPdfMetadata(title, doi);
        } catch (IOException ex) {
            log.warn("Failed to extract metadata from pdf {}", file.getOriginalFilename(), ex);
            return new ExtractedPdfMetadata(null, null);
        }
    }

    private String extractDoi(PDDocument document) throws IOException {
        String text = readFirstPagesText(document, 2);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = DOI_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.replaceAll("[\\s<>.,;)]*$", "").trim();
    }

    private String extractTitle(PDDocument document) throws IOException {
        String docTitle = sanitizeTitle(document.getDocumentInformation().getTitle());
        if (StringUtils.hasText(docTitle) && !looksLikeFilename(docTitle)) {
            return docTitle;
        }

        String firstPageText = readFirstPagesText(document, 1);
        if (!StringUtils.hasText(firstPageText)) {
            return null;
        }
        List<String> lines = normalizeLines(firstPageText);
        if (lines.isEmpty()) {
            return null;
        }

        return lines.stream()
                .filter(this::isPossibleTitleLine)
                .max(Comparator.comparingInt(this::scoreTitleCandidate))
                .orElse(null);
    }

    private String readFirstPagesText(PDDocument document, int pages) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(Math.min(Math.max(pages, 1), document.getNumberOfPages()));
        return stripper.getText(document);
    }

    private List<String> normalizeLines(String text) {
        String[] rawLines = text.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            String normalized = line == null ? null : line.replaceAll("\\s+", " ").trim();
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            lines.add(normalized);
            if (lines.size() >= 40) {
                break;
            }
        }
        return lines;
    }

    private boolean isPossibleTitleLine(String line) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        if (line.length() < 12 || line.length() > 220) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.startsWith("abstract")
                || lower.startsWith("introduction")
                || lower.startsWith("keywords")
                || lower.startsWith("arxiv")
                || lower.contains("@")
                || lower.contains("http://")
                || lower.contains("https://")
                || lower.contains("doi")) {
            return false;
        }
        return countLetters(line) >= (line.length() * 0.45);
    }

    private int scoreTitleCandidate(String line) {
        int score = 0;
        int words = line.split("\\s+").length;
        if (words >= 4 && words <= 20) {
            score += 6;
        }
        if (line.length() >= 20 && line.length() <= 140) {
            score += 5;
        }
        score += Math.min(countLetters(line) / 10, 6);
        if (Character.isUpperCase(line.charAt(0))) {
            score += 2;
        }
        return score;
    }

    private int countLetters(String line) {
        int count = 0;
        for (char ch : line.toCharArray()) {
            if (Character.isLetter(ch)) {
                count++;
            }
        }
        return count;
    }

    private String sanitizeTitle(String rawTitle) {
        if (!StringUtils.hasText(rawTitle)) {
            return null;
        }
        return rawTitle.replaceAll("\\s+", " ").trim();
    }

    private boolean looksLikeFilename(String title) {
        String lower = title.toLowerCase(Locale.ROOT);
        return lower.endsWith(".pdf")
                || lower.contains("_")
                || lower.matches(".*\\d{4,}.*");
    }

    public record ExtractedPdfMetadata(String title, String doi) {}
}
