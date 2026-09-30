package com.lms.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

public class UpdateManager {

    private static final String REPO_OWNER = "snowRepo";
    private static final String REPO_NAME = "LMS-OFF";
    public static final String CURRENT_VERSION;
    
    static {
        String ver = "v1.0.0"; // fallback
        try (InputStream is = UpdateManager.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                ver = props.getProperty("version", "v1.0.0");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CURRENT_VERSION = ver;
    }
    
    public record ReleaseInfo(String version, String releaseNotes, String downloadUrl) {}

    /**
     * Checks GitHub for the latest release.
     */
    public static Optional<ReleaseInfo> checkForUpdates() throws Exception {
        String url = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
                
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
                
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String tagName = json.has("tag_name") ? json.get("tag_name").getAsString() : "";
            String body = json.has("body") ? json.get("body").getAsString() : "No release notes provided.";
            
            // Compare version
            if (!tagName.isEmpty() && isNewerVersion(CURRENT_VERSION, tagName)) {
                
                // Find correct asset for OS
                String os = System.getProperty("os.name").toLowerCase();
                String ext = os.contains("win") ? ".exe" : os.contains("mac") ? ".dmg" : ".deb"; // fallback linux
                
                if (json.has("assets") && json.get("assets").isJsonArray()) {
                    JsonArray assets = json.getAsJsonArray("assets");
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String assetName = asset.get("name").getAsString();
                        if (assetName.endsWith(ext)) {
                            String downloadUrl = asset.get("browser_download_url").getAsString();
                            return Optional.of(new ReleaseInfo(tagName, body, downloadUrl));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * Downloads the update and reports progress.
     */
    public static File downloadUpdate(String downloadUrl, Consumer<Double> progressCallback) throws Exception {
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .build();
                
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        
        if (response.statusCode() != 200) {
            throw new Exception("Failed to download update. HTTP " + response.statusCode());
        }
        
        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        
        try (InputStream is = response.body(); FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;
                if (contentLength > 0 && progressCallback != null) {
                    double progress = (double) totalRead / contentLength;
                    progressCallback.accept(progress);
                }
            }
        }
        
        return tempFile;
    }
    
    public static void executeUpdate(File installerFile) throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            // macOS: mounting a DMG or running a pkg
            Runtime.getRuntime().exec(new String[]{"open", installerFile.getAbsolutePath()});
        } else {
            // Windows/Linux executable
            java.awt.Desktop.getDesktop().open(installerFile);
        }
        System.exit(0);
    }
    
    private static boolean isNewerVersion(String current, String remote) {
        String c = current.replace("v", "").trim();
        String r = remote.replace("v", "").trim();
        // Basic semantic versioning check
        try {
            String[] cParts = c.split("\\.");
            String[] rParts = r.split("\\.");
            int length = Math.max(cParts.length, rParts.length);
            for (int i = 0; i < length; i++) {
                int cVal = i < cParts.length ? Integer.parseInt(cParts[i]) : 0;
                int rVal = i < rParts.length ? Integer.parseInt(rParts[i]) : 0;
                if (rVal > cVal) return true;
                if (rVal < cVal) return false;
            }
        } catch (Exception e) {
            return !c.equals(r); // fallback to simple string comparison
        }
        return false;
    }
}
