package com.amadeus.commercial.services.impl;


import com.amadeus.commercial.services.RssService;
import com.amadeus.commons.utils.CommercialUtils;
import com.amadeus.commons.utils.Constants;
import com.amadeus.commons.utils.PageUtils;
import com.amadeus.commons.utils.TagsConstants;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.tagging.Tag;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@Component(immediate = true, metatype = false, enabled = true)
@Service(value = RssService.class)
public class RssServiceImpl implements RssService{
    private static final Logger LOGGER = LoggerFactory.getLogger(RssServiceImpl.class);
    private static final String URI_NS_ATOM = "http://www.w3.org/2005/Atom";
    private static final String URI_NS_XML = "http://www.w3.org/2001/sw/";
    private static final String URI_NS_XSL = "http://purl.org/rss/1.0/modules/content/";
    private static final String URI_NS_CHANNEL = "http://www.w3.org/2001/sw/Overview.rss";
    private static final String RESTYPE_TEXT = "amadeus/commons/components/content/modules/text";
    private static final String RESTYPE_IMG = "amadeus/commons/components/content/modules/image";
    private static final String RESTYPE_HEAD = "amadeus/commons/components/content/modules/heading";
    public static final String DESCRIPTION = "description";
    public static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:MM:ss z");

    @Reference
    private Externalizer externalizer;

    @Reference
    private SlingSettingsService slingSettingsService;


    @Override
    public void constructRss(SlingHttpServletResponse response, Page page, ResourceResolver resourceResolver, Locale lang)
            throws IOException {
        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        try {
            XMLStreamWriter stream = outputFactory.createXMLStreamWriter(response.getWriter());

            writeHeaders(stream, page, resourceResolver, lang);
        } catch (XMLStreamException e) {
            LOGGER.error("XML Stream Exception occured, ", e);
            throw new IOException(e);
        }
    }

    private void writeHeaders(XMLStreamWriter stream, Page page, ResourceResolver resourceResolver, Locale lang) {
        try {
            //XML
            stream.setDefaultNamespace(URI_NS_XML);
            stream.writeStartDocument("UTF-8", "1.0");
            stream.writeEndDocument();

            //RSS
            stream.setDefaultNamespace(URI_NS_XSL);
            stream.writeStartElement(URI_NS_XSL, "rss");
            stream.writeAttribute("version", "2.0");
            stream.writeAttribute("xmlns:content", URI_NS_XSL);
            stream.writeAttribute("xmlns:wfw", "http://wellformedweb.org/CommentAPI/");
            stream.writeAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
            stream.writeAttribute("xmlns:atom", URI_NS_ATOM);
            stream.writeAttribute("xmlns:sy", "http://purl.org/rss/1.0/modules/syndication/");
            stream.writeAttribute("xmlns:slash", "http://purl.org/rss/1.0/modules/slash/");

            //Channel
            writeChannel(stream, page, resourceResolver, lang);

            stream.writeEndElement(); //end RSS
        } catch (XMLStreamException e) {
            LOGGER.error("Error writing the RSS Feed header, ", e);
        }
    }


