package com.visionvault.service;

import com.visionvault.model.Photo;
import com.visionvault.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoStorageService {
    
    @Value("${visionvault.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${visionvault.thumbnail-dir:thumbnails}")
    private String thumbnailDir;
    
    @Autowired
    private PhotoRepository photoRepository;
    
    private final int THUMBNAIL_WIDTH = 300;
    private final int THUMBNAIL_HEIGHT = 300;
    
    public List<Photo> storePhotos(List<MultipartFile> files) throws IOException {
        List<Photo> savedPhotos = new ArrayList<>();
        
        Path uploadPath = Paths.get(uploadDir);
        Path thumbnailPath = Paths.get(thumbnailDir);
        Files.createDirectories(uploadPath);
        Files.createDirectories(thumbnailPath);
        
        for (MultipartFile file : files) {
            if (file.isEmpty() || !file.getContentType().startsWith("image/")) {
                continue;
            }
            
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String newFilename = UUID.randomUUID().toString() + extension;
            
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);
            
            String thumbnailFilename = createThumbnail(file, newFilename);
            
            Photo photo = new Photo();
            photo.setFileName(originalFilename != null ? originalFilename : newFilename);
            photo.setOriginalPath(filePath.toString());
            photo.setThumbnailPath(thumbnailPath.resolve(thumbnailFilename).toString());
            photo.setUploadTime(LocalDateTime.now());
            photo.setIndexed(false);
            
            savedPhotos.add(photoRepository.save(photo));
        }
        
        return savedPhotos;
    }
    
    private String createThumbnail(MultipartFile file, String originalFilename) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("Could not read image file");
        }
        
        BufferedImage thumbnail = scaleImage(originalImage, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        
        String thumbnailFilename = "thumb_" + originalFilename;
        Path thumbnailPath = Paths.get(this.thumbnailDir).resolve(thumbnailFilename);
        ImageIO.write(thumbnail, "jpg", thumbnailPath.toFile());
        
        return thumbnailFilename;
    }
    
    private BufferedImage scaleImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return scaled;
    }
    
    public byte[] loadPhoto(Long id) throws IOException {
        Photo photo = photoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Photo not found"));
        return Files.readAllBytes(Paths.get(photo.getOriginalPath()));
    }
    
    public byte[] loadThumbnail(Long id) throws IOException {
        Photo photo = photoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Photo not found"));
        return Files.readAllBytes(Paths.get(photo.getThumbnailPath()));
    }
}
