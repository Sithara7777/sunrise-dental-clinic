package lk.icbt.cis6003.dental.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * A slice of a larger result set.
 *
 * <p>Spring's own {@code Page} is not stable across the wire, so the API
 * publishes this minimal, framework-free shape instead. That keeps the desktop
 * client free of any Spring dependency - it only needs the {@code dental-common}
 * jar and an HTTP library.</p>
 *
 * @param <T> element type
 */
public class PageResponse<T> {

    private List<T> content = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public PageResponse() {
        // required by Jackson
    }

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content == null ? new ArrayList<>() : new ArrayList<>(content);
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        this.first = page == 0;
        this.last = totalPages == 0 || page >= totalPages - 1;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content == null ? new ArrayList<>() : new ArrayList<>(content);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }
}
