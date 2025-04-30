package org.extendify.bot.util;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.*;

@Getter
@RequiredArgsConstructor
public class JsonObjectDiff {
    private final Map<String, String> added = new HashMap<>();
    private final Map<String, String> removed = new HashMap<>();
    private final Map<String, Pair<String, String>> changed = new HashMap<>();

    public static <V> JsonObjectDiff diff(JsonObject oldObj, JsonObject newObj) {
        JsonObjectDiff diff = new JsonObjectDiff();
        Set<String> keys = new HashSet<>();
        keys.addAll(oldObj.keySet());
        keys.addAll(newObj.keySet());

        for (String key : keys) {
            String oldVal = oldObj.has(key) ? oldObj.get(key).getAsString() : null;
            String newVal = newObj.has(key) ? newObj.get(key).getAsString() : null;

            if (!oldObj.has(key)) {
                diff.added.put(key, newVal);
            } else if (!newObj.has(key)) {
                diff.removed.put(key, oldVal);
            } else if (!Objects.equals(oldVal, newVal)) {
                diff.changed.put(key, Pair.of(oldVal, newVal));
            }
        }

        return diff;
    }
}
