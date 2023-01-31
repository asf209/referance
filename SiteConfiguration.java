package com.amadeus.commons.services;

import java.util.Locale;

import com.amadeus.careers.pojos.config.ConfigCareers;
import com.amadeus.commercial.pojos.config.ConfigCommercial;
import com.amadeus.commons.pojos.config.ConfigFavicons;
import com.amadeus.commons.pojos.config.ConfigLists;
import com.amadeus.commons.pojos.config.ConfigSocialMedia;
import com.amadeus.corporate.pojos.config.ConfigCorporate;
import com.amadeus.saleskit.pojos.config.ConfigSalesKit;

public interface SiteConfiguration {
    ConfigSocialMedia getSocialMediaConfigurations(Locale locale);
    ConfigLists getCommonsListsConfigurations(Locale locale);
    ConfigFavicons getConfigFaviconsConfigurations(Locale locale);
    ConfigCommercial getCommercialGeneralConfigurations(Locale locale);
    ConfigCorporate getCorporateGeneralConfigurations(Locale locale);
    ConfigCareers getCareersGeneralConfigurations(Locale locale);
    ConfigSalesKit getSalesKitConfigurations(Locale locale, String buPath);
    String getCommonsCountryPath();
}
