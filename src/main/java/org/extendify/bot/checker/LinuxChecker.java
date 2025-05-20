package org.extendify.bot.checker;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.util.OperatingSystem;
import org.extendify.bot.util.PackageParser;

import java.util.ArrayList;
import java.util.List;

public class LinuxChecker extends VersionChecker {
    private static final Logger LOGGER = LogManager.getLogger("Linux Version Checker");
    private static final String BASE_URL = "https://repository-origin.spotify.com";
    private static final String[] CHANNELS = { "stable", "testing" };
    private static final String[] ARCHITECTURES = { "amd64", "i386" };

    public LinuxChecker(String channelId, String roleId) {
        super("linux", channelId, roleId);
    }

    @Override
    public List<VersionInfo> getLatestVersions() {
        List<VersionInfo> result = new ArrayList<>();

        for (String channel : CHANNELS) {
            for (String arch : ARCHITECTURES) {
                String url = BASE_URL + "/dists/" + channel + "/non-free/binary-" + arch + "/Packages";
                String response = this.sendGetRequest(url, new JsonObject());
                if (response == null) {
                    continue;
                }

                JsonObject pkg = PackageParser.parse(response)
                                              .stream()
                                              .filter(v -> v.get("package").getAsString().equals("spotify-client"))
                                              .findFirst()
                                              .orElseGet(JsonObject::new);
                if (pkg.isEmpty()) {
                    LOGGER.warn("Package is empty in channel {} for architecture {}", channel, arch);
                    continue;
                }

                String version = pkg.get("version").getAsString().split(":")[1];
                result.add(VersionInfo
                                   .builder()
                                   .os(OperatingSystem.LINUX)
                                   .architecture(arch)
                                   .channel(channel.toUpperCase())
                                   .url(BASE_URL + "/" + pkg.get("filename").getAsString())
                                   .version(version)
                                   .build());
                LOGGER.info("Added new version {} for architecture {}", version, arch);
            }
        }

        return result;
    }
}
