package org.extendify.bot.util;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class PackageParser {
    public static List<JsonObject> parse(String content) {
        List<JsonObject> result = new ArrayList<>();
        for (String pkg : content.split("\n\n")) {
            if (pkg.isEmpty()) {
                continue;
            }

            JsonObject object = new JsonObject();
            for (String line : pkg.split("\n")) {
                if (!line.contains(": ")) {
                    continue;
                }

                String[] parts = line.split(": ");
                object.addProperty(parts[0].toLowerCase().trim(), parts[1].trim());
            }

            if (!object.isEmpty()) {
                result.add(object);
            }
        }
        return result;
    }
}
