package org.extendify.bot.analyzer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.DataWriter;
import org.extendify.bot.Main;
import org.extendify.bot.checker.VersionInfo;
import org.extendify.bot.util.JsonObjectDiff;
import org.jetbrains.annotations.Nullable;
import pink.madis.apk.arsc.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@RequiredArgsConstructor
public class StringDiffAnalyzer {
    private static final int CHARACTER_LIMIT = 2000;
    private static final Logger LOGGER = LogManager.getLogger("Translation Diff Analyzer");
    private final @Nullable VersionInfo windowsVersion;
    private final @Nullable VersionInfo androidVersion;

    private JsonObject loadSavedStrings(String version) {
        JsonElement element = new DataWriter(version + "_strings.json").readJsonElement();
        return element == null ? new JsonObject() : element.getAsJsonObject();
    }

    private void saveStrings(JsonObject translations, String version) {
        new DataWriter(version + "_strings.json").writeJsonElement(translations);
    }

    private JsonObject getWindowsStrings() {
        if (this.windowsVersion == null) {
            return new JsonObject();
        }

        JsonObject result = new JsonObject();

        try {
            Path appxPath = this.windowsVersion.downloadFile();
            ZipFile archive = new ZipFile(appxPath.toFile());

            for (String app : new String[] { "xpui", "login" }) {
                ZipEntry bundle = archive.getEntry("Apps/" + app + ".spa");
                if (bundle == null) {
                    continue;
                }

                try (ZipInputStream in = new ZipInputStream(archive.getInputStream(bundle))) {
                    ZipEntry entry;
                    while ((entry = in.getNextEntry()) != null) {
                        if (!entry.getName().endsWith("en.json")) {
                            continue;
                        }

                        LOGGER.info("Found translation file for {}", app);

                        // Why doesn't JsonObject just have a concat function...
                        String content = new String(in.readAllBytes());
                        for (Map.Entry<String, JsonElement> translation : JsonParser.parseString(content).getAsJsonObject().entrySet()) {
                            result.add(translation.getKey(), translation.getValue());
                        }

                        break;
                    }
                }
            }

            archive.close();
        } catch (IOException e) {
            LOGGER.error("Error while reading Windows strings", e);
        }

        return result;
    }

    private JsonObject getAndroidStrings() {
        if (this.androidVersion == null) {
            return new JsonObject();
        }

        JsonObject result = new JsonObject();

        try {
            Path xapkPath = this.androidVersion.downloadFile();
            ZipFile archive = new ZipFile(xapkPath.toFile());

            ZipEntry arscEntry = archive.getEntry("resources.arsc");
            if (arscEntry != null) {
                ResourceFile resources = ResourceFile.fromInputStream(archive.getInputStream(arscEntry));
                ResourceTableChunk table = (ResourceTableChunk) resources.getChunks().get(0);
                PackageChunk pkg = table.getPackages().iterator().next();

                for (TypeChunk typeChunk : pkg.getTypeChunks()) {
                    if (!typeChunk.getTypeName().equals("string")) {
                        continue;
                    }

                    if (!new String(typeChunk.getConfiguration().language()).equals("en")) {
                        continue;
                    }

                    for (TypeChunk.Entry entry : typeChunk.getEntries().values()) {
                        ResourceValue value = entry.value();
                        if (value == null || value.data() > table.getStringPool().getStringCount()) {
                            continue;
                        }

                        result.addProperty(entry.key(), table.getStringPool().getString(value.data()));
                    }
                }
            }

            archive.close();
        } catch (IOException e) {
            LOGGER.error("Error while reading Android strings", e);
        }

        return result;
    }

    @SuppressWarnings("StringConcatenationInLoop")
    private List<String> splitMessage(String title, Collection<String> lines) {
        List<String> parts = new ArrayList<>();
        String current = "**" + title + "**\n```diff\n";

        for (String line : lines) {
            String add = line + "\n\n";
            if ((current + add).length() > CHARACTER_LIMIT - 3) {
                parts.add(current + "```");
                current = "```diff\n" + add;
            } else {
                current += add;
            }
        }

        if (!current.equals("```diff\n")) {
            parts.add(current + "```");
        }

        return parts;
    }

    private void sendMessages(JsonObjectDiff diff, GuildMessageChannel channel) {
        if (!diff.getAdded().isEmpty()) {
            List<String> addedLines = diff.getAdded().entrySet().stream()
                                          .map(entry -> "+ " + entry.getKey() + ": " + entry.getValue())
                                          .collect(Collectors.toList());

            for (String message : this.splitMessage("Added", addedLines)) {
                channel.sendMessage(message).complete();
            }
        }

        if (!diff.getRemoved().isEmpty()) {
            List<String> removedLines = diff.getRemoved().entrySet().stream()
                                            .map(entry -> "- " + entry.getKey() + ": " + entry.getValue())
                                            .collect(Collectors.toList());

            for (String message : this.splitMessage("Removed", removedLines)) {
                channel.sendMessage(message).complete();
            }
        }

        if (!diff.getChanged().isEmpty()) {
            List<String> changedLines = new ArrayList<>();
            for (Map.Entry<String, Pair<String, String>> entry : diff.getChanged().entrySet()) {
                changedLines.add("- " + entry.getKey() + ": " + entry.getValue().getLeft());
                changedLines.add("+ " + entry.getKey() + ": " + entry.getValue().getRight());
            }

            for (String message : this.splitMessage("Changed", changedLines)) {
                channel.sendMessage(message).complete();
            }
        }
    }

    public void diffStringsAsync() {
        new Thread(() -> {
            try {
                Main.JDA.awaitReady();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            JsonObject windowsStrings = this.getWindowsStrings();
            JsonObject androidStrings = this.getAndroidStrings();
            JsonObject oldWindowsStrings = this.loadSavedStrings("windows");
            JsonObject oldAndroidStrings = this.loadSavedStrings("android");

            if (!oldWindowsStrings.isEmpty() && this.windowsVersion != null) {
                GuildMessageChannel channel = Main.getChannel(System.getProperty("DESKTOP_STRINGS_CHANNEL"));
                if (channel != null) {
                    JsonObjectDiff diff = JsonObjectDiff.diff(oldWindowsStrings, windowsStrings);
                    if (diff.hasChanged()) {
                        channel.sendMessage(
                                "## New strings for desktop version (" +
                                this.windowsVersion.getVersion() +
                                ", Windows) found:\n<@&" +
                                System.getProperty("DESKTOP_STRINGS_ROLE") +
                                ">"
                        ).complete();
                        this.sendMessages(diff, channel);
                    }
                }
            }

            if (!oldAndroidStrings.isEmpty() && this.androidVersion != null) {
                GuildMessageChannel channel = Main.getChannel(System.getProperty("MOBILE_STRINGS_CHANNEL"));
                if (channel != null) {
                    JsonObjectDiff diff = JsonObjectDiff.diff(oldAndroidStrings, androidStrings);
                    if (diff.hasChanged()) {
                        channel.sendMessage(
                                "## New strings for mobile version (" +
                                this.androidVersion.getVersion() +
                                ", Android) found:\n<@&" +
                                System.getProperty("MOBILE_STRINGS_ROLE") +
                                ">"
                        ).complete();
                        this.sendMessages(diff, channel);
                    }
                }
            }

            this.saveStrings(windowsStrings, "windows");
            this.saveStrings(androidStrings, "android");
        }, "String Diff Thread").start();
    }
}
