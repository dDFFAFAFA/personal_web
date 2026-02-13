package com.changye.web.model.enums;

public enum ReadingStatus {
    UNREAD("未读"),
    SKIMMED("读了一点"),
    HALF_READ("读了一半"),
    FINISHED("精读完成"),
    NEED_REREAD("需要重读");

    private final String label;

    ReadingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
