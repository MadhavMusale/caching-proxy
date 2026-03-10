package com.proxy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyRequest {
    private String method;
    private String path;
    private String queryString;
    private String body;
    private String origin;
}
