package mw.loancompliance.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(List<T> records, int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100");
        }

        long from = (long) page * size;
        int start = (int) Math.min(from, records.size());
        int end = Math.min(start + size, records.size());
        int totalPages = (int) Math.ceil(records.size() / (double) size);
        return new PageResponse<>(List.copyOf(records.subList(start, end)), page, size,
                records.size(), totalPages);
    }
}
