package com.visionvault.service;

import com.visionvault.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IndexingService {
    
    @Autowired
    private PhotoRepository photoRepository;
    
    @Autowired
    private FaceRecognitionService faceRecognitionService;
    
    private final AtomicInteger indexedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private boolean isIndexing = false;
    
    public String startIndexing() {
        if (isIndexing) {
            return "Indexing already in progress";
        }
        
        int unindexed = photoRepository.findByIndexedFalse().size();
        if (unindexed == 0) {
            return "No photos to index";
        }
        
        isIndexing = true;
        totalCount.set(unindexed);
        indexedCount.set(0);
        
        new Thread(() -> {
            try {
                int indexed = faceRecognitionService.indexPhotos();
                indexedCount.set(indexed);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isIndexing = false;
            }
        }).start();
        
        return "Indexing started for " + unindexed + " photos";
    }
    
    public String getProgress() {
        int indexed = indexedCount.get();
        int total = totalCount.get();
        
        if (total == 0) {
            return "No indexing in progress";
        }
        
        return indexed + " of " + total + " photos done";
    }
    
    public boolean isComplete() {
        return indexedCount.get() >= totalCount.get() && !isIndexing;
    }
}
