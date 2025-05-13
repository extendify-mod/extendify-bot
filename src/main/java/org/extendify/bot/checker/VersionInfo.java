package org.extendify.bot.checker;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.util.OperatingSystem;
import org.extendify.bot.util.ScannablePlatform;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
@Builder
@ToString
public class VersionInfo {
    private static final Logger LOGGER = LogManager.getLogger("Version Info Instance");
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
    private final OperatingSystem os;
    private final String architecture;
    private final String channel;
    private final String version;
    private final String url;

    public static VersionInfo fromObject(JsonObject object) {
        return new VersionInfo(
                OperatingSystem.valueOf(object.get("os").getAsString()),
                object.get("arch").getAsString(),
                object.get("channel").getAsString(),
                object.get("version").getAsString(),
                object.get("url").getAsString()
        );
    }

    private String getFilename() {
        return (this.getOs().name().toLowerCase() + "_" + this.getArchitecture().toLowerCase() + "_" + this.getChannel().toLowerCase()).replace(' ', '-') + ".download";
    }

    public synchronized Path downloadFile() {
        String filename = this.getFilename();
        Path path = Paths.get("./data", filename);

        if (Files.exists(path)) {
            return path;
        }

        try {
            LOGGER.info("Downloading file {}...", filename);

            HttpURLConnection connection = (HttpURLConnection) new URL(this.url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0");
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }

            SCHEDULER.schedule(this::deleteFile, 10L, TimeUnit.MINUTES);

            LOGGER.info("Finished downloading file {}", filename);
        } catch (IOException e) {
            LOGGER.error("Couldn't download file {}", filename, e);
            return null;
        }

        return path;
    }

    public void deleteFile() {
        String filename = this.getFilename();

        try {
            if (Files.deleteIfExists(Paths.get("./data", filename))) {
                LOGGER.info("Deleted file {}", filename);
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't delete file {}", filename, e);
        }
    }

    public boolean matches(VersionInfo other) {
        return this.os.equals(other.os) && this.architecture.equals(other.architecture) && this.channel.equals(other.channel);
    }

    public boolean matches(ScannablePlatform other) {
        return this.os.equals(other.getOs()) && this.architecture.equals(other.getArchitecture());
    }

    public JsonObject serialize() {
        JsonObject result = new JsonObject();
        result.addProperty("os", this.os.name());
        result.addProperty("arch", this.architecture);
        result.addProperty("channel", this.channel);
        result.addProperty("version", this.version);
        result.addProperty("url", this.url);
        return result;
    }
}
