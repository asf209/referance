package com.amadeus.commercial.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

import com.adobe.acs.commons.models.injectors.annotation.AemObject;
import com.day.cq.wcm.api.Page;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Model(adaptables = SlingHttpServletRequest.class)
public class SocialMediaLinksModel {

    @AemObject
    private Page currentPage;
    
    private List<Map<String, String>> socialMediaItems;
    
    @PostConstruct
    public void init() {
        socialMediaItems = new ArrayList<>();
        String[] socialMediaLinks = currentPage.getProperties().get("socialMediaLinks", String[].class);
        if(socialMediaLinks != null) {
            getSocialMediaLinks(socialMediaLinks);
        }
    }

    /**
     * Load the object exposed
     * with the information about 
     * the socialMedia links.
     * @param socialMediaLinks The socialMediaLinks information 
     * contained in the page property.
     */
    private void getSocialMediaLinks(String[] socialMediaLinks) {
        for (int i = 0; i < socialMediaLinks.length; i++) {
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(socialMediaLinks[i]).getAsJsonObject();
            Map<String, String> socialMediaItem = new HashMap<>();
            socialMediaItem.put("icon", json.get("icon").getAsString());
            socialMediaItem.put("path", json.get("path").getAsString());
            socialMediaItems.add(socialMediaItem);
        }
    }

    public List<Map<String, String>> getSocialMediaItems() {
        return socialMediaItems;
    }

}
