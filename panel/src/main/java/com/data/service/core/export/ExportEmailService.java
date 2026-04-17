package com.data.service.core.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ExportEmailService {

    private static final Logger log = LoggerFactory.getLogger(ExportEmailService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Map<String, List<String>> SUPPORTED_CONTENT_TYPES = Map.of(
            "csv", List.of("text/csv", "text/csv;charset=utf-8", "text/csv;charset=utf-8;"),
            "xlsx", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    );
    private static final int MAX_RECIPIENTS = 25;
    private static final int MAX_ATTACHMENTS = 2;
    private static final int MAX_ATTACHMENT_BYTES = 5 * 1024 * 1024;

    public ExportEmailResponse accept(String entity, ExportEmailRequest request) {
        if (request == null) {
            throw badRequest("Export email request body is required.");
        }

        List<String> to = normalizeRequiredEmails(request.getTo(), "to");
        String from = normalizeSender(request.getFrom());
        List<String> cc = normalizeOptionalEmails(request.getCc(), "cc");
        List<NormalizedAttachment> attachments = normalizeAttachments(request.getAttachments());
        validateRecipientLimit(to.size() + cc.size());

        log.info(
                "Accepted export email request: entity={}, from={}, toCount={}, ccCount={}, recipientCount={}, attachmentCount={}, attachmentNames={}, totalAttachmentBytes={}, deliveryMode=log-only",
                entity,
                from,
                to.size(),
                cc.size(),
                to.size() + cc.size(),
                attachments.size(),
                attachments.stream().map(NormalizedAttachment::fileName).toList(),
                attachments.stream().mapToInt(NormalizedAttachment::sizeBytes).sum()
        );

        return new ExportEmailResponse(
                "accepted",
                "log-only",
                to.size() + cc.size(),
                attachments.size(),
                "Export email request accepted in log-only mode."
        );
    }

    private List<String> normalizeRequiredEmails(List<String> rawEmails, String fieldName) {
        List<String> normalizedEmails = normalizeEmailList(rawEmails, fieldName);
        if (normalizedEmails.isEmpty()) {
            throw badRequest("At least one " + fieldName + " email address is required.");
        }
        return normalizedEmails;
    }

    private List<String> normalizeOptionalEmails(List<String> rawEmails, String fieldName) {
        return normalizeEmailList(rawEmails, fieldName);
    }

    private List<String> normalizeEmailList(List<String> rawEmails, String fieldName) {
        LinkedHashSet<String> uniqueRecipients = new LinkedHashSet<>();
        for (String rawRecipient : rawEmails == null ? List.<String>of() : rawEmails) {
            String normalizedRecipient = rawRecipient == null ? "" : rawRecipient.trim().toLowerCase(Locale.ROOT);
            if (normalizedRecipient.isEmpty()) {
                continue;
            }

            if (!EMAIL_PATTERN.matcher(normalizedRecipient).matches()) {
                throw badRequest("The " + fieldName + " email addresses must be valid.");
            }

            uniqueRecipients.add(normalizedRecipient);
        }

        return List.copyOf(uniqueRecipients);
    }

    private String normalizeSender(String rawFrom) {
        String from = rawFrom == null ? "" : rawFrom.trim().toLowerCase(Locale.ROOT);
        if (from.isEmpty()) {
            throw badRequest("A valid from email address is required.");
        }

        if (!EMAIL_PATTERN.matcher(from).matches()) {
            throw badRequest("The from email address must be valid.");
        }

        return from;
    }

    private void validateRecipientLimit(int recipientCount) {
        if (recipientCount > MAX_RECIPIENTS) {
            throw badRequest("Export emails support up to " + MAX_RECIPIENTS + " total to/cc recipients per request.");
        }
    }

    private List<NormalizedAttachment> normalizeAttachments(List<ExportEmailAttachmentRequest> rawAttachments) {
        if (rawAttachments == null || rawAttachments.isEmpty()) {
            throw badRequest("At least one export attachment is required.");
        }

        if (rawAttachments.size() > MAX_ATTACHMENTS) {
            throw badRequest("Export emails support up to " + MAX_ATTACHMENTS + " attachments per request.");
        }

        List<NormalizedAttachment> normalizedAttachments = new ArrayList<>();
        for (ExportEmailAttachmentRequest rawAttachment : rawAttachments) {
            if (rawAttachment == null) {
                throw badRequest("Export attachments cannot be empty.");
            }

            String fileName = normalizeFileName(rawAttachment.getFileName());
            String contentType = normalizeContentType(rawAttachment.getContentType(), fileName);
            int sizeBytes = normalizeAttachmentBytes(rawAttachment.getFileBase64());

            normalizedAttachments.add(new NormalizedAttachment(fileName, sizeBytes));
        }

        return List.copyOf(normalizedAttachments);
    }

    private String normalizeFileName(String rawFileName) {
        String fileName = rawFileName == null ? "" : rawFileName.trim();
        if (fileName.isEmpty()) {
            throw badRequest("Each export attachment must include a file name.");
        }

        if (resolveAttachmentExtension(fileName) == null) {
            throw badRequest("Attachment file names must end with .csv or .xlsx.");
        }

        return fileName;
    }

    private String normalizeContentType(String rawContentType, String fileName) {
        String contentType = rawContentType == null ? "" : rawContentType.trim().toLowerCase(Locale.ROOT);
        String attachmentExtension = resolveAttachmentExtension(fileName);
        List<String> supportedTypes = attachmentExtension == null ? null : SUPPORTED_CONTENT_TYPES.get(attachmentExtension);
        if (supportedTypes == null || !supportedTypes.contains(contentType)) {
            throw badRequest("Attachment content type does not match the selected export format.");
        }
        return contentType;
    }

    private String resolveAttachmentExtension(String fileName) {
        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
        if (normalizedFileName.endsWith(".csv")) {
            return "csv";
        }
        if (normalizedFileName.endsWith(".xlsx")) {
            return "xlsx";
        }
        return null;
    }

    private int normalizeAttachmentBytes(String rawBase64) {
        String fileBase64 = rawBase64 == null ? "" : rawBase64.trim();
        if (fileBase64.isEmpty()) {
            throw badRequest("Each export attachment must include a base64 payload.");
        }

        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(fileBase64);
        } catch (IllegalArgumentException exception) {
            throw badRequest("Export attachments must contain valid base64 content.");
        }

        if (decodedBytes.length == 0) {
            throw badRequest("Export attachments cannot be empty.");
        }

        if (decodedBytes.length > MAX_ATTACHMENT_BYTES) {
            throw badRequest("Each export attachment must be 5 MB or smaller.");
        }

        return decodedBytes.length;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record NormalizedAttachment(
            String fileName,
            int sizeBytes
    ) {
    }
}
