package io.github.khezyapp.fs;

import io.github.khezyapp.storageapi.SignedUrlOptions;
import io.github.khezyapp.storageapi.StorageMetadata;
import io.github.khezyapp.storageapi.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Optional;

public class FileSystemStorageService implements StorageService {
    private final String baseUrl;

    public FileSystemStorageService(final String baseUrl) {
        this.baseUrl = Optional.ofNullable(baseUrl)
                .map(this::stripTrailingSlash)
                .orElse("");
    }

    private String stripTrailingSlash(final String url) {
        if (!url.endsWith("/")) {
            return url.strip();
        }
        String finalUrl = url;
        while (finalUrl.endsWith("/")) {
            finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
        }
        return finalUrl.strip();
    }

    @Override
    public void upload(final String path,
                       final InputStream inputStream,
                       final String contentType) throws IOException {
        Files.copy(inputStream, Path.of(path));
    }

    @Override
    public Optional<InputStream> download(final String path) throws IOException {
        return Optional.of(Files.newInputStream(Path.of(path)));
    }

    @Override
    public void delete(final String path) throws IOException {
        Files.deleteIfExists(Path.of(path));
    }

    @Override
    public boolean exists(final String path) {
        return Files.exists(Path.of(path));
    }

    @Override
    public String getUrl(final String path) {
        return String.format("%s/%s", baseUrl, path);
    }

    @Override
    public String getSignedUrl(final String path,
                               final SignedUrlOptions options) {
        return getUrl(path);
    }

    @Override
    public StorageMetadata getMetadata(final String path) throws IOException {
        final var pathObj = Path.of(path);
        final var view = Files.getFileAttributeView(pathObj, BasicFileAttributeView.class);
        final var metadata = view.readAttributes();
        return new StorageMetadata(
                pathObj.toAbsolutePath().toString(),
                metadata.size(),
                Files.probeContentType(pathObj),
                metadata.lastModifiedTime().toInstant(),
                ""
        );
    }
}
