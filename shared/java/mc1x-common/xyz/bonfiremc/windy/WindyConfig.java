package xyz.bonfiremc.windy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class WindyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_CONFIG_VERSION = 2;
    private static WindyConfig INSTANCE = new WindyConfig();

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean spawnWind = true;
    public boolean windMustSeeSky = true;
    public WindDensityMode windDensityMode = WindDensityMode.CONSTANT;
    public ConstantWind constantWind = new ConstantWind();
    public YLevelScalingWind yLevelScalingWind = new YLevelScalingWind();
    public BiomeWind biomeWind = new BiomeWind();

    private transient Map<String, Double> biomeMultiplierCache = new LinkedHashMap<>();
    private transient Map<String, TagKey<Biome>> tagKeyCache = new LinkedHashMap<>();

    public static WindyConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Path path = WindyMod.getConfigPath();
        Path legacyJsonPath = path.resolveSibling("windy-config.json");
        boolean shouldSaveConfig = true;
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                TomlReadResult result = readToml(path);
                INSTANCE = result.config;
                shouldSaveConfig = !result.hadParseProblems;
            } else if (Files.exists(legacyJsonPath)) {
                readLegacyJson(legacyJsonPath);
            }
            INSTANCE.clamp();
            if (shouldSaveConfig) {
                save();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Windy config", e);
        }
    }

    private static void readLegacyJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement root = JsonParser.parseReader(jsonReader);
            WindyConfig loaded = GSON.fromJson(root, WindyConfig.class);
            if (loaded != null) {
                INSTANCE = loaded;
                if (root != null && root.isJsonObject()) {
                    INSTANCE.migrateJsonIfNeeded(root.getAsJsonObject());
                }
            }
        }
    }

    public static void save() {
        Path path = WindyMod.getConfigPath();
        INSTANCE.configVersion = CURRENT_CONFIG_VERSION;
        INSTANCE.clamp();
        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.write(INSTANCE.toCommentedToml());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Windy config", e);
        }
    }

    public int getActiveMinimumWindHeight() {
        if (windDensityMode == WindDensityMode.Y_LEVEL_SCALING) {
            return yLevelScalingWind.minimumWindHeight;
        }
        return constantWind.minimumWindHeight;
    }

    public double getHeightSpawnRateMultiplier(int y) {
        if (windDensityMode != WindDensityMode.Y_LEVEL_SCALING) {
            return constantWind.spawnRateMultiplier;
        }

        int minY = yLevelScalingWind.minimumWindHeight;
        int maxY = yLevelScalingWind.maximumWindHeight;
        if (maxY <= minY) {
            return yLevelScalingWind.maximumHeightSpawnRateMultiplier;
        }

        double progress = (double) (y - minY) / (double) (maxY - minY);
        if (progress < 0.0D) progress = 0.0D;
        if (progress > 1.0D) progress = 1.0D;
        return yLevelScalingWind.minimumHeightSpawnRateMultiplier
                + (yLevelScalingWind.maximumHeightSpawnRateMultiplier - yLevelScalingWind.minimumHeightSpawnRateMultiplier) * progress;
    }

    public double getBiomeSpawnRateMultiplier(Holder<Biome> biomeHolder) {
        if (biomeWind == null || !biomeWind.enabled) {
            return 1.0D;
        }

        String biomeId = getBiomeId(biomeHolder);
        String cacheKey = biomeId != null ? biomeId : "unknown:" + System.identityHashCode(biomeHolder.value());
        Double cached = biomeMultiplierCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        double resolved = resolveBiomeSpawnRateMultiplier(biomeHolder, biomeId);
        biomeMultiplierCache.put(cacheKey, resolved);
        return resolved;
    }

    public void clearBiomeCache() {
        biomeMultiplierCache.clear();
        tagKeyCache.clear();
    }

    private double resolveBiomeSpawnRateMultiplier(Holder<Biome> biomeHolder, String biomeId) {
        if (biomeWind == null) {
            return 1.0D;
        }

        if (isBlacklisted(biomeHolder, biomeId)) {
            return 0.0D;
        }

        if (biomeId != null && biomeWind.multipliers.containsKey(biomeId)) {
            return biomeWind.multipliers.get(biomeId);
        }

        boolean matchedTag = false;
        double highestTagMultiplier = 1.0D;
        for (Map.Entry<String, Double> entry : biomeWind.multipliers.entrySet()) {
            String key = normalizeRuleKey(entry.getKey());
            if (!isTagRule(key)) {
                continue;
            }
            TagKey<Biome> tagKey = getTagKey(key.substring(1));
            if (tagKey != null && biomeHolder.is(tagKey)) {
                double multiplier = entry.getValue();
                if (!matchedTag || multiplier > highestTagMultiplier) {
                    highestTagMultiplier = multiplier;
                }
                matchedTag = true;
            }
        }

        return matchedTag ? highestTagMultiplier : 1.0D;
    }

    private boolean isBlacklisted(Holder<Biome> biomeHolder, String biomeId) {
        for (String rawEntry : biomeWind.blacklist) {
            String entry = normalizeRuleKey(rawEntry);
            if (entry.isEmpty()) {
                continue;
            }
            if (isTagRule(entry)) {
                TagKey<Biome> tagKey = getTagKey(entry.substring(1));
                if (tagKey != null && biomeHolder.is(tagKey)) {
                    return true;
                }
            } else if (biomeId != null && biomeId.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    private TagKey<Biome> getTagKey(String rawId) {
        String id = normalizeRuleKey(rawId);
        if (id.isEmpty()) {
            return null;
        }
        if (tagKeyCache.containsKey(id)) {
            return tagKeyCache.get(id);
        }

        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            tagKeyCache.put(id, null);
            return null;
        }

        TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, location);
        tagKeyCache.put(id, tagKey);
        return tagKey;
    }

    private static String getBiomeId(Holder<Biome> biomeHolder) {
        Optional<net.minecraft.resources.ResourceKey<Biome>> key = biomeHolder.unwrapKey();
        return key.map(resourceKey -> resourceKey.location().toString()).orElse(null);
    }

    private void migrateJsonIfNeeded(JsonObject root) {
        int detectedVersion = getInt(root, "configVersion", 1);
        if (detectedVersion < CURRENT_CONFIG_VERSION) {
            if (root.has("minimumWindHeight")) {
                constantWind.minimumWindHeight = getInt(root, "minimumWindHeight", constantWind.minimumWindHeight);
            }
            if (root.has("spawnRateMultiplier")) {
                constantWind.spawnRateMultiplier = getDouble(root, "spawnRateMultiplier", constantWind.spawnRateMultiplier);
            }
            windDensityMode = WindDensityMode.CONSTANT;
        }
        configVersion = CURRENT_CONFIG_VERSION;
    }

    private void clamp() {
        configVersion = CURRENT_CONFIG_VERSION;
        if (windDensityMode == null) windDensityMode = WindDensityMode.CONSTANT;
        if (constantWind == null) constantWind = new ConstantWind();
        if (yLevelScalingWind == null) yLevelScalingWind = new YLevelScalingWind();
        if (biomeWind == null) biomeWind = new BiomeWind();
        if (biomeMultiplierCache == null) biomeMultiplierCache = new LinkedHashMap<>();
        if (tagKeyCache == null) tagKeyCache = new LinkedHashMap<>();

        constantWind.minimumWindHeight = clampInt(constantWind.minimumWindHeight, -64, 320);
        constantWind.spawnRateMultiplier = clampDouble(constantWind.spawnRateMultiplier, 0.0D, 100.0D);

        yLevelScalingWind.minimumWindHeight = clampInt(yLevelScalingWind.minimumWindHeight, -64, 320);
        yLevelScalingWind.maximumWindHeight = clampInt(yLevelScalingWind.maximumWindHeight, -64, 320);
        if (yLevelScalingWind.maximumWindHeight < yLevelScalingWind.minimumWindHeight) {
            yLevelScalingWind.maximumWindHeight = yLevelScalingWind.minimumWindHeight;
        }
        yLevelScalingWind.minimumHeightSpawnRateMultiplier = clampDouble(yLevelScalingWind.minimumHeightSpawnRateMultiplier, 0.0D, 100.0D);
        yLevelScalingWind.maximumHeightSpawnRateMultiplier = clampDouble(yLevelScalingWind.maximumHeightSpawnRateMultiplier, 0.0D, 100.0D);

        biomeWind.sanitize();
        clearBiomeCache();
    }

    private static TomlReadResult readToml(Path path) throws IOException {
        WindyConfig config = new WindyConfig();
        TomlParseState state = new TomlParseState();
        String section = "";
        boolean multipliersSectionCleared = false;

        String multilineKey = null;
        String multilineSection = "";
        StringBuilder multilineValue = new StringBuilder();

        for (String rawLine : Files.readAllLines(path)) {
            String line = stripInlineComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (multilineKey != null) {
                if (multilineValue.length() > 0) {
                    multilineValue.append('\n');
                }
                multilineValue.append(line);

                if (isCompleteTomlArray(multilineValue.toString())) {
                    applyTomlValue(config, multilineSection, multilineKey, multilineValue.toString(), state);
                    multilineKey = null;
                    multilineSection = "";
                    multilineValue.setLength(0);
                }
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                if (isSection(section, "biome_wind.multipliers", "biomeWind.multipliers") && !multipliersSectionCleared) {
                    config.biomeWind.multipliers.clear();
                    multipliersSectionCleared = true;
                }
                continue;
            }

            int equals = findUnquotedEquals(line);
            if (equals < 0) {
                state.hadParseProblems = true;
                continue;
            }

            String key = parseTomlKey(line.substring(0, equals).trim());
            String value = line.substring(equals + 1).trim();
            if (value.startsWith("[") && !isCompleteTomlArray(value)) {
                multilineKey = key;
                multilineSection = section;
                multilineValue.setLength(0);
                multilineValue.append(value);
                continue;
            }

            applyTomlValue(config, section, key, value, state);
        }

        if (multilineKey != null) {
            state.hadParseProblems = true;
        }

        return new TomlReadResult(config, state.hadParseProblems);
    }

    private static void applyTomlValue(WindyConfig config, String section, String key, String value, TomlParseState state) {
        if (isSection(section, "")) {
            switch (key) {
                case "config_version", "configVersion" -> config.configVersion = parseInt(value, config.configVersion, state);
                case "spawn_wind", "spawnWind" -> config.spawnWind = parseBoolean(value, config.spawnWind, state);
                case "wind_must_see_sky", "windMustSeeSky" -> config.windMustSeeSky = parseBoolean(value, config.windMustSeeSky, state);
                case "wind_density_mode", "windDensityMode" -> config.windDensityMode = parseWindDensityMode(value, config.windDensityMode, state);
                case "minimumWindHeight" -> config.constantWind.minimumWindHeight = parseInt(value, config.constantWind.minimumWindHeight, state);
                case "spawnRateMultiplier" -> config.constantWind.spawnRateMultiplier = parseDouble(value, config.constantWind.spawnRateMultiplier, state);
            }
        } else if (isSection(section, "constant_wind", "constantWind")) {
            switch (key) {
                case "minimum_wind_height", "minimumWindHeight" -> config.constantWind.minimumWindHeight = parseInt(value, config.constantWind.minimumWindHeight, state);
                case "spawn_rate_multiplier", "spawnRateMultiplier" -> config.constantWind.spawnRateMultiplier = parseDouble(value, config.constantWind.spawnRateMultiplier, state);
            }
        } else if (isSection(section, "y_level_scaling_wind", "yLevelScalingWind")) {
            switch (key) {
                case "minimum_wind_height", "minimumWindHeight" -> config.yLevelScalingWind.minimumWindHeight = parseInt(value, config.yLevelScalingWind.minimumWindHeight, state);
                case "minimum_height_spawn_rate_multiplier", "minimumHeightSpawnRateMultiplier" -> config.yLevelScalingWind.minimumHeightSpawnRateMultiplier = parseDouble(value, config.yLevelScalingWind.minimumHeightSpawnRateMultiplier, state);
                case "maximum_wind_height", "maximumWindHeight" -> config.yLevelScalingWind.maximumWindHeight = parseInt(value, config.yLevelScalingWind.maximumWindHeight, state);
                case "maximum_height_spawn_rate_multiplier", "maximumHeightSpawnRateMultiplier" -> config.yLevelScalingWind.maximumHeightSpawnRateMultiplier = parseDouble(value, config.yLevelScalingWind.maximumHeightSpawnRateMultiplier, state);
            }
        } else if (isSection(section, "biome_wind", "biomeWind")) {
            switch (key) {
                case "enabled" -> config.biomeWind.enabled = parseBoolean(value, config.biomeWind.enabled, state);
                case "blacklist" -> config.biomeWind.blacklist = parseStringList(value, config.biomeWind.blacklist, state);
            }
        } else if (isSection(section, "biome_wind.multipliers", "biomeWind.multipliers")) {
            config.biomeWind.multipliers.put(normalizeRuleKey(key), parseDouble(value, 1.0D, state));
        }
    }

    private String toCommentedToml() {
        StringBuilder builder = new StringBuilder();
        appendComment(builder, "Internal config version used by the mod for future migrations. Current version: " + CURRENT_CONFIG_VERSION);
        appendComment(builder, "Range: > 1");
        appendTomlValue(builder, "config_version", configVersion);
        appendTomlValue(builder, "spawn_wind", spawnWind);
        appendTomlValue(builder, "wind_must_see_sky", windMustSeeSky);
        builder.append('\n');

        appendComment(builder, "Controls how wind particle density is calculated.");
        appendComment(builder, "CONSTANT = uses constant_wind.minimum_wind_height and constant_wind.spawn_rate_multiplier.");
        appendComment(builder, "Y_LEVEL_SCALING = scales from y_level_scaling_wind.minimum_height_spawn_rate_multiplier to y_level_scaling_wind.maximum_height_spawn_rate_multiplier between the configured Y levels.");
        appendComment(builder, "Allowed values: CONSTANT, Y_LEVEL_SCALING");
        appendTomlValue(builder, "wind_density_mode", windDensityMode.name());
        builder.append('\n');

        appendSection(builder, "constant_wind");
        appendComment(builder, "Settings used when wind_density_mode is CONSTANT.");
        appendComment(builder, "Wind can spawn at/above minimum_wind_height and uses the same spawn_rate_multiplier at every height.");
        appendTomlValue(builder, "minimum_wind_height", constantWind.minimumWindHeight);
        appendTomlValue(builder, "spawn_rate_multiplier", constantWind.spawnRateMultiplier);
        builder.append('\n');

        appendSection(builder, "y_level_scaling_wind");
        appendComment(builder, "Settings used when wind_density_mode is Y_LEVEL_SCALING.");
        appendComment(builder, "Wind starts at minimum_wind_height, reaches maximum_height_spawn_rate_multiplier at maximum_wind_height, and stays at that maximum above it.");
        appendTomlValue(builder, "minimum_wind_height", yLevelScalingWind.minimumWindHeight);
        appendTomlValue(builder, "minimum_height_spawn_rate_multiplier", yLevelScalingWind.minimumHeightSpawnRateMultiplier);
        appendTomlValue(builder, "maximum_wind_height", yLevelScalingWind.maximumWindHeight);
        appendTomlValue(builder, "maximum_height_spawn_rate_multiplier", yLevelScalingWind.maximumHeightSpawnRateMultiplier);
        builder.append('\n');

        appendSection(builder, "biome_wind");
        appendComment(builder, "Optional biome-based wind rules. These multiply the active CONSTANT or Y_LEVEL_SCALING density.");
        appendComment(builder, "Biome rules are only checked when enabled is true.");
        appendTomlValue(builder, "enabled", biomeWind.enabled);
        builder.append('\n');
        appendComment(builder, "Biomes or biome tags listed here spawn no wind. Use biome IDs like \"minecraft:deep_dark\" or tags like \"#minecraft:is_nether\".");
        appendStringListToml(builder, "blacklist", biomeWind.blacklist);
        builder.append('\n');

        appendSection(builder, "biome_wind.multipliers");
        appendComment(builder, "Add, remove, or change biome IDs and biome tags here. Biome IDs use \"namespace:path\"; biome tags use \"#namespace:path\".");
        appendComment(builder, "Priority: blacklist wins; exact biome ID multipliers override tag multipliers; if multiple tags match, the highest matching tag multiplier is used.");
        if (biomeWind.multipliers != null) {
            for (Map.Entry<String, Double> entry : biomeWind.multipliers.entrySet()) {
                builder.append(toTomlString(entry.getKey())).append(" = ").append(entry.getValue()).append('\n');
            }
        }
        return builder.toString();
    }

    private static void appendComment(StringBuilder builder, String comment) {
        builder.append('#').append(comment).append('\n');
    }

    private static void appendSection(StringBuilder builder, String section) {
        builder.append('[').append(section).append("]\n");
    }

    private static void appendTomlValue(StringBuilder builder, String key, Object value) {
        builder.append(key).append(" = ");
        if (value instanceof String stringValue) {
            builder.append(toTomlString(stringValue));
        } else {
            builder.append(value);
        }
        builder.append('\n');
    }

    private static void appendStringListToml(StringBuilder builder, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            builder.append(key).append(" = []\n");
            return;
        }

        builder.append(key).append(" = [\n");
        for (int i = 0; i < values.size(); i++) {
            builder.append("  ").append(toTomlString(values.get(i)));
            if (i < values.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("]\n");
    }

    private static boolean isSection(String section, String... candidates) {
        String normalized = section == null ? "" : section.trim();
        for (String candidate : candidates) {
            if (normalized.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String stripInlineComment(String line) {
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (c == '#' && !inString) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int findUnquotedEquals(String line) {
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (c == '=' && !inString) {
                return i;
            }
        }
        return -1;
    }

    private static String parseTomlKey(String rawKey) {
        String key = rawKey.trim();
        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
            return unescapeTomlString(key.substring(1, key.length() - 1));
        }
        return key;
    }

    private static String parseString(String rawValue, String fallback) {
        String value = rawValue.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return unescapeTomlString(value.substring(1, value.length() - 1));
        }
        return value.isEmpty() ? fallback : value;
    }

    private static WindDensityMode parseWindDensityMode(String rawValue, WindDensityMode fallback, TomlParseState state) {
        String value = parseString(rawValue, fallback.name()).trim().toUpperCase(Locale.ROOT);
        try {
            return WindDensityMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            state.hadParseProblems = true;
            return fallback;
        }
    }

    private static boolean parseBoolean(String rawValue, boolean fallback, TomlParseState state) {
        String value = rawValue.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        state.hadParseProblems = true;
        return fallback;
    }

    private static int parseInt(String rawValue, int fallback, TomlParseState state) {
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (RuntimeException e) {
            state.hadParseProblems = true;
            return fallback;
        }
    }

    private static double parseDouble(String rawValue, double fallback, TomlParseState state) {
        try {
            return Double.parseDouble(rawValue.trim());
        } catch (RuntimeException e) {
            state.hadParseProblems = true;
            return fallback;
        }
    }

    private static List<String> parseStringList(String rawValue, List<String> fallback, TomlParseState state) {
        String value = rawValue.trim();
        if (!value.startsWith("[") || !isCompleteTomlArray(value)) {
            state.hadParseProblems = true;
            return fallback;
        }

        List<String> result = new ArrayList<>();
        String inner = value.substring(1, value.lastIndexOf(']')).trim();
        if (inner.isEmpty()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }
            if (c == ',' && !inString) {
                addParsedListValue(result, current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (inString) {
            state.hadParseProblems = true;
            return fallback;
        }
        addParsedListValue(result, current.toString());
        return result;
    }

    private static void addParsedListValue(List<String> result, String rawValue) {
        String parsed = parseString(rawValue.trim(), "");
        if (!parsed.isEmpty()) {
            result.add(parsed);
        }
    }

    private static String toTomlString(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String unescapeTomlString(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    default -> builder.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                builder.append(c);
            }
        }
        if (escaped) {
            builder.append('\\');
        }
        return builder.toString();
    }

    private static boolean isTagRule(String entry) {
        return entry.startsWith("#") && entry.length() > 1;
    }

    private static String normalizeRuleKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isCompleteTomlArray(String value) {
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        boolean sawOpeningBracket = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '[') {
                    depth++;
                    sawOpeningBracket = true;
                } else if (c == ']') {
                    depth--;
                    if (sawOpeningBracket && depth <= 0) {
                        return true;
                    }
                }
            }
        }
        return sawOpeningBracket && depth <= 0 && !inString;
    }

    private static class TomlReadResult {
        private final WindyConfig config;
        private final boolean hadParseProblems;

        private TomlReadResult(WindyConfig config, boolean hadParseProblems) {
            this.config = config;
            this.hadParseProblems = hadParseProblems;
        }
    }

    private static class TomlParseState {
        private boolean hadParseProblems = false;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static double getDouble(JsonObject object, String key, double fallback) {
        try {
            return object.has(key) ? object.get(key).getAsDouble() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public enum WindDensityMode {
        CONSTANT,
        Y_LEVEL_SCALING
    }

    public static class ConstantWind {
        public int minimumWindHeight = 83;
        public double spawnRateMultiplier = 2.0D;
    }

    public static class YLevelScalingWind {
        public int minimumWindHeight = 83;
        public double minimumHeightSpawnRateMultiplier = 1.0D;
        public int maximumWindHeight = 180;
        public double maximumHeightSpawnRateMultiplier = 4.0D;
    }

    public static class BiomeWind {
        public boolean enabled = false;
        public List<String> blacklist = new ArrayList<>();
        public Map<String, Double> multipliers = new LinkedHashMap<>();

        public BiomeWind() {
            multipliers.put("#minecraft:is_ocean", 1.5D);
            multipliers.put("#minecraft:is_mountain", 2.0D);
            multipliers.put("minecraft:desert", 0.6D);
        }

        private void sanitize() {
            if (blacklist == null) {
                blacklist = new ArrayList<>();
            }
            List<String> cleanBlacklist = new ArrayList<>();
            for (String entry : blacklist) {
                String normalized = normalizeRuleKey(entry);
                if (!normalized.isEmpty()) {
                    cleanBlacklist.add(normalized);
                }
            }
            blacklist = cleanBlacklist;

            if (multipliers == null) {
                multipliers = new LinkedHashMap<>();
            }
            Map<String, Double> cleanMultipliers = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : multipliers.entrySet()) {
                String key = normalizeRuleKey(entry.getKey());
                if (key.isEmpty() || entry.getValue() == null) {
                    continue;
                }
                cleanMultipliers.put(key, clampDouble(entry.getValue(), 0.0D, 100.0D));
            }
            multipliers = cleanMultipliers;
        }
    }
}
