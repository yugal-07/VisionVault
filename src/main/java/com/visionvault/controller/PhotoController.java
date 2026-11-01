package com.visionvault.controller;

import com.visionvault.dto.SearchResult;
import com.visionvault.model.Photo;
import com.visionvault.repository.PhotoRepository;
import com.visionvault.service.FaceRecognitionService;
import com.visionvault.service.IndexingService;
import com.visionvault.service.PhotoStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PhotoController {
    
    @Autowired
    private PhotoStorageService storageService;
    
    @Autowired
    private IndexingService indexingService;
    
    @Autowired
    private FaceRecognitionService faceRecognitionService;
    
    @Autowired
    private PhotoRepository photoRepository;
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadPhotos(@RequestParam("files") MultipartFile[] files) {
        try {
            List<Photo> saved = storageService.storePhotos(List.of(files));
            return ResponseEntity.ok("Uploaded " + saved.size() + " photos successfully");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/index/start")
    public ResponseEntity<String> startIndexing() {
        String result = indexingService.startIndexing();
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/index/progress")
    public ResponseEntity<String> getProgress() {
        return ResponseEntity.ok(indexingService.getProgress());
    }
    
    @PostMapping("/search")
    public ResponseEntity<List<SearchResult>> searchByFace(@RequestParam("image") MultipartFile selfie) {
        try {
            List<Photo> matches = faceRecognitionService.searchByFace(selfie.getBytes());
            List<SearchResult> results = new ArrayList<>();
            for (Photo photo : matches) {
                results.add(new SearchResult(
                    photo.getId(),
                    photo.getFileName(),
                    "/api/photos/" + photo.getId() + "/thumbnail",
                    "/api/photos/" + photo.getId() + "/image"
                ));
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/photos/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id) {
        try {
            byte[] data = storageService.loadThumbnail(id);
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(data);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/photos/{id}/image")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        try {
            byte[] data = storageService.loadPhoto(id);
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(data);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/photos/count")
    public ResponseEntity<Long> getPhotoCount() {
        return ResponseEntity.ok(photoRepository.count());
    }
}
