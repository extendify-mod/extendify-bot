package org.extendify.bot.checker;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.util.CompareResult;
import org.extendify.bot.util.OperatingSystem;
import org.extendify.bot.util.VersionParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsChecker extends VersionChecker {
    private static final Logger LOGGER = LogManager.getLogger("Windows Version Checker");

    public WindowsChecker(String channelId, String roleId) {
        super("windows", channelId, roleId);
    }

    @Override
    public List<VersionInfo> getLatestVersions() {
        List<VersionInfo> result = new ArrayList<>();

        String response = this.sendPostRequest("https://store.rg-adguard.net/api/GetFiles", new JsonObject(), "type=url&url=https://apps.microsoft.com/detail/9ncbcszsjrsb&ring=RP&lang=en-US");
        if (response == null) {
            LOGGER.warn("No response from AdGuard");
            return result;
        }
        LOGGER.info("Got response from AdGuard");
        Pattern pattern = Pattern.compile("<a\\s+href=\"([^\"]+)\"[^>]*>SpotifyAB.SpotifyMusic_(.+?)_(arm64|x64)__[^<]+?\\.appx");
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            String url = matcher.group(1).trim();
            String version = matcher.group(2).trim();
            String arch = matcher.group(3).trim();

            Optional<VersionInfo> prevVer = result.stream().filter(v -> v.getArchitecture().equals(arch)).findFirst();
            if (prevVer.isPresent() && VersionParser.compare(version, prevVer.get().getVersion()).equals(CompareResult.NEWER)) {
                continue;
            }

            result.add(VersionInfo
                               .builder()
                               .os(OperatingSystem.WINDOWS)
                               .architecture(arch)
                               .channel("MS STORE")
                               .url(url)
                               .version(version)
                               .build());
            LOGGER.info("Added new version {} for architecture {}", version, arch);
        }

        return result;
    }
}