    private void writeChannel(XMLStreamWriter stream, Page page, ResourceResolver resourceResolver, Locale lang) {
        try {
            stream.setDefaultNamespace(URI_NS_CHANNEL);
            stream.writeStartElement(URI_NS_CHANNEL, "channel");

            if (page != null) {
                String pagePath = page.getPath() + ".rss.xml";
                if (!StringUtils.isEmpty(page.getPath())) {
                    pagePath = externalizer.publishLink(resourceResolver, pagePath);
                }

                stream.writeStartElement("title");
                stream.writeCharacters(page.getTitle());
                stream.writeEndElement();

                stream.setDefaultNamespace(URI_NS_ATOM);
                stream.writeEmptyElement(URI_NS_ATOM, "atom:link");
                stream.writeAttribute("href", pagePath);
                stream.writeAttribute("rel", "self");
                stream.writeAttribute("type", "application/rss+xml");

                stream.writeStartElement("link");
                stream.writeCharacters(pagePath);
                stream.writeEndElement();

                stream.writeStartElement(DESCRIPTION);
                stream.writeCharacters(page.getDescription());
                stream.writeEndElement();

                Date now = new Date();
                String date = getDateFormatted(now);
                stream.writeStartElement("lastBuildDate");
                stream.writeCharacters(date);
                stream.writeEndElement();

                String locale = getISOLocale(page);
                stream.writeStartElement("language");
                stream.writeCharacters(locale);
                stream.writeEndElement();

                stream.writeStartElement("sy:updatePeriod");
                stream.writeCharacters("hourly");
                stream.writeEndElement();

                stream.writeStartElement("sy:updateFrequency");
                stream.writeCharacters("1");
                stream.writeEndElement();

                stream.writeStartElement("generator");
                stream.writeCharacters("Amadeus AEM 6.3 RSS Servlet");
                stream.writeEndElement();
            }

            //item
            writeItem(stream, page, resourceResolver, lang);

            stream.writeEndElement(); //end Channel
        } catch (XMLStreamException e) {
            LOGGER.error("Error writing the RSS Feed channel, ", e);
        }
    }



    private void writeItem(XMLStreamWriter stream, Page parentPage, ResourceResolver resourceResolver, Locale lang) {
        Iterator<Page> childrenPages = parentPage.listChildren(new PageFilter(false, true), true);
        List<Page> allPages = getAllPages(childrenPages);

        if (allPages != null) {
            Iterator<Page> children = allPages.iterator();

            while (children.hasNext()) {
                Page page = children.next();
                ValueMap pageProperties = page.getProperties();
                String redirectTarget = pageProperties.get("redirectTarget", "");

                if (StringUtils.isBlank(redirectTarget)) {
                    try {
                        stream.writeStartElement("item");

                        writeCategories(stream, page, lang);

                        String pagePath = page.getPath();
                        if (!StringUtils.isEmpty(page.getPath())) {
                            pagePath = externalizer.publishLink(resourceResolver, pagePath);
                        }

                        String title = page.getPageTitle();
                        if (StringUtils.isBlank(title)) {
                            title = page.getTitle();
                            if (StringUtils.isBlank(title)) {
                                title = page.getName();
                            }
                        }
                        stream.writeStartElement("title");
                        stream.writeCharacters(title);
                        stream.writeEndElement();

                        stream.writeStartElement("link");
                        stream.writeCharacters(pagePath);
                        stream.writeEndElement();

                        String pub = getPublishedDate(pageProperties);
                        stream.writeStartElement("pubDate");
                        String formattedDate = getDateFormattedStr(pub);
                        stream.writeCharacters(formattedDate);
                        stream.writeEndElement();

                        String author = CommercialUtils.getAuthorFromHero(page);
                        if (StringUtils.isNotBlank(author)) {
                            stream.writeStartElement("dc:creator");
                            stream.writeCData(author);
                            stream.writeEndElement();
                        }

                        stream.writeStartElement("guid");
                        stream.writeAttribute("isPermaLink", "true");
                        stream.writeCharacters(pagePath);
                        stream.writeEndElement();

                        if (StringUtils.isNotBlank(page.getDescription())) {
                            stream.writeStartElement(DESCRIPTION);
                            stream.writeCData(page.getDescription());
                            stream.writeEndElement();
                        } else {
                            stream.writeEmptyElement(DESCRIPTION);
                        }

                        writeImage(stream, resourceResolver, pageProperties);
                        writeContent(stream, page, resourceResolver);

                        stream.writeEndElement(); //end item
                    } catch (XMLStreamException e) {
                        LOGGER.error("Error writing RSS feed item, ", e);
                    }
                }
            }
        }
    }

