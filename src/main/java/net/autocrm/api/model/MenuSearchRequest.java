package net.autocrm.api.model;

import lombok.Data;

@Data
public class MenuSearchRequest {
    private String query;
    private String authSeq;
    private String userId;
    private String dealerId;
}