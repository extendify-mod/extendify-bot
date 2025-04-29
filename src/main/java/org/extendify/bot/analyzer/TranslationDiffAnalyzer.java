package org.extendify.bot.analyzer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.Main;
import org.extendify.bot.checker.VersionInfo;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class TranslationDiffAnalyzer {
    private static final Logger LOGGER = LogManager.getLogger("Translation Diff Analyzer");
    private final @Nullable VersionInfo windowsVersion;
    private final @Nullable VersionInfo androidVersion;

    private Path getSavePath(String version) {
        return Paths.get("./data", version.trim().toLowerCase() + "_translations.json");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean ensureTranslationsFile(String version) {
        Path path = this.getSavePath(version);

        if (Files.exists(path)) {
            return true;
        }

        if (path.getParent().toFile().mkdirs()) {
            LOGGER.info("Created data directory");
        }

        try {
            if (path.toFile().createNewFile()) {
                LOGGER.info("Created translations file");
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't create translations file", e);
            return false;
        }

        return true;
    }

    private JsonObject loadSavedTranslations(String version) {
        Path path = this.getSavePath(version);

        if (!this.ensureTranslationsFile(version)) {
            return new JsonObject();
        }

        try {
            String content = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
            JsonObject translations = JsonParser.parseString(content).getAsJsonObject();
            LOGGER.info("Loaded {} old translations", translations.size());
            return translations;
        } catch (IOException e) {
            LOGGER.error("Couldn't read translations file", e);
            return new JsonObject();
        }
    }

    private void saveTranslations(JsonObject translations, String version) {
        Path path = this.getSavePath(version);

        if (!this.ensureTranslationsFile(version)) {
            return;
        }

        try {
            FileUtils.writeStringToFile(path.toFile(), Main.GSON.toJson(translations), StandardCharsets.UTF_8);
            LOGGER.info("Saved {} new translations", translations.size());
        } catch (IOException e) {
            LOGGER.error("Couldn't save translations file", e);
        }
    }

    private JsonObject getWindowsTranslations() {
        return new JsonObject();
    }

    private JsonObject getAndroidTranslations() {
        return new JsonObject();
    }

    public void diffTranslations() {}
}
