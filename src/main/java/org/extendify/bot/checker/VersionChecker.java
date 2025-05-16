package org.extendify.bot.checker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.extendify.bot.DataWriter;
import org.extendify.bot.util.CompareResult;
import org.extendify.bot.util.VersionParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public abstract class VersionChecker {
    private static final int REQUEST_TRIES = 5;
    private static final long REQUEST_DELAY = 2000L;
    private final String id;
    @Getter private final String channelId;
    @Getter private final String roleId;

    public static String createMessage(List<VersionInfo> versions) {
        StringBuilder message = new StringBuilder()
                .append("## Version ")
                .append(versions.get(0).getVersion())
                .append(" is now available on ")
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

            for (VersionInfo old : this.getLastBatch()) {
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

        this.saveBatch(batch);
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

    public @Nullable String sendGetRequest(String url, JsonObject headers) {
        try {
            int tries = 0;
            while (tries < REQUEST_TRIES) {
                HttpURLConnection connection = this.sendRawRequest(url, "GET", headers);
                if (connection.getResponseCode() != 200) {
                    tries++;
                    Thread.sleep(REQUEST_DELAY);
                    continue;
                }
                return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public @Nullable String sendPostRequest(String url, JsonObject headers, String data) {
        try {
            int tries = 0;
            while (tries < REQUEST_TRIES) {
                HttpURLConnection connection = this.sendRawRequest(url, "POST", headers, data);
                if (connection.getResponseCode() != 200) {
                    tries++;
                    Thread.sleep(REQUEST_DELAY);
                    continue;
                }
                return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void saveBatch(List<VersionInfo> batch) {
        JsonArray array = new JsonArray();
        batch.forEach(v -> array.add(v.serialize()));
        new DataWriter(this.id + "_batch.json").writeJsonElement(array);
    }

    public List<VersionInfo> getLastBatch() {
        JsonElement array = new DataWriter(this.id + "_batch.json").readJsonElement();
        if (array == null) {
            return new ArrayList<>();
        }

        List<VersionInfo> result = new ArrayList<>();
        for (JsonElement element : array.getAsJsonArray()) {
            result.add(VersionInfo.fromObject(element.getAsJsonObject()));
        }
        return result;
    }
}
