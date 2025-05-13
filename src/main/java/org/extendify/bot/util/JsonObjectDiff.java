package org.extendify.bot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
@RequiredArgsConstructor
public class JsonObjectDiff {
    private final Map<String, String> added = new HashMap<>();
    private final Map<String, String> removed = new HashMap<>();
    private final Map<String, Pair<String, String>> changed = new HashMap<>();

    private static boolean isString(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    public static JsonObjectDiff diff(JsonObject oldObj, JsonObject newObj) {
        return diff(oldObj, newObj, null);
    }

    public static JsonObjectDiff diff(JsonObject oldObj, JsonObject newObj, @Nullable String parent) {
        JsonObjectDiff diff = new JsonObjectDiff();
        Set<String> keys = new HashSet<>();
        keys.addAll(oldObj.keySet());
        keys.addAll(newObj.keySet());

        String prefix = parent == null ? "" : parent + "/";

        for (String key : keys) {
            JsonElement oldVal = oldObj.has(key) ? oldObj.get(key) : null;
            JsonElement newVal = newObj.has(key) ? newObj.get(key) : null;

            if (newVal instanceof JsonObject && oldVal instanceof JsonObject) {
                JsonObjectDiff childDiff = diff(oldVal.getAsJsonObject(), newVal.getAsJsonObject(), prefix + key);
                diff.concat(childDiff);
            } else if (newVal instanceof JsonObject) {
                if (isString(oldVal)) {
                    diff.removed.put(key, oldVal.getAsString());
                }
                for (Map.Entry<String, JsonElement> added : newVal.getAsJsonObject().entrySet()) {
                    if (isString(added.getValue())) {
                        diff.added.put(prefix + key + "/" + added.getKey(), added.getValue().getAsString());
                    }
                }
            } else if (oldVal instanceof JsonObject) {
                for (Map.Entry<String, JsonElement> removed : oldVal.getAsJsonObject().entrySet()) {
                    if (isString(removed.getValue())) {
                        diff.removed.put(prefix + key + "/" + removed.getKey(), removed.getValue().getAsString());
                    }
                }
                if (newVal != null && newVal.getAsJsonPrimitive().isString()) {
                    diff.added.put(prefix + key, newVal.getAsString());
                }
            } else if (isString(newVal) && oldVal == null) {
                diff.added.put(prefix + key, newVal.getAsString());
            } else if (isString(oldVal) && newVal == null) {
                diff.removed.put(prefix + key, oldVal.getAsString());
            } else if (isString(oldVal) && isString(newVal)) {
                if (!Objects.equals(oldVal.getAsString(), newVal.getAsString())) {
                    diff.changed.put(prefix + key, Pair.of(oldVal.getAsString(), newVal.getAsString()));
                }
            }
        }

        return diff;
    }

    private void concat(JsonObjectDiff other) {
        this.added.putAll(other.getAdded());
        this.removed.putAll(other.getRemoved());
        this.changed.putAll(other.getChanged());
    }

    public boolean hasChanged() {
        return !this.added.isEmpty() || !this.removed.isEmpty() || !this.changed.isEmpty();
    }
}
