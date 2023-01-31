package com.amadeus.commons.pojos.config;

public class ConfigSocialMedia {
    private boolean enableTwitter;
    private boolean enableFacebook;
    private boolean enableEmail;
    private boolean enableLinkedIn;
    private boolean enableShareaholic;
    private String shareaholicId;
    private String shareaholidMobileId;
    private String shareaholicDesktopId;

    public boolean isEnableTwitter() {
        return enableTwitter;
    }

    public void setEnableTwitter(boolean enableTwitter) {
        this.enableTwitter = enableTwitter;
    }

    public boolean isEnableFacebook() {
        return enableFacebook;
    }

    public void setEnableFacebook(boolean enableFacebook) {
        this.enableFacebook = enableFacebook;
    }

    public boolean isEnableEmail() {
        return enableEmail;
    }

    public void setEnableEmail(boolean enableEmail) {
        this.enableEmail = enableEmail;
    }

    public boolean isEnableLinkedIn() {
        return enableLinkedIn;
    }

    public void setEnableLinkedIn(boolean enableLinkedIn) {
        this.enableLinkedIn = enableLinkedIn;
    }

    public boolean isEnableShareaholic() {
        return enableShareaholic;
    }

    public void setEnableShareaholic(boolean enableShareaholic) {
        this.enableShareaholic = enableShareaholic;
    }

    public String getShareaholicId() {
        return shareaholicId;
    }

    public void setShareaholicId(String shareaholicId) {
        this.shareaholicId = shareaholicId;
    }

    public String getShareaholidMobileId() {
        return shareaholidMobileId;
    }

    public void setShareaholidMobileId(String shareaholidMobileId) {
        this.shareaholidMobileId = shareaholidMobileId;
    }

    public String getShareaholicDesktopId() {
        return shareaholicDesktopId;
    }

    public void setShareaholicDesktopId(String shareaholicDesktopId) {
        this.shareaholicDesktopId = shareaholicDesktopId;
    }
}
