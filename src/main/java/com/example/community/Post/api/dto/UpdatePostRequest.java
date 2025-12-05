package com.example.community.Post.api.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdatePostRequest(
        @Size(max = 26, message = "제목은 최대 26자까지 작성 가능합니다.")
        String title,

        String content,

        /**
         * 최종 이미지 리스트 (기존 이미지 objectKey + 새 이미지 objectKey)
         * - 기존 이미지 objectKey: "public/image/xxx.jpg" (유지할 기존 이미지)
         * - 새 이미지 objectKey: "temp/xxx.jpg" (추가할 새 이미지, temp에서 public으로 이동됨)
         * - 이 리스트에 없는 기존 이미지는 삭제됨
         */
        List<String> objectKeys,

        String thumbnailObjectKey
) {
}
