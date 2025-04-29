package org.extendify.bot.checker;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.util.OperatingSystem;
import org.extendify.bot.util.ScannablePlatform;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Builder
@ToString
public class VersionInfo {
    private static final Logger LOGGER = LogManager.getLogger("Version Info Instance");
    private OperatingSystem os;
    private String architecture;
    private String channel;
    private String version;
    private String url;
    private final String filename = (this.os.name().toLowerCase() + "_" + this.architecture.toLowerCase() + "_" + this.channel.toLowerCase()).replace(' ', '-');

    public synchronized Path downloadFile() {
        Path path = Paths.get("./data", this.filename);

        if (Files.exists(path)) {
            return path;
        }

        try {
            LOGGER.info("Downloading file {}...", this.filename);
            FileUtils.copyURLToFile(new URL(this.url), path.toFile(), 10_000, 120_000);
            LOGGER.info("Finished downloading file {}", this.filename);
        } catch (IOException e) {
            LOGGER.error("Couldn't download file {}", this.filename, e);
        }

        return path;
    }

    public void deleteFile() {
        try {
            if (Files.deleteIfExists(Paths.get("./data", this.filename))) {
                LOGGER.info("Deleted file {}", this.filename);
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't delete file {}", this.filename, e);
        }
    }

    public boolean matches(VersionInfo other) {
        return this.os.equals(other.os) && this.architecture.equals(other.architecture) && this.channel.equals(other.channel);
    }

    public boolean matches(ScannablePlatform other) {
        return this.os.equals(other.getOs()) && this.architecture.equals(other.getArchitecture());
    }
}
