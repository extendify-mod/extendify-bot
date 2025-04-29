package org.extendify.bot.checker;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.Main;
import org.extendify.bot.util.CompareResult;
import org.extendify.bot.util.OperatingSystem;
import org.extendify.bot.util.ScannablePlatform;
import org.extendify.bot.util.VersionParser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WindowsChecker extends VersionChecker {
    private static final Logger LOGGER = LogManager.getLogger("Windows Version Checker");

    public WindowsChecker(String channelId, String roleId) {
        super(channelId, roleId);
    }

    @Override
    public List<VersionInfo> getLatestVersions() {
        List<VersionInfo> result = new ArrayList<>();

        String response = this.sendPostRequest("https://store.rg-adguard.net/api/GetFiles", new JsonObject(), "type=url&url=https://apps.microsoft.com/detail/9ncbcszsjrsb&ring=RP&lang=en-US");
        LOGGER.info("Got response from AdGuard");
        Pattern pattern = Pattern.compile("<a\\s+href=\"([^\"]+)\"[^>]*>SpotifyAB.SpotifyMusic_(.+?)_(.+?)__[^<]+?\\.appx");
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

    @Override
    public List<VersionInfo> getNewVersions() {
        List<VersionInfo> result = super.getNewVersions();

        if (!result.isEmpty()) {
            new Thread(() -> {
                try {
                    Main.JDA.awaitReady();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                LOGGER.info("Starting version scanner...");
                List<VersionInfo> scanned = new VersionScanner(result.get(result.size() - 1).getUrl()).scanInstallers();

                Map<OperatingSystem, List<VersionInfo>> osToVersions = scanned
                        .stream()
                        .collect(Collectors.groupingBy(VersionInfo::getOs));
                Map<ScannablePlatform, List<VersionInfo>> platformToVersion = Arrays
                        .stream(ScannablePlatform.values())
                        .collect(Collectors.toMap(
                                platform -> platform,
                                platform -> osToVersions.getOrDefault(platform.getOs(), new ArrayList<>())
                        ));

                Set<OperatingSystem> processed = new HashSet<>();

                for (Map.Entry<ScannablePlatform, List<VersionInfo>> entry : platformToVersion.entrySet()) {
                    if (entry.getValue().isEmpty() || processed.contains(entry.getKey().getOs())) {
                        continue;
                    }

                    TextChannel channel = Main.JDA.getTextChannelById(entry.getKey().getChannelId());
                    if (channel == null) {
                        LOGGER.error("No text channel for {}", entry.getKey().name());
                        continue;
                    }

                    processed.add(entry.getKey().getOs());

                    channel.sendMessage(VersionChecker.createMessage(entry.getValue()) + "\n" + "<@&" + entry.getKey().getRoleId() + ">").complete();
                }
            }, "Version Scanner Thread").start();
        }

        return result;
    }
}
