package com.data.service.core.export;

import lombok.Data;

@Data
public class ExportEmailAttachmentRequest {
    private String fileName;
    private String contentType;
    private String fileBase64;
}
