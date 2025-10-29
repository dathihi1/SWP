package com.badat.study1.util;

/**
 * Utility class for pagination validation and sanitization
 */
public class PaginationValidator {
    
    private static final int DEFAULT_PAGE = 0; // zero-based default
    private static final int MIN_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;
    
    /**
     * Validate and sanitize page number
     * @param page Requested page number
     * @return Validated page number (min: 0)
     */
    public static int validatePage(int page) {
        if (page < MIN_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /**
     * Validate 1-based page coming from UI; returns a 1-based page >= 1
     */
    public static int validateOneBasedPage(int pageOneBased) {
        if (pageOneBased < 1) {
            return 1;
        }
        return pageOneBased;
    }

    /**
     * Convert 1-based page to zero-based index
     */
    public static int toZeroBased(int pageOneBased) {
        return Math.max(0, pageOneBased - 1);
    }
    
    /**
     * Validate and sanitize page size
     * @param size Requested page size
     * @return Validated size (min: 1, max: 100)
     */
    public static int validateSize(int size) {
        if (size < MIN_SIZE) {
            return MIN_SIZE;
        }
        if (size > MAX_SIZE) {
            return MAX_SIZE;
        }
        return size;
    }
    
    /**
     * Validate page number against total pages
     * @param page Current page
     * @param totalPages Total number of pages
     * @return Validated page number
     */
    public static int validatePageAgainstTotal(int page, int totalPages) {
        if (totalPages == 0) {
            return 0;
        }
        
        if (page < 0) {
            return 0;
        }
        
        if (page >= totalPages) {
            return totalPages - 1;
        }
        
        return page;
    }

    /**
     * Clamp a 1-based page to [1..totalPages], returns 1 if totalPages == 0
     */
    public static int clampOneBased(int pageOneBased, int totalPages) {
        if (totalPages <= 0) {
            return 1;
        }
        if (pageOneBased < 1) {
            return 1;
        }
        if (pageOneBased > totalPages) {
            return totalPages;
        }
        return pageOneBased;
    }
    
    /**
     * Get default page number
     */
    public static int getDefaultPage() {
        return DEFAULT_PAGE;
    }
    
    /**
     * Get default page size
     */
    public static int getDefaultSize() {
        return DEFAULT_SIZE;
    }
    
    /**
     * Get minimum page number
     */
    public static int getMinPage() {
        return MIN_PAGE;
    }
    
    /**
     * Get minimum page size
     */
    public static int getMinSize() {
        return MIN_SIZE;
    }
    
    /**
     * Get maximum page size
     */
    public static int getMaxSize() {
        return MAX_SIZE;
    }
}





