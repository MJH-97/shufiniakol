package com.shofiniakol.media;

import com.shofiniakol.common.ApiException;
import com.shofiniakol.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Stores dashboard-uploaded images on disk (a Docker volume) and serves them from /media/**. */
@Service
@RequiredArgsConstructor
public class StorageService {

    private final AppProperties props;
    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Path.of(props.getStorage().getDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** The extension comes from the file's magic bytes, never from the client-supplied name or type. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("FILE_REQUIRED", "اختر صورة للرفع");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            String ext = detectExtension(head);
            if (ext == null) {
                throw ApiException.badRequest("UNSUPPORTED_FILE_TYPE", "الصيغ المدعومة: JPG, PNG, WEBP");
            }
            String name = UUID.randomUUID() + "." + ext;
            try (InputStream full = file.getInputStream()) {
                Files.copy(full, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return props.getPublicBaseUrl().replaceAll("/+$", "") + "/media/" + name;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store upload", e);
        }
    }

    static String detectExtension(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return "png";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
