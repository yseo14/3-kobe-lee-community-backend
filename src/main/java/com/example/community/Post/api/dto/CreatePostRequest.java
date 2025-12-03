package com.example.community.Post.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePostRequest(
        @Size(max = 26, message = "제목은 최대 26자까지 작성 가능합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        List<String> objectKeys,  // 이미지 업로드 후 S3에 저장된 objectKey 리스트

        String thumbnailObjectKey
) {
}
