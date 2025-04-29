package org.extendify.bot.checker;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.util.ScannablePlatform;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RequiredArgsConstructor
public class VersionScanner {
    private static final Logger LOGGER = LogManager.getLogger("Version Scanner");
    private static final String BASE_URL = "https://upgrade.scdn.co/upgrade/client/";
    private static final int ADDITIONAL_SEARCHES = 10;
    private static final int INCREMENT = 1000;
    private final String appxUrl;

    private Path downloadAppx() {
        Path path = Paths.get("./data/latest.appx");

        try {
            if (Files.deleteIfExists(path)) {
                LOGGER.info("Deleted old .appx file");
            }

            LOGGER.info("Downloading .appx file...");
            FileUtils.copyURLToFile(new URL(this.appxUrl), path.toFile(), 10_000, 120_000);
            LOGGER.info("Finished downloading .appx file");
            return path;
        } catch (IOException e) {
            LOGGER.error("Couldn't download .appx file", e);
        }

        return null;
    }

    private String getFullVersion() {
        Path path = this.downloadAppx();
        if (path == null) {
            return null;
        }

        try {
            try (ZipFile archive = new ZipFile(path.toFile())) {
                ZipEntry entry = archive.getEntry("Spotify.exe");
                if (entry == null) {
                    LOGGER.error("Spotify.exe file not found in archive");
                    return null;
                }

                String content = IOUtils.toString(archive.getInputStream(entry), StandardCharsets.UTF_8);
                Pattern pattern = Pattern.compile("(?<![\\w\\-])(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)\\.(g[0-9a-f]{8})(?![\\w\\-])");
                Matcher matcher = pattern.matcher(content);

                if (!matcher.find()) {
                    LOGGER.error("Couldn't find full build version in application");
                    return null;
                }

                StringBuilder fullVersion = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    fullVersion.append(matcher.group(i + 1));
                    if (i != 4) {
                        fullVersion.append(".");
                    }
                }
                LOGGER.info("Found full version: {}", fullVersion);
                return fullVersion.toString();
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't read .appx archive", e);
        }

        return null;
    }

    public List<VersionInfo> scanInstallers() {
        String fullVersion = this.getFullVersion();
        if (fullVersion == null) {
            return new ArrayList<>();
        }

        String version = fullVersion.substring(0, fullVersion.lastIndexOf('.'));
        List<VersionInfo> result = new ArrayList<>();

        int start = 0;
        int beforeEnter = INCREMENT;
        ExecutorService executor = Executors.newFixedThreadPool(100);

        for (int round = 0; round <= ADDITIONAL_SEARCHES; round++) {
            List<CompletableFuture<VersionInfo>> futures = new ArrayList<>();

            for (ScannablePlatform platform : ScannablePlatform.values()) {
                if (round > 0 && result.stream().anyMatch(r -> r.matches(platform))) {
                    continue;
                }

                for (int i = start; i <= beforeEnter; i++) {
                    String url = BASE_URL + String.format(platform.getPath(), fullVersion, i);
                    futures.add(this.checkUrlAsync(url, version, platform, executor));
                }
            }

            List<VersionInfo> newFound = futures
                    .stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            LOGGER.info("Found {} new valid version(s)", newFound.size());
            result.addAll(newFound);

            if (result.stream().allMatch(v -> {
                for (ScannablePlatform platform : ScannablePlatform.values()) {
                    if (v.matches(platform)) {
                        return true;
                    }
                }
                return false;
            })) {
                break;
            }

            start = beforeEnter + 1;
            beforeEnter += INCREMENT;
        }

        executor.shutdown();

        return result;
    }

    private CompletableFuture<VersionInfo> checkUrlAsync(String url, String version, ScannablePlatform platform, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("HEAD");
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return VersionInfo
                            .builder()
                            .os(platform.getOs())
                            .architecture(platform.getArchitecture())
                            .channel("INSTALLER")
                            .url(url)
                            .version(version)
                            .build();
                }
            } catch (Exception ignored) {
            }
            return null;
        }, executor);
    }
}
