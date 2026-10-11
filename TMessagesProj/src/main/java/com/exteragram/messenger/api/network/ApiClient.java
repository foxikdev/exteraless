package com.exteragram.messenger.api.network;

import com.exteragram.messenger.ExteraConfig;

/** Compatibility holder for the original API client names. */
public final class ApiClient {
    private static final String BASE_URL = "https://api.exteragram.app/api/v1/";
    public static final ApiClient INSTANCE = new ApiClient();
    private final ApiService apiService = new ApiService() { };

    private ApiClient() {
    }

    public boolean getRequestsEnabled$TMessagesProj() {
        return !ExteraConfig.getApiBaseUrl().trim().isEmpty();
    }

    public ApiService getApiService() {
        return apiService;
    }

    public String getBaseUrl() {
        return BASE_URL;
    }
}
