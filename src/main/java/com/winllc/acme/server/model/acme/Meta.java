package com.winllc.acme.server.model.acme;


/**
 * "meta": {
 *        "termsOfService": "https://example.com/acme/terms/2017-5-30",
 *        "website": "https://www.example.com/",
 *        "caaIdentities": ["example.com"],
 *        "externalAccountRequired": false
 *      }
 */
public class Meta {
    private String termsOfService;
    private String website;
    private String[] caaIdentities;
    private boolean externalAccountRequired;

    public String getTermsOfService() {
        return termsOfService;
    }

    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String[] getCaaIdentities() {
        return caaIdentities;
    }

    public void setCaaIdentities(String[] caaIdentities) {
        this.caaIdentities = caaIdentities;
    }

    public boolean isExternalAccountRequired() {
        return externalAccountRequired;
    }

    public void setExternalAccountRequired(boolean externalAccountRequired) {
        this.externalAccountRequired = externalAccountRequired;
    }
}
