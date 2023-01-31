package com.amadeus.commons.models;

import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.acs.commons.models.injectors.annotation.AemObject;
import com.amadeus.commons.pojos.config.ConfigSocialMedia;
import com.amadeus.commons.services.SiteConfiguration;
import com.amadeus.commons.utils.SocialShareUtils;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class)
public class SocialShareModel {

    @AemObject
    private Page currentPage;

    @OSGiService
    private Externalizer externalizer;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private SiteConfiguration siteConfiguration;

    private boolean facebookEnabled;

    private boolean twitterEnabled;

    private boolean linkedinEnabled;

    private boolean emailEnabled;

    private String facebookURL;

    private String twitterURL;

    private String linkedinURL;

    private String currentPageCanonical;

    @PostConstruct
    public void init() {
        currentPageCanonical = externalizer.publishLink(request.getResourceResolver(), currentPage.getPath());
        Locale locale = currentPage.getLanguage();
        ConfigSocialMedia configSocialMedia = siteConfiguration.getSocialMediaConfigurations(locale);

        facebookEnabled = (configSocialMedia.isEnableFacebook());
        if (facebookEnabled) {
            facebookURL = SocialShareUtils.getFacebookShareURL(getCurrentPageCanonical());
        }
        twitterEnabled = configSocialMedia.isEnableTwitter();
        if (twitterEnabled) {
            twitterURL = SocialShareUtils.getTwitterShareURL(getCurrentPageCanonical());
        }
        linkedinEnabled = configSocialMedia.isEnableLinkedIn();
        if (linkedinEnabled) {
            linkedinURL = SocialShareUtils.getLinkedinShareURL(getCurrentPageCanonical());
        }
        emailEnabled = configSocialMedia.isEnableEmail();
    }

    public String getFacebookURL() {
        return facebookURL;
    }

    public String getTwitterURL() {
        return twitterURL;
    }

    public String getLinkedinURL() {
        return linkedinURL;
    }

    public String getCurrentPageCanonical() {
        return currentPageCanonical;
    }

    public boolean getFacebookEnabled() {
        return facebookEnabled;
    }

    public boolean getTwitterEnabled() {
        return twitterEnabled;
    }

    public boolean getLinkedinEnabled() {
        return linkedinEnabled;
    }

    public boolean getEmailEnabled() {
        return emailEnabled;
    }
}
