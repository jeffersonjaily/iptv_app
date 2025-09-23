package com.seuprojeto;

public class Channel {
    private String name;
    private String url;
    private String logoUrl;
    private String group;

    public Channel(String name, String url, String logoUrl, String group) {
        this.name = name;
        this.url = url;
        this.logoUrl = logoUrl;
        this.group = group;
    }

    // Construtor auxiliar caso o logoUrl e group não sejam fornecidos
    public Channel(String name, String url) {
        this(name, url, null, null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return name;
    }
}