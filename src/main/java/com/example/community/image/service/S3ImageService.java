package com.example.community.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageService {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 임시 폴더의 이미지를 영구 폴더로 이동
     * @param tempKey 예: temp/profile/uuid.jpg
     * @param newKey 예: public/image/profile/uuid.jpg
     */
    public void moveImage(String tempKey, String newKey) {
        try {
            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(tempKey)
                    .destinationBucket(bucket)
                    .destinationKey(newKey)
                    .build();

            s3Client.copyObject(copyReq);

            deleteImage(tempKey);

            log.info("S3 이미지 이동 성공: {} -> {}", tempKey, newKey);
        } catch (Exception e) {
            log.error("S3 이미지 이동 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 저장 중 오류가 발생했습니다.");
        }
    }

    /**
     * 이미지 삭제 (사용 안 하는 이미지 정리용)
     */
    public void deleteImage(String key) {
        if (key == null || key.isBlank()) return;

        try {
            DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteReq);
        } catch (Exception e) {
            log.error("S3 이미지 삭제 실패: {}", key);
        }
    }
}