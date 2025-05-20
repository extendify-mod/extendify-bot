package org.extendify.bot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class DataWriter {
    private static final Logger LOGGER = LogManager.getLogger("Data Writer");
    private final String filename;

    public static void deleteOldDownloads() {
        Path path = Paths.get("./data");

        if (!Files.exists(path)) {
            LOGGER.warn("No data path found, couldn't clean up");
            return;
        }

        try (Stream<Path> stream = Files.walk(path)) {
            stream.forEach(v -> {
                if (v.toString().endsWith(".download")) {
                    if (v.toFile().delete()) {
                        LOGGER.info("Cleaned up file {}", v.toString());
                        return;
                    }

                    LOGGER.warn("Couldn't clean up file {}", v.toString());
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getPath() {
        return Paths.get("./data", this.filename.trim().toLowerCase());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean ensureFile() {
        Path path = this.getPath();

        if (Files.exists(path)) {
            return true;
        }

        if (path.getParent().toFile().mkdirs()) {
            LOGGER.info("Created data directory");
        }

        try {
            if (path.toFile().createNewFile()) {
                LOGGER.info("Created file {}", this.filename);
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't create file {}", this.filename, e);
            return false;
        }

        return true;
    }

    public @Nullable JsonElement readJsonElement() {
        Path path = this.getPath();

        if (!this.ensureFile()) {
            return null;
        }

        try {
            String content = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
            JsonElement element = JsonParser.parseString(content);
            return element.isJsonNull() ? null : element;
        } catch (IOException e) {
            LOGGER.error("Couldn't read file {}", this.filename, e);
            return null;
        }
    }

    public void writeJsonElement(JsonElement element) {
        Path path = this.getPath();

        if (!this.ensureFile()) {
            return;
        }

        try {
            FileUtils.writeStringToFile(path.toFile(), Main.GSON.toJson(element), StandardCharsets.UTF_8);
            LOGGER.info("Wrote to file {}", this.filename);
        } catch (IOException e) {
            LOGGER.error("Couldn't save translations file", e);
        }
    }
}
