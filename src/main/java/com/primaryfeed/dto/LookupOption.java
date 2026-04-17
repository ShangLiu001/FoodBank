package com.primaryfeed.dto;

/**
 * Generic lightweight projection for selector dropdowns.
 * Used by /api/lookups/* endpoints for donors and beneficiaries.
 */
public record LookupOption(Integer id, String name) {}
