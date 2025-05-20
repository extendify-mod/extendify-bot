package org.extendify.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.analyzer.VersionAnalyzer;
import org.extendify.bot.checker.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    public static final JDA JDA;
    public static final boolean DEVELOPMENT;
    public static final String VERSION = Main.class.getPackage().getSpecificationVersion();
    public static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogManager.getLogger("Main");

    static {
        Dotenv.configure().systemProperties().load();
        DEVELOPMENT = Boolean.parseBoolean(System.getProperty("DEVELOPMENT"));
        JDA = JDABuilder.create(System.getProperty("DISCORD_TOKEN"), Collections.emptyList()).build();
        JDA.getPresence().setActivity(Activity.listening("Extendify (" + (DEVELOPMENT ? "Dev" : VERSION) + ")"));

        LOGGER.info("Running Extendify Bot {} ({})", VERSION, DEVELOPMENT ? "Dev" : "Prod");
    }

    public static GuildMessageChannel getChannel(String id) {
        GuildChannel channel = JDA.getGuildChannelById(id);
        if (channel instanceof GuildMessageChannel) {
            return (GuildMessageChannel) channel;
        }
        return null;
    }

    public static void main(String[] args) {
        DataWriter.deleteOldDownloads();

        VersionChecker[] checkers = {
                new AndroidChecker(System.getProperty("ANDROID_RELEASE_CHANNEL"), System.getProperty("ANDROID_PING_ROLE")),
                new LinuxChecker(System.getProperty("LINUX_RELEASE_CHANNEL"), System.getProperty("LINUX_PING_ROLE")),
                new WindowsChecker(System.getProperty("WINDOWS_RELEASE_CHANNEL"), System.getProperty("WINDOWS_PING_ROLE"))
        };

        new Thread(() -> {
            try {
                JDA.awaitReady();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            while (true) {
                List<VersionInfo> newVersions = new ArrayList<>();

                for (VersionChecker checker : checkers) {
                    GuildMessageChannel channel = getChannel(checker.getChannelId());
                    if (channel == null) {
                        LOGGER.error("No text channel for {}", checker.getClass().getSimpleName());
                        continue;
                    }

                    List<VersionInfo> versions = checker.getNewVersions();
                    if (versions.isEmpty()) {
                        continue;
                    }

                    newVersions.addAll(versions);

                    String message = checker.formatMessage(versions);
                    channel.sendMessage(message + "\n" + "<@&" + checker.getRoleId() + ">").complete();
                }

                LOGGER.info("Found {} new versions", newVersions.size());

                new VersionAnalyzer(newVersions).runAnalyzer(!newVersions.isEmpty());

                try {
                    // noinspection BusyWait
                    Thread.sleep((DEVELOPMENT ? 1 : 5) * 60 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "Update Check Thread").start();
    }
}
