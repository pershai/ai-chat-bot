package com.example.aichatbot.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentRequest {

    @Size(min = 1, max = 255, message = "Filename must be between 1 and 255 characters")
    private String filename;

    @Size(max = 2000, message = "Summary cannot exceed 2000 characters")
    private String summary;
}
