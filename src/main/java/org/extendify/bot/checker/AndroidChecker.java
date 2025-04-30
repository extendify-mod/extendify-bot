package org.extendify.bot.checker;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.util.OperatingSystem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AndroidChecker extends VersionChecker {
    private static final Logger LOGGER = LogManager.getLogger("Android Version Checker");

    public AndroidChecker(String channelId, String roleId) {
        super("android", channelId, roleId);
    }

    @Override
    public List<VersionInfo> getLatestVersions() {
        try {
            JsonObject headers = new JsonObject();
            headers.addProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0");

            String url = "https://d.apkpure.com/b/XAPK/com.spotify.music?version=latest";

            HttpURLConnection connection = this.sendRawRequest(url, "GET", headers);
            LOGGER.info("Got response from APKPure");
            if (connection.getResponseCode() != 200) {
                LOGGER.error("Couldn't get latest Android version due to ratelimit or IP block.");
                return new ArrayList<>();
            }

            Pattern pattern = Pattern.compile("Podcasts_(.+?)_");
            Matcher matcher = pattern.matcher(connection.getHeaderField("Content-Disposition"));
            if (matcher.find()) {
                String version = matcher.group(1);
                LOGGER.info("Found latest Android version: {}", version);
                return Arrays.stream(new VersionInfo[] {
                        VersionInfo
                                .builder()
                                .os(OperatingSystem.ANDROID)
                                .architecture("AnyCPU")
                                .channel("APKPURE")
                                .url(url)
                                .version(version)
                                .build()
                }).collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>();
    }

    @Override
    public String formatMessage(List<VersionInfo> versions) {
        VersionInfo version = versions.get(0);
        return "## Version " +
               version.getVersion() +
               " is available for Android:\n- [**" +
               version.getArchitecture() +
               " (" +
               version.getChannel() +
               ")**](<" +
               version.getUrl() +
               ">)\n";
    }
}