    private List<Page> getAllPages(Iterator<Page> children) {
        List<Page> allPages = new ArrayList<>();

        while (children.hasNext()) {
            Page page = children.next();
            String templatePath = page.getProperties().get(NameConstants.NN_TEMPLATE, String.class);
            if (!StringUtils.equals(templatePath, Constants.REDIRECT_TEMPLATE_PATH)) {
                allPages.add(page);
            }
        }

        if (allPages != null) {
            Collections.sort(allPages, new Comparator<Page>() {
                public int compare(Page p1, Page p2) {
                    ValueMap pp1 = p1.getProperties();
                    ValueMap pp2 = p2.getProperties();

                    String dateStr1 = getPublishedDate(pp1);
                    Date fd1 = getDateFromString(dateStr1);

                    String dateStr2 = getPublishedDate(pp2);
                    Date fd2 = getDateFromString(dateStr2);

                    if (fd1 == null || fd2 == null) {
                        return -1;
                    } else {
                        return fd1.compareTo(fd2);
                    }

                }
            });
        }
        return allPages;
    }

    private void writeContent(XMLStreamWriter stream, Page page, ResourceResolver resourceResolver) {
        StringBuilder pageContent = new StringBuilder();

        try {
            Resource centerParsysRes = page.getContentResource("centerParsys");
            if (centerParsysRes != null) {
                Iterator<Resource> childResources = centerParsysRes.listChildren();
                while (childResources.hasNext()) {
                    Resource res = childResources.next();
                    pageContent = getContentResourceList(res, resourceResolver);
                }
            }

            stream.writeStartElement("content:encoded");
            stream.writeCData(pageContent.toString());
            stream.writeEndElement();
        } catch (XMLStreamException e) {
            LOGGER.error("Error writing the content, ", e);
        }
    }

    private StringBuilder getContentResourceList(Resource res, ResourceResolver resourceResolver) {
        StringBuilder content = new StringBuilder();
        if (res.hasChildren()) {
            Iterator<Resource> resourcesIt = res.listChildren();
            while (resourcesIt.hasNext()) {
                Resource nextRes = resourcesIt.next();
                String resourceType = nextRes.getResourceType();
                ValueMap vm = nextRes.getValueMap();
                extractMarkupFromSelectComponents(resourceResolver, content, resourceType, vm);
                content.append(getContentResourceList(nextRes, resourceResolver));
            }
        }
        return content;
    }

    private void extractMarkupFromSelectComponents(ResourceResolver resourceResolver, StringBuilder content, String resourceType, ValueMap vm) {
        if (RESTYPE_TEXT.equals(resourceType)) {
            Object text = vm.get("text");
            if (text != null) {
                content.append(cleanHTML(text.toString(), resourceResolver));
            }
        } else if (RESTYPE_IMG.equals(resourceType)) {
            Object img = vm.get("fileReference");
            if (img != null) {
                String imagePath = externalizer.publishLink(resourceResolver, img.toString());
                content.append("<img src='" + imagePath + "'>");
            }
        } else if (RESTYPE_HEAD.equals(resourceType)) {
            Object text = vm.get(JcrConstants.JCR_TITLE);
            if (text != null) {
                content.append("<h2>" + text.toString() + "</h2>");
            }
        }
    }


    public String cleanHTML(String unsafe, ResourceResolver resourceResolver){
        String htmlAbsoluteAnchors = makeUrlsAbsolute(unsafe, "a", "href", resourceResolver);
        String htmlAbsoluteImgs = makeUrlsAbsolute(htmlAbsoluteAnchors, "img", "src", resourceResolver);
        Whitelist whitelist = Whitelist.none();
        whitelist.addTags("p","br","ul","li","b","i","strong","h1","h2","h3","h4","h5","h6");
        whitelist.addAttributes("a","href","name");
        whitelist.addAttributes("img","src");
        String safe = Jsoup.clean(htmlAbsoluteImgs, whitelist);
        return StringEscapeUtils.unescapeXml(safe);
    }

