package dev.steyon.translateMCPlugin.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TranslationAPI {
    private final String baseUrl;
    private final String apiToken;
    private final Logger logger;

    public TranslationAPI(String baseUrl, String apiToken, Logger logger) {
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
        this.logger = logger;
    }

    /**
     * Fetches all translation keys and languages from the API
     * @return APIResponse containing keys and languages
     */
    public APIResponse fetchTranslations() {
        try {
            URL url = new URL(baseUrl + "/keys");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (apiToken != null && !apiToken.isEmpty()) {
                connection.setRequestProperty("X-API-Key", apiToken);
            }
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return parseAPIResponse(response.toString());
            } else {
                logger.warning("API returned status code: " + responseCode);
                return new APIResponse(new HashMap<>(), new ArrayList<>());
            }
        } catch (Exception e) {
            logger.severe("Failed to fetch translations from API: " + e.getMessage());
            return new APIResponse(new HashMap<>(), new ArrayList<>());
        }
    }

    /**
     * Parses the JSON response from the API
     */
    private APIResponse parseAPIResponse(String jsonString) {
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

            // Parse keys and translations
            Map<String, Map<String, Translation>> translations = new HashMap<>();
            JsonArray keys = json.getAsJsonArray("keys");

            for (JsonElement keyElement : keys) {
                JsonObject keyObj = keyElement.getAsJsonObject();
                String key = keyObj.get("key").getAsString();
                JsonObject translationsObj = keyObj.getAsJsonObject("translations");

                Map<String, Translation> langTranslations = new HashMap<>();
                for (String lang : translationsObj.keySet()) {
                    JsonObject translationObj = translationsObj.getAsJsonObject(lang);
                    String value = translationObj.get("value").getAsString();
                    String status = translationObj.get("status").getAsString();

                    // Only add approved translations or non-empty values
                    if ("approved".equals(status) || !value.isEmpty()) {
                        langTranslations.put(lang, new Translation(value, status));
                    }
                }

                translations.put(key, langTranslations);
            }

            // Parse languages
            List<Language> languages = new ArrayList<>();
            JsonArray languagesArray = json.getAsJsonArray("languages");

            for (JsonElement langElement : languagesArray) {
                JsonObject langObj = langElement.getAsJsonObject();
                String code = langObj.get("code").getAsString();
                String name = langObj.get("name").getAsString();
                boolean isSource = langObj.get("is_source").getAsInt() == 1;
                String minecraftHead = langObj.has("minecraft_head") && !langObj.get("minecraft_head").isJsonNull()
                    ? langObj.get("minecraft_head").getAsString()
                    : null;

                languages.add(new Language(code, name, isSource, minecraftHead));
            }

            return new APIResponse(translations, languages);
        } catch (Exception e) {
            logger.severe("Failed to parse API response: " + e.getMessage());
            return new APIResponse(new HashMap<>(), new ArrayList<>());
        }
    }

    /**
     * Container for API response data
     */
    public static class APIResponse {
        private final Map<String, Map<String, Translation>> translations;
        private final List<Language> languages;

        public APIResponse(Map<String, Map<String, Translation>> translations, List<Language> languages) {
            this.translations = translations;
            this.languages = languages;
        }

        public Map<String, Map<String, Translation>> getTranslations() {
            return translations;
        }

        public List<Language> getLanguages() {
            return languages;
        }
    }

    /**
     * Represents a single translation
     */
    public static class Translation {
        private final String value;
        private final String status;

        public Translation(String value, String status) {
            this.value = value;
            this.status = status;
        }

        public String getValue() {
            return value;
        }

        public String getStatus() {
            return status;
        }

        public boolean isApproved() {
            return "approved".equals(status);
        }
    }

    /**
     * Represents a language
     */
    public static class Language {
        private final String code;
        private final String name;
        private final boolean isSource;
        private final String minecraftHead;

        public Language(String code, String name, boolean isSource, String minecraftHead) {
            this.code = code;
            this.name = name;
            this.isSource = isSource;
            this.minecraftHead = minecraftHead;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public boolean isSource() {
            return isSource;
        }

        public String getMinecraftHead() {
            return minecraftHead;
        }
    }
}
