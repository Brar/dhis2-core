/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller.user;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.UserCredWrapper;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@AllArgsConstructor
@JsonInclude( JsonInclude.Include.ALWAYS )
public class MeDto
{
    @JsonProperty( )
    private String id;

    @JsonProperty( )
    private String username;

    @JsonProperty( )
    private String surname;

    @JsonProperty( )
    private String firstName;

    @JsonProperty( )
    private String employer;

    @JsonProperty( )
    private String languages;

    @JsonProperty( )
    private String gender;

    @JsonProperty( )
    private String jobTitle;

    @JsonProperty( )
    private String created;

    @JsonProperty( )
    private String lastUpdated;

    @JsonProperty( "dataViewOrganisationUnits" )
    private Set<OrganisationUnit> dataViewOrganisationUnits;

    @JsonProperty( "favorites" )
    protected Set<String> favorites;

    @JsonProperty( "sharing" )
    protected Sharing sharing;

    @JsonProperty( "userGroupAccesses" )
    private Set<org.hisp.dhis.user.UserGroupAccess> userGroupAccesses;

    @JsonProperty( "userAccesses" )
    private Set<org.hisp.dhis.user.UserAccess> sharingUserAccesses;

    @JsonProperty( "userGroups" )
    private Set<UserGroup> groups;

    @JsonProperty( )
    private Set<Translation> translations;

    @JsonProperty( )
    private Set<OrganisationUnit> teiSearchOrganisationUnits;

    @JsonProperty( )
    private Set<OrganisationUnit> organisationUnits;

    @JsonProperty( )
    private Boolean externalAccess;

    @JsonProperty( )
    private String displayName;

    @JsonProperty( )
    private Access access;

    @JsonProperty( )
    private String name;

    @JsonProperty( )
    private String email;

    // "lastUpdated": "2018-12-03T13:24:26.356",
    // "id": "xE7jOejl9FI",
    // "created": "2013-04-18T17:15:08.407",
    // "birthday": "1971-04-08T00:00:00.000",
    // "education": "Master of super using",
    // "gender": "gender_male",
    // "displayName": "John Traore",
    // "jobTitle": "Super user",
    // "externalAccess": false,
    // "skype": "john.traore",
    // "twitter": "john.traore",
    // "surname": "Traore",
    // "employer": "DHIS",
    // "facebookMessenger": "john.traore",
    // "email": "dummy@dhis2.org",
    // "introduction": "I am the super user of DHIS 2",
    // "whatsApp": "+123123123123",
    // "languages": "English",
    // "telegram": "john.traore",
    // "firstName": "John",
    // "lastCheckedInterpretations": "2016-10-13T11:51:34.317",
    // "nationality": "Sierra Leone",
    // "name": "John Traore",
    // "interests": "Football, swimming, singing, dancing",
    // "favorite": false,

    @JsonProperty( "userCredentials" )
    private UserCredWrapper user;

    @JsonProperty( "settings" )
    private Map<String, Serializable> settings;

    @JsonProperty( "programs" )
    private List<String> programs;

    // authorities
    @JsonProperty( "authorities" )
    private List<String> authorities;

    @JsonProperty( "dataSets" )
    private List<String> dataSets;
}
