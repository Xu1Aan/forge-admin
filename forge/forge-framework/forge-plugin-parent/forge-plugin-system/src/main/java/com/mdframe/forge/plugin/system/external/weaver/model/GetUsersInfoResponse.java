package com.mdframe.forge.plugin.system.external.weaver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 泛微 getUsersInfo：{ "data": [ ... ] }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetUsersInfoResponse {
    private List<FlatRow> data;
}
