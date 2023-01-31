package com.amadeus.commercial.services.impl;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amadeus.commercial.pojos.AvailabilityEnum;
import com.amadeus.commercial.pojos.Event;
import com.amadeus.commercial.pojos.FilterItem;
import com.amadeus.commercial.pojos.config.ConfigCommercial;
import com.amadeus.commercial.services.EventService;
import com.amadeus.commercial.services.ResourceResolverProvider;
import com.amadeus.commons.extension.api.ExtendedTag;
import com.amadeus.commons.extension.api.ExtendedTagManager;
import com.amadeus.commons.services.SiteConfiguration;
import com.amadeus.commons.utils.Constants;
import com.amadeus.commons.utils.TagsConstants;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.facets.Bucket;
import com.day.cq.search.facets.Facet;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(immediate = true, metatype = false, enabled = true)
@Service(value = EventService.class)
public class EventServiceImpl implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceImpl.class);
    private static final String NOT = "not";

    @Reference
    private SiteConfiguration siteConfiguration;

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private QueryBuilder queryBuilder;
    
    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public List<Event> buildEventList(ResourceResolver resourceResolver, SearchResult results, Locale lang, String countryId) {
        List<Event> pages = new LinkedList<>();
        try {
            if (results != null) {
                PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
                if (pageManager != null) {
                    for (Hit hit : results.getHits()) {
                        Page page = pageManager.getPage(hit.getPath());
                        if (page != null) {
                            pages.add(new Event(page, lang, resourceResolver, siteConfiguration));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting the events list to search through, " + e.getMessage());
        }
        return pages;
    }


    @Override
    public SearchResult searchEvents(ResourceResolver resourceResolver, Locale locale, int start, Integer limit, String countryId) throws ServletException {
        Map<String, String> map = buildQueryParameters(locale, null, null, countryId);
        return executeSearch(resourceResolver, map, start, limit);
    }

    @Override
    public List<FilterItem> extractRegions(ResourceResolver resourceResolver, SearchResult results, Locale lang) {
        return buildTagFilter(resourceResolver, results, lang, TagsConstants.EVENTS_LOCATION);
    }

    @Override
    public List<FilterItem> extractCategories(ResourceResolver resourceResolver, SearchResult results, Locale lang) {
        return buildTagFilter(resourceResolver, results, lang, TagsConstants.PRODUCT_INDUSTRY);
    }

    public List<FilterItem> extractCustomDateRanges(ResourceResolver resourceResolver, Locale lang) {
        List<FilterItem> items = new LinkedList<>();
        ExtendedTagManager tagManager = resourceResolver.adaptTo(ExtendedTagManager.class);
        ExtendedTag rootTag = tagManager.resolve(TagsConstants.DATE_RANGE);
        if (rootTag != null) {
            for (ExtendedTag tag : rootTag.getExtendedChildren()) {
                FilterItem filter = new FilterItem();
                filter.setName(tag.getName(lang));
                filter.setBaseName(tag.getName());
                filter.setTitle(tag.getTitle(lang));
                filter.setAlternativeText(tag.getName());
                items.add(filter);
            }
        }
        return items;
    }

    @Override
    public List<Event> searchUpcomingEvents(Locale locale, int start, Integer limit, List<String> tags, List<String> relatedProducts, String countryId) {
        List<Event> events;
        try (ResourceResolver resourceResolver = ResourceResolverProvider.getSearchResourceResolver(resolverFactory)) {
            Map<String, String> map = buildQueryParameters(locale, tags, relatedProducts, countryId);
            SimpleDateFormat iso8601 = new SimpleDateFormat(Constants.AEM_DATE_FORMAT);
            Date truncatedDate = DateUtils.truncate(new Date(), Calendar.DATE);
            map.put("daterange.property", "jcr:content/eventStartDate");
            map.put("daterange.lowerBound", iso8601.format(truncatedDate));
            map.put("daterange.lowerOperation", ">=");
            SearchResult results = executeSearch(resourceResolver, map, start, limit);
            events = buildEventList(resourceResolver, results, locale, countryId);
        } catch (LoginException e) {
            LOGGER.error(e.getMessage(), e);
            events = new LinkedList<>();
        }
        return events;
    }

    private List<FilterItem> buildTagFilter(ResourceResolver resourceResolver, SearchResult results, Locale lang, String tagPrefix) {
        List<FilterItem> items = new LinkedList<>();
        try {
            ExtendedTagManager tagManager = resourceResolver.adaptTo(ExtendedTagManager.class);
            Facet tagFacets = results.getFacets().get("tagid");
            if (tagFacets != null && tagManager != null) {
                for (Bucket bucket : tagFacets.getBuckets()) {
                    String item = bucket.getValue();
                    if (item.startsWith(tagPrefix)) {
                        ExtendedTag tag = tagManager.resolve(item);
                        if (tag != null) {
                            FilterItem filter = new FilterItem();
                            filter.setName(tag.getName(lang));
                            filter.setBaseName(tag.getName());
                            filter.setTitle(tag.getTitle(lang));
                            filter.setDescription(tag.getDescription(lang));
                            items.add(filter);
                        }
                    }
                }
                Collections.sort(items, (FilterItem o1, FilterItem o2) -> o1.getName().compareTo(o2.getName()));
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error getting industry tags for events ", e.getMessage(), e);
        }
        return items;
    }

    private Map<String, String> buildQueryParameters(Locale locale, List<String> tags, List<String> relatedProducts, String countryId) {
        ConfigCommercial configGeneral = siteConfiguration.getCommercialGeneralConfigurations(locale);
        String rootPath = configGeneral.getEventsPath();
        Map<String, String> map = new HashMap<>();
        map.put("type", "cq:Page");
        map.put("path", rootPath);
        map.put("0_property", "jcr:content/cq:template");
        map.put("0_property.value", Constants.EVENTS_TEMPLATE_PATH);
        map.put("tagid.property", "jcr:content/cq:tags");
        if (tags != null && !tags.isEmpty()) {
            map.put("tagid.and", "true");
            int count = 1;
            for (String tag : tags) {
                map.put("tagid." + count++ + "_value", tag);
            }
        }

        /*
        The dialog properties to set up the availability rules include extra properties that are created by a Javascript
        country.js file. This creates the structure used for the below queries.It creates a new variable excludedIn or
        includedIn, depending on selection, and the value of this is the countries added.

         type=cq:Page
         path=/content/commercial/en/events
         property=jcr:content/cq:template
         property.value=/conf/amadeus/commercial/settings/wcm/templates/event-page
         orderby.sort=asc

        1_group.p.or=true

        1_group.0_group.1_property=jcr:content/availability
        1_group.0_group.1_property.operation=not

        1_group.1_group.1_property=jcr:content/availability
        1_group.1_group.1_property.value=includedIn
        1_group.1_group.2_property.operation=not
        1_group.1_group.2_property=jcr:content/includedIn

        1_group.2_group.1_property.value=includedIn
        1_group.2_group.1_property=jcr:content/availability
        1_group.2_group.2_property.value=US
        1_group.2_group.2_property=jcr:content/includedIn

        1_group.3_group.1_property.value=excludedIn
        1_group.3_group.1_property=jcr:content/availability
        1_group.3_group.2_property=jcr:content/excludedIn
        1_group.3_group.2_property.operation=not

        1_group.4_group.1_property=jcr:content/availability
        1_group.4_group.1_property.value=excludedIn
        1_group.4_group.2_group.p.not=true
        1_group.4_group.2_group.1_property=jcr:content/excludedIn
        1_group.4_group.2_group.1_property.value=US
         */
        map.put("1_group.p.or", "true");
        map.put("1_group.0_group.1_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY.toString());
        map.put("1_group.0_group.1_property.operation", NOT);

        map.put("1_group.1_group.1_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY.toString());
        map.put("1_group.1_group.1_property.value", AvailabilityEnum.AVAILABILITY_INCLUDED_IN.toString());
        map.put("1_group.1_group.2_property.operation", NOT);
        map.put("1_group.1_group.2_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY_INCLUDED_IN.toString());

        if (StringUtils.isNotBlank(countryId)) {
            map.put("1_group.2_group.1_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY.toString());
            map.put("1_group.2_group.1_property.value", AvailabilityEnum.AVAILABILITY_INCLUDED_IN.toString());
            map.put("1_group.2_group.2_property.value", countryId.trim().toUpperCase());
            map.put("1_group.2_group.2_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY_INCLUDED_IN.toString());

            map.put("1_group.3_group.1_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY.toString());
            map.put("1_group.3_group.1_property.value", AvailabilityEnum.AVAILABILITY_EXCLUDED_IN.toString());
            map.put("1_group.3_group.2_property.operation", NOT);
            map.put("1_group.3_group.2_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY_EXCLUDED_IN.toString());

            map.put("1_group.4_group.1_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY.toString());
            map.put("1_group.4_group.1_property.value", AvailabilityEnum.AVAILABILITY_EXCLUDED_IN.toString());
            map.put("1_group.4_group.2_group.p.not", "true");
            map.put("1_group.4_group.2_group.1_property.value", countryId.trim().toUpperCase());
            map.put("1_group.4_group.2_group.1_property", JcrConstants.JCR_CONTENT + Constants.SLASH + AvailabilityEnum.AVAILABILITY_EXCLUDED_IN.toString());
        }

        if (relatedProducts != null && !relatedProducts.isEmpty()) {
            int i = 1;
            map.put("2_group.0_group.p.or=true", "true");

            for (String productPath : relatedProducts) {
                map.put("2_group.0_group." + i + "_property", "jcr:content/productLink");
                map.put("2_group.0_group." + i + "_property.value", productPath);
                i++;
            }
        }
        
        map.put("orderby", "@jcr:content/eventStartDate");
        map.put("orderby.sort", "asc");

        return map;
    }

    private SearchResult executeSearch(ResourceResolver resourceResolver, Map<String, String> map, int start, Integer limit) {
        Query query = queryBuilder.createQuery(PredicateGroup.create(map), resourceResolver.adaptTo(Session.class));
        if (limit != null) {
            query.setHitsPerPage(limit);
        }
        query.setStart(start);
        return query.getResult();
    }

}
