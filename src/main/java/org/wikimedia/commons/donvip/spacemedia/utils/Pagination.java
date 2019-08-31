package org.wikimedia.commons.donvip.spacemedia.utils;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

public final class Pagination {

    private Pagination() {
    }

    public static void setPageNumbers(Model model, Page<?> page) {
        int totalPages = page.getTotalPages();
        if (totalPages > 0) {
            model.addAttribute("pageNumbers",
                    IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList()));
        }
    }
}
