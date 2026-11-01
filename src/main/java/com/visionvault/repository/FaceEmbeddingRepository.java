package com.visionvault.repository;

import com.visionvault.model.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, Long> {
    void deleteByPhotoId(Long photoId);
    List<FaceEmbedding> findByPhotoId(Long photoId);
}
