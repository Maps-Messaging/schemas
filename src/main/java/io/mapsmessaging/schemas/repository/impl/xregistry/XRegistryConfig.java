package io.mapsmessaging.schemas.repository.impl.xregistry;


public class XRegistryConfig {
  private String baseUrl;
  private String apiKey;
  private String groupName = "schemas";
  private int timeout = 30000; // ms
  private int retryAttempts = 3;
  private int retryDelay = 1000; // ms
  private boolean enableCache = true;
  private int cacheSize = 100;
  private long cacheTtl = 3600000; // 1 hour in ms

  // Getters
  public String getBaseUrl() {
    return baseUrl;
  }

  // Setters
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public int getRetryAttempts() {
    return retryAttempts;
  }

  public void setRetryAttempts(int retryAttempts) {
    this.retryAttempts = retryAttempts;
  }

  public int getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(int retryDelay) {
    this.retryDelay = retryDelay;
  }

  public boolean isEnableCache() {
    return enableCache;
  }

  public void setEnableCache(boolean enableCache) {
    this.enableCache = enableCache;
  }

  public int getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  public long getCacheTtl() {
    return cacheTtl;
  }

  public void setCacheTtl(long cacheTtl) {
    this.cacheTtl = cacheTtl;
  }
}
