package com.bossbank.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BossBankSyncHttpClient
{
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final Gson gson = new GsonBuilder().create();

    @Inject
    private BossBankSyncConfig config;

    public void postJson(String path, Object payload)
    {
        String baseUrl = trimTrailingSlash(config.apiBaseUrl());
        String apiKey = config.apiKey() == null ? "" : config.apiKey().trim();

        if (baseUrl.isEmpty())
        {
            log.warn("BossBank Sync: API base URL is empty");
            return;
        }

        try
        {
            String json = gson.toJson(payload);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

            if (!apiKey.isEmpty())
            {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                log.warn("BossBank Sync failed: {} {} -> {} {}", path, baseUrl, response.statusCode(), response.body());
                return;
            }

            if (config.debugLogging())
            {
                log.info("BossBank Sync success: {} -> {} {}", path, response.statusCode(), response.body());
            }
        }
        catch (IOException | InterruptedException e)
        {
            log.error("BossBank Sync error posting to {}", path, e);
            Thread.currentThread().interrupt();
        }
        catch (Exception e)
        {
            log.error("BossBank Sync unexpected error posting to {}", path, e);
        }
    }

    private String trimTrailingSlash(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
