package ma.clinic.results.extraction;

import java.util.Optional;

/**
 * Turns an uploaded document into plain text.
 *
 * Today only PDFs that contain real text are supported (PdfTextExtractor).
 * Scanned papers and photos need OCR: add a second implementation here (for example with Tesseract)
 * and return its text for the image content types. Nothing else in the service has to change.
 */
public interface TextExtractor {

    /** @return the text, or empty if this extractor cannot read that kind of document. */
    Optional<String> extract(byte[] data, String contentType);
}
