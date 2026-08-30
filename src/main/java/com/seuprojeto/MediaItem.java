package com.seuprojeto;

public class MediaItem {
    private String title;
    private String logo;
    private String url;
    private String group;
    private String type;

    public MediaItem(String title, String logo, String url, String group, String type) {
        this.title = title;
        this.logo = logo;
        this.url = url;
        this.group = group;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public String getLogo() {
        return logo;
    }

    public String getUrl() {
        return url;
    }

    public String getGroup() {
        return group;
    }

    public String getType() {
        return type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setType(String type) {
        this.type = type;
    }
}
