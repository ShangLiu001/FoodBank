package com.primaryfeed.dto;

/**
 * Lightweight projection for branch selector dropdowns.
 * Returns only the fields needed by frontend &lt;select&gt; elements.
 */
public record BranchLookup(Integer branchId, String branchName) {}
