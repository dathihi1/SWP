package com.badat.study1.util;

/**
 * Utility class for pagination validation and sanitization
 */
public class PaginationValidator {
    
    private static final int DEFAULT_PAGE = 0;
    private static final int MIN_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
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


