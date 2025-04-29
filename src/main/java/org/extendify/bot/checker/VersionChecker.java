package org.extendify.bot.checker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.extendify.bot.util.CompareResult;
import org.extendify.bot.util.VersionParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class VersionChecker {
    @Getter private final String channelId;
    @Getter private final String roleId;
    private List<VersionInfo> lastBatch;

    public VersionChecker(String channelId, String roleId) {
        this.channelId = channelId;
        this.roleId = roleId;
        this.lastBatch = new ArrayList<>();
    }

    public static String createMessage(List<VersionInfo> versions) {
        boolean single = versions.size() == 1;
        StringBuilder message = new StringBuilder()
                .append("## ")
                .append(versions.size())
                .append(" new version")
                .append(single ? "" : "s")
                .append(" are available on ")
                .append(versions.get(0).getOs())
                .append(" for the following architectures:\n");
        for (VersionInfo version : versions) {
            message
                    .append("- [**")
                    .append(version.getArchitecture())
                    .append(" (")
                    .append(version.getChannel())
                    .append(")**](<")
                    .append(version.getUrl())
                    .append(">)\n");
        }
        return message.toString();
    }

    public abstract List<VersionInfo> getLatestVersions();

    public String formatMessage(List<VersionInfo> versions) {
        return createMessage(versions);
    }

    public List<VersionInfo> getNewVersions() {
        List<VersionInfo> newVersions = new ArrayList<>();
        List<VersionInfo> batch = this.getLatestVersions();

        for (VersionInfo versionInfo : batch) {
            boolean isNewer = false;
            boolean foundAny = false;

            for (VersionInfo old : this.lastBatch) {
                if (versionInfo.matches(old)) {
                    if (VersionParser.compare(versionInfo.getVersion(), old.getVersion()).equals(CompareResult.NEWER)) {
                        isNewer = true;
                    }
                    foundAny = true;
                    break;
                }
            }

            if (isNewer || !foundAny) {
                newVersions.add(versionInfo);
            }
        }

        this.lastBatch = batch;
        return newVersions;
    }

    public HttpURLConnection sendRawRequest(String url, String mode, JsonObject headers) {
        return this.sendRawRequest(url, mode, headers, null);
    }

    public HttpURLConnection sendRawRequest(String url, String mode, JsonObject headers, String data) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(mode);

            for (Map.Entry<String, JsonElement> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue().getAsString());
            }

            if (data != null && !data.isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    byte[] input = data.getBytes(StandardCharsets.UTF_8);
                    out.write(input, 0, input.length);
                }
            }

            return connection;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String sendGetRequest(String url, JsonObject headers) {
        try {
            HttpURLConnection connection = this.sendRawRequest(url, "GET", headers);
            return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String sendPostRequest(String url, JsonObject headers, String data) {
        try {
            HttpURLConnection connection = this.sendRawRequest(url, "POST", headers, data);
            return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
