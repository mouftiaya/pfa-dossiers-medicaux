package ma.clinic.results.extraction;

import java.io.IOException;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/** Reads the text layer of a PDF. A scanned PDF (only images) gives an empty text. */
@Component
public class PdfTextExtractor implements TextExtractor {

    @Override
    public Optional<String> extract(byte[] data, String contentType) {
        if (!"application/pdf".equals(contentType)) {
            return Optional.empty();
        }
        try (PDDocument document = Loader.loadPDF(data)) {
            String text = new PDFTextStripper().getText(document);
            return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (IOException e) {
            // Corrupted or protected PDF: the assistant will enter the values by hand
            return Optional.empty();
        }
    }
}
