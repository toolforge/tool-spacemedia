package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.net.URL;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record User(String CountryIso3, String DisplayName, String FirstName, String LastName, int UserId,
        String UserProfileAvatar, URL UserProfileUrl)
{

}
