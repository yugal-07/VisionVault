package com.visionvault.dto;

public class SearchResult {
    private Long id;
    private String fileName;
    private String thumbnailUrl;
    private String imageUrl;
    
    public SearchResult() {}
    
    public SearchResult(Long id, String fileName, String thumbnailUrl, String imageUrl) {
        this.id = id;
        this.fileName = fileName;
        this.thumbnailUrl = thumbnailUrl;
        this.imageUrl = imageUrl;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
