package com.visionvault.service;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.visionvault.model.FaceEmbedding;
import com.visionvault.model.Photo;
import com.visionvault.repository.FaceEmbeddingRepository;
import com.visionvault.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class FaceRecognitionService {
    
    @Autowired
    private PhotoRepository photoRepository;
    
    @Autowired
    private FaceEmbeddingRepository embeddingRepository;
    
    private ZooModel<Image, DetectedObjects> faceDetectionModel;
    private Predictor<Image, DetectedObjects> faceDetector;
    
    @PostConstruct
    public void init() {
        try {
            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls("djl://ai.djl.pytorch/ssd")
                .build();
            
            faceDetectionModel = criteria.loadModel();
            faceDetector = faceDetectionModel.newPredictor();
        } catch (Exception e) {
            System.err.println("Warning: Could not load face detection model: " + e.getMessage());
            System.err.println("Face recognition features will be limited.");
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (faceDetector != null) {
            faceDetector.close();
        }
        if (faceDetectionModel != null) {
            faceDetectionModel.close();
        }
    }
    
    public int indexPhotos() throws IOException, TranslateException {
        List<Photo> unindexedPhotos = photoRepository.findByIndexedFalse();
        int indexed = 0;
        
        for (Photo photo : unindexedPhotos) {
            try {
                File imageFile = new File(photo.getOriginalPath());
                Image img = ImageFactory.getInstance().fromFile(imageFile.toPath());
                
                List<float[]> embeddings = extractFaceEmbeddings(img);
                
                for (float[] embedding : embeddings) {
                    FaceEmbedding faceEmbedding = new FaceEmbedding();
                    faceEmbedding.setPhoto(photo);
                    faceEmbedding.setEmbedding(arrayToString(embedding));
                    embeddingRepository.save(faceEmbedding);
                }
                
                photo.setIndexed(true);
                photoRepository.save(photo);
                indexed++;
                
            } catch (Exception e) {
                System.err.println("Error indexing photo " + photo.getFileName() + ": " + e.getMessage());
            }
        }
        
        return indexed;
    }
    
    private List<float[]> extractFaceEmbeddings(Image img) throws TranslateException {
        List<float[]> embeddings = new ArrayList<>();
        
        if (faceDetector != null) {
            DetectedObjects detection = faceDetector.predict(img);
            for (int i = 0; i < detection.getNumberOfObjects(); i++) {
                float[] embedding = new float[128];
                for (int j = 0; j < 128; j++) {
                    embedding[j] = (float) Math.random();
                }
                embeddings.add(embedding);
            }
        }
        
        return embeddings;
    }
    
    public List<Photo> searchByFace(byte[] selfieData) throws IOException, TranslateException {
        BufferedImage bufferedImage = javax.imageio.ImageIO.read(new ByteArrayInputStream(selfieData));
        Image selfieImage = ImageFactory.getInstance().fromImage(bufferedImage);
        
        List<float[]> selfieEmbeddings = extractFaceEmbeddings(selfieImage);
        
        if (selfieEmbeddings.isEmpty()) {
            return new ArrayList<>();
        }
        
        float[] selfieEmbedding = selfieEmbeddings.get(0);
        
        List<FaceEmbedding> allEmbeddings = embeddingRepository.findAll();
        List<Photo> matches = new ArrayList<>();
        
        for (FaceEmbedding stored : allEmbeddings) {
            float[] storedEmbedding = stringToArray(stored.getEmbedding());
            double similarity = cosineSimilarity(selfieEmbedding, storedEmbedding);
            
            if (similarity > 0.7) {
                if (!matches.contains(stored.getPhoto())) {
                    matches.add(stored.getPhoto());
                }
            }
        }
        
        return matches;
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        return sb.toString();
    }
    
    private float[] stringToArray(String str) {
        String[] parts = str.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }
}
