package ma.clinic.results.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Stores the uploaded analysis files on disk under a random name. */
@Component
public class FileStorage {

    private final Path root;

    public FileStorage(@Value("${results.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /** Detects the real type from the first bytes (the type sent by the browser is not trusted). */
    public static String detectContentType(byte[] data) {
        if (data.length >= 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F') {
            return "application/pdf";
        }
        if (data.length >= 4 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            return "image/png";
        }
        if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    public static String extensionFor(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            default -> ".jpg";
        };
    }

    /** @return the name of the stored file */
    public String store(byte[] data, String contentType) {
        String storedName = UUID.randomUUID() + extensionFor(contentType);
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(storedName), data);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store the uploaded file", e);
        }
        return storedName;
    }

    public Resource load(String storedName) {
        Path file = root.resolve(storedName).normalize();
        if (!file.startsWith(root) || !Files.isReadable(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new PathResource(file);
    }

    public void delete(String storedName) {
        if (storedName == null) {
            return;
        }
        try {
            Path file = root.resolve(storedName).normalize();
            if (file.startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {
            // The database row is what matters; a leftover file is harmless
        }
    }
}
