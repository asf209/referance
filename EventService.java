package com.amadeus.commercial.services;

import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;

import org.apache.sling.api.resource.ResourceResolver;

import com.amadeus.commercial.pojos.Event;
import com.amadeus.commercial.pojos.FilterItem;
import com.day.cq.search.result.SearchResult;

public interface EventService {

    /**
     * Extracts the Regions filters from the available Events pages.
     */
    List<FilterItem> extractRegions(ResourceResolver resourceResolver, SearchResult results, Locale lang);

    /**
     * Extracts the custom date ranges.
     */
    List<FilterItem> extractCustomDateRanges(ResourceResolver resourceResolver, Locale lang);

    /**
     * Extracts the industries filters from the available Events pages.
     */
    List<FilterItem> extractCategories(ResourceResolver resourceResolver, SearchResult results, Locale lang);

    /**
     * Extracts the Events as search results.
     */
    SearchResult searchEvents(ResourceResolver resourceResolver, Locale locale, int start, Integer limit, String countryId) throws ServletException;

    /**
     * Returns a list of event pages from the events root in the system.
     */
    List<Event> buildEventList(ResourceResolver resourceResolver, SearchResult results, Locale lang, String countryId);

    /**
     * Search upcoming events by criteria
     */
    List<Event> searchUpcomingEvents(Locale locale, int start, Integer limit, List<String> tags, List<String> relatedProducts, String countryId);

}
