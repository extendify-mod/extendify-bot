package org.extendify.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extendify.bot.analyzer.VersionAnalyzer;
import org.extendify.bot.checker.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    public static final JDA JDA;
    public static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogManager.getLogger("Main");

    static {
        Dotenv.configure().systemProperties().load();
        JDA = JDABuilder.create(System.getProperty("DISCORD_TOKEN"), Collections.emptyList()).build();
    }

    public static void main(String[] args) {
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
                    TextChannel channel = JDA.getTextChannelById(checker.getChannelId());
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

                if (!newVersions.isEmpty()) {
                    LOGGER.info("Found {} new versions, running analyzer", newVersions.size());
                    new VersionAnalyzer(newVersions).runAnalyzer();
                }

                try {
                    // noinspection BusyWait
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "Update Check Thread").start();
    }
}
