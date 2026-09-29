package org.rostislav.curiokeep.items;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

public enum ExportFormat {
    /** Lossless: every module of the collection in one file that can be imported again. */
    JSON("json", MediaType.APPLICATION_JSON),
    /** One module as a table for spreadsheets; it cannot be imported. */
    CSV("csv", MediaType.parseMediaType("text/csv;charset=UTF-8"));

    private final String extension;
    private final MediaType mediaType;

    ExportFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public static ExportFormat parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_EXPORT_FORMAT");
        }
    }
}
