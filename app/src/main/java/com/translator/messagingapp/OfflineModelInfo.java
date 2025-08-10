package com.translator.messagingapp;

/**
 * Model class representing an offline translation model.
 */
public class OfflineModelInfo {
    private String languageCode;
    private String languageName;
    private long sizeBytes;
    private boolean isDownloaded;
    private boolean isDownloading;
    private int downloadProgress;
    
    public OfflineModelInfo(String languageCode, String languageName, long sizeBytes) {
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.sizeBytes = sizeBytes;
        this.isDownloaded = false;
        this.isDownloading = false;
        this.downloadProgress = 0;
    }
    
    public String getLanguageCode() {
        return languageCode;
    }
    
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
    
    public String getLanguageName() {
        return languageName;
    }
    
    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }
    
    public long getSizeBytes() {
        return sizeBytes;
    }
    
    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    public boolean isDownloaded() {
        return isDownloaded;
    }
    
    public void setDownloaded(boolean downloaded) {
        this.isDownloaded = downloaded;
    }
    
    public boolean isDownloading() {
        return isDownloading;
    }
    
    public void setDownloading(boolean downloading) {
        this.isDownloading = downloading;
    }
    
    public int getDownloadProgress() {
        return downloadProgress;
    }
    
    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }
    
    /**
     * Get human-readable file size.
     */
    public String getFormattedSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OfflineModelInfo that = (OfflineModelInfo) obj;
        return languageCode != null ? languageCode.equals(that.languageCode) : that.languageCode == null;
    }
    
    @Override
    public int hashCode() {
        return languageCode != null ? languageCode.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "OfflineModelInfo{" +
                "languageCode='" + languageCode + '\'' +
                ", languageName='" + languageName + '\'' +
                ", sizeBytes=" + sizeBytes +
                ", isDownloaded=" + isDownloaded +
                ", isDownloading=" + isDownloading +
                ", downloadProgress=" + downloadProgress +
                '}';
    }
}