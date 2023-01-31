package com.amadeus.commercial.models;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.acs.commons.models.injectors.annotation.AemObject;
import com.amadeus.commercial.pojos.Event;
import com.amadeus.commercial.services.EventService;
import com.amadeus.commons.services.SiteConfiguration;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.common.base.Strings;

@Model(adaptables = SlingHttpServletRequest.class)
public class EventDetailModel {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @AemObject
    private PageManager pageManager;

    @OSGiService
    private SiteConfiguration siteConfiguration;

    @OSGiService
    private EventService eventService;

    @RequestAttribute(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String subNodeName;

    @RequestAttribute(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String eventPath;

    private static final String EVENT_PATH = "eventPath";

    private Event event;
    
    @PostConstruct
    public void init() {
        String path = getPath();
        if (!Strings.isNullOrEmpty(path)) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Page page = pageManager.getPage(path);
            if (page != null) {
                event = new Event(page, page.getLanguage(), resourceResolver, siteConfiguration);
            }
        }
    }

    private String getPath() {
        if (!Strings.isNullOrEmpty(eventPath)) {
            return eventPath;
        } else if (Strings.isNullOrEmpty(subNodeName)) {
            return resource.getValueMap().get(EVENT_PATH, "");
        } else {
            Resource propResource = resource.getChild(subNodeName);
            if (propResource != null) {
                return propResource.getValueMap().get(EVENT_PATH, "");
            }
        }
        return "";
    }
    
    public Event getEvent() {
        return event;
    }

}
