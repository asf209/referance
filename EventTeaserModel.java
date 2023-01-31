package com.amadeus.commercial.models;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.models.injectors.annotation.AemObject;
import com.amadeus.commercial.pojos.Event;
import com.amadeus.commercial.services.EventService;
import com.amadeus.commons.services.SiteConfiguration;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.common.base.Strings;

@Model(adaptables = SlingHttpServletRequest.class)
public class EventTeaserModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventTeaserModel.class);
    public static final String POPULATE_CARDS_MANUAL = "manual";
    private static final int SELECTOR_COUNTRY_ID = 1;
    private static final String SELECTOR_ALL = "all";

    @Self
    private SlingHttpServletRequest request;
    
    @OSGiService
    private SiteConfiguration siteConfiguration;
    
    @AemObject
    private Page currentPage;

    @OSGiService
    private EventService eventService;

    @AemObject
    private PageManager pageManager;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String populateCards;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private List<Resource> cards;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Integer limit;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private List<String> tags;
    
    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private List<String> relatedProducts;

    private List<Event> events;

    private boolean invalidPath;
    
    private Locale language;

    public List<Event> getEvents() {
        return events;
    }

    public boolean isInvalidPath() {
        return invalidPath;
    }

    @PostConstruct
    public void init() {
        invalidPath = false;
        if (!Strings.isNullOrEmpty(populateCards)) {
            language = currentPage.getLanguage();
            if (POPULATE_CARDS_MANUAL.equals(populateCards)) {
                loadManualItems(request.getResourceResolver());
            } else {
                loadDynamicItems();
            }
        }
    }

    private void loadManualItems(ResourceResolver resourceResolver) {
        try {
            events = new LinkedList<>();
            if (cards != null) {
                for (Resource resource : cards) {
                    String path = resource.getValueMap().get("path", "");
                    Page page = pageManager.getPage(path);
                    if (page != null) {
                        events.add(new Event(page, language, resourceResolver, siteConfiguration));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            invalidPath = true;
        }
    }

    private void loadDynamicItems() {
        events = eventService.searchUpcomingEvents(language, 0, (limit == null)? 1 : limit, tags, relatedProducts, getCountryId());
    }
    
    private String getCountryId() {
        String[] selectors = request.getRequestPathInfo().getSelectors();
        if (selectors.length == 2) {
            String sel = selectors[SELECTOR_COUNTRY_ID];
            if (StringUtils.equals(sel, SELECTOR_ALL)) {
                sel = null;
            }
            return sel;
        } else {
            return null;
        }
    }

}
