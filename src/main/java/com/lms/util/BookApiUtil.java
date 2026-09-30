package com.lms.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public class BookApiUtil {
    
    public record BookMetadata(String title, String author, String publishedYear, String description) {}
    
    public static Optional<BookMetadata> fetchBookByIsbn(String isbn) throws Exception {
        String cleanIsbn = isbn.replaceAll("[^0-9Xx]", "");
        if (cleanIsbn.isEmpty()) return Optional.empty();
        
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + cleanIsbn;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
                
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
                
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("items") && json.get("items").isJsonArray()) {
                JsonArray items = json.getAsJsonArray("items");
                if (!items.isEmpty()) {
                    JsonObject volumeInfo = items.get(0).getAsJsonObject().getAsJsonObject("volumeInfo");
                    
                    String title = volumeInfo.has("title") ? volumeInfo.get("title").getAsString() : "";
                    
                    String author = "";
                    if (volumeInfo.has("authors") && volumeInfo.get("authors").isJsonArray()) {
                        JsonArray authors = volumeInfo.getAsJsonArray("authors");
                        if (!authors.isEmpty()) {
                            author = authors.get(0).getAsString();
                        }
                    }
                    
                    String year = "";
                    if (volumeInfo.has("publishedDate")) {
                        String date = volumeInfo.get("publishedDate").getAsString();
                        if (date.length() >= 4) {
                            year = date.substring(0, 4);
                        }
                    }
                    
                    String description = volumeInfo.has("description") ? volumeInfo.get("description").getAsString() : "";
                    
                    return Optional.of(new BookMetadata(title, author, year, description));
                }
            }
        }
        return Optional.empty();
    }
}