    private String makeUrlsAbsolute(String unsafe, String tag, String attr, ResourceResolver resourceResolver) {
        Document doc = Jsoup.parse(unsafe);
        Elements select = doc.select(tag);
        for (Element e : select){
            String url = e.attr(attr);
            if (url.startsWith("/")) {
                String domain = externalizer.publishLink(resourceResolver, url);
                e.attr(attr, domain);
            }
        }
        return doc.toString();
    }

    private void writeImage(XMLStreamWriter stream, ResourceResolver resourceResolver, ValueMap vm) {
        if (vm.get("imgPath") != null) {
            String imagePath = vm.get("imgPath").toString();
            if (StringUtils.isNotBlank(imagePath)) {
                try {
                    stream.writeEmptyElement("enclosure");
                    stream.writeAttribute("url", externalizer.publishLink(resourceResolver, imagePath));
                    String ext = "image/" + FilenameUtils.getExtension(imagePath);
                    stream.writeAttribute("type", ext);

                    Resource imgMetaResource = resourceResolver.getResource(imagePath + "/jcr:content/metadata");
                    String size = getProperty(imgMetaResource, DamConstants.DAM_SIZE);
                    if (StringUtils.isBlank(size)) {
                        stream.writeAttribute("length", "0");
                    } else {
                        stream.writeAttribute("length", size);
                    }
                } catch (XMLStreamException e) {
                    LOGGER.error("Error writing image enclosure, ", e);
                }
            }
        }
    }

    private void writeCategories(XMLStreamWriter stream, Page page, Locale lang) {
        List<String> nonArticleTags = new ArrayList<>();
        for (Tag tag : page.getTags()) {
            try {
                if (tag.getTagID().contains(TagsConstants.ARTICLE_TYPE)) {
                    stream.writeStartElement("category");
                    stream.writeCData(tag.getTitle(lang));
                    stream.writeEndElement();
                } else {
                    nonArticleTags.add(tag.getTitle(lang));
                }
            } catch (XMLStreamException e) {
                LOGGER.error("Error writing categories for tags ", e);
            }
        }

        try {
            if (!nonArticleTags.isEmpty()) {
                writeAdditionalTags(stream, nonArticleTags);
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Error writing categories for additional tags ", e);
        }
    }

    private void writeAdditionalTags(XMLStreamWriter stream, List<String> titles) throws XMLStreamException {
        for (String title : titles) {
            stream.writeStartElement("category");
            stream.writeCData(title);
            stream.writeEndElement();
        }
    }

    private String getPublishedDate(ValueMap vm) {
        String property = (PageUtils.isPublishRunmode(slingSettingsService))?
                "publishDate" : "cq:lastModified";
        return vm.get(property, "");
    }


    private String getDateFormattedStr(String dateStr) {
        try {
            Date date = FORMATTER.parse(dateStr);
            return getDateFormatted(date);
        } catch (ParseException e) {
            LOGGER.error("String date parse problem ", e);
        }
        return null;
    }

    private Date getDateFromString(String dateStr) {
        Date d = new Date();
        try {
            d = FORMATTER.parse(dateStr);
        } catch (ParseException e) {
            LOGGER.error("String date parse problem ", e);
        }
        return d;
    }

    private String getProperty(Resource resource, String propertyName) {
        String returnVal = "";
        if (resource != null) {
            ValueMap properties = resource.getValueMap();
            returnVal = properties.get(propertyName, "");
        }
        return returnVal;
    }

    private String getDateFormatted(Date date) {
        String returnValue = "";
        returnValue = dateFormat.format(date);
        return returnValue;
    }

    /*
    Derived from requirements to always set these against the locale
     */
    private String getISOLocale(Page page) {
        String returnValue = "";
        String loc = page.getLanguage().toString();
        switch (loc) {
            case "en":
                returnValue = loc + Constants.DASH + "US";
                break;
            case "fr":
                returnValue = loc + Constants.DASH + "FR";
                break;
            case "es":
                returnValue = loc + Constants.DASH + "ES";
                break;
            case "de":
                returnValue = loc + Constants.DASH + "DE";
                break;
            default:
                returnValue = "en-US";
                break;
        }
        return returnValue;
    }


}
