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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.MergeParams;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@AllArgsConstructor
@Slf4j
public class UserObjectBundleHook extends AbstractObjectBundleHook<User>
{
    private final UserService userService;

    private final FileResourceService fileResourceService;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    @Override
    public void validate( User user, ObjectBundle bundle,
        Consumer<ErrorReport> addReports )
    {
        if ( bundle.getImportMode().isCreate() && !ValidationUtils.usernameIsValid( user.getUsername() ) )
        {
            addReports.accept(
                new ErrorReport( User.class, ErrorCode.E4049, "username", user.getUsername() )
                    .setErrorProperty( "username" ) );
        }

        if ( user.getWhatsApp() != null && !ValidationUtils.validateWhatsapp( user.getWhatsApp() ) )
        {
            addReports.accept(
                new ErrorReport( User.class, ErrorCode.E4027, user.getWhatsApp(), "whatsApp" )
                    .setErrorProperty( "whatsApp" ) );
        }
    }

    @Override
    public void preCreate( User user, ObjectBundle bundle )
    {
        if ( user == null )
            return;

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            user.getCogsDimensionConstraints().addAll(
                currentUser.getCogsDimensionConstraints() );

            user.getCatDimensionConstraints().addAll(
                currentUser.getCatDimensionConstraints() );
        }

        // bundle.putExtras( user, "uc", user );
    }

    @Override
    public void postCreate( User user, ObjectBundle bundle )
    {
        // if ( !bundle.hasExtras( user, "uc" ) )
        // return;

        if ( !StringUtils.isEmpty( user.getPassword() ) )
        {
            userService.encodeAndSetPassword( user, user.getPassword() );
        }

        if ( user.getAvatar() != null )
        {
            FileResource fileResource = fileResourceService.getFileResource( user.getAvatar().getUid() );
            fileResource.setAssigned( true );
            fileResourceService.updateFileResource( fileResource );
        }

        preheatService.connectReferences( user, bundle.getPreheat(), bundle.getPreheatIdentifier() );
        // sessionFactory.getCurrentSession().save( user );
        sessionFactory.getCurrentSession().update( user );
        // bundle.removeExtras( user, "uc" );
    }

    @Override
    public void preUpdate( User user, User persisted, ObjectBundle bundle )
    {
        if ( user == null )
            return;

        bundle.putExtras( user, "uc", user );

        if ( persisted.getAvatar() != null
            && (user.getAvatar() == null || !persisted.getAvatar().getUid().equals( user.getAvatar().getUid() )) )
        {
            FileResource fileResource = fileResourceService.getFileResource( persisted.getAvatar().getUid() );
            fileResourceService.updateFileResource( fileResource );

            if ( user.getAvatar() != null )
            {
                fileResource = fileResourceService.getFileResource( user.getAvatar().getUid() );
                fileResource.setAssigned( true );
                fileResourceService.updateFileResource( fileResource );
            }
        }
    }

    @Override
    public void postUpdate( User persistedUser, ObjectBundle bundle )
    {
        if ( !bundle.hasExtras( persistedUser, "uc" ) )
            return;

        // final UserCredentials userCredentials = (UserCredentials)
        // bundle.getExtras( user, "uc" );
        final User preUpdateUser = (User) bundle.getExtras( persistedUser, "uc" );

        final User persistedUserCredentials = bundle.getPreheat().get( bundle.getPreheatIdentifier(),
            User.class, persistedUser );

        boolean hasUpdated = false;
        if ( !StringUtils.isEmpty( preUpdateUser.getPassword() ) )
        {
            userService.encodeAndSetPassword( persistedUserCredentials, preUpdateUser.getPassword() );
            // sessionFactory.getCurrentSession().update( persistedUser );
            hasUpdated = true;
        }

        if ( preUpdateUser != persistedUserCredentials )
        {
            mergeService.merge(
                new MergeParams<>( persistedUser, persistedUserCredentials ).setMergeMode( bundle.getMergeMode() ) );
            preheatService.connectReferences( persistedUserCredentials, bundle.getPreheat(),
                bundle.getPreheatIdentifier() );
            hasUpdated = true;
        }

        if ( hasUpdated )
        {
            sessionFactory.getCurrentSession().update( persistedUser );
        }
        bundle.removeExtras( persistedUser, "uc" );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void postCommit( ObjectBundle bundle )
    {
        Iterable<User> objects = bundle.getObjects( User.class );
        Map<String, Map<String, Object>> userReferences = bundle.getObjectReferences( User.class );

        if ( userReferences == null || userReferences.isEmpty() )
        {
            return;
        }

        for ( User identifiableObject : objects )
        {
            User user = identifiableObject;

            user = bundle.getPreheat().get( bundle.getPreheatIdentifier(), user );
            Map<String, Object> userReferenceMap = userReferences.get( identifiableObject.getUid() );

            if ( userReferenceMap == null || userReferenceMap.isEmpty() )
            {
                continue;
            }

            if ( user == null )
            {
                continue;
            }

            // Map<String, Object> userReferencesMap = userReferences.get(
            // user.getUid() );
            // if ( userReferencesMap == null || userReferencesMap.isEmpty() )
            // {
            // continue;
            // }

            Set<UserAuthorityGroup> userAuthorityGroups = (Set<UserAuthorityGroup>) userReferenceMap.get( "userRoles" );
            if ( userAuthorityGroups != null )
            {
                user.setUserAuthorityGroups( userAuthorityGroups );
            }
            else
            {
                user.setUserAuthorityGroups( new HashSet<>() );
            }

            handleNoAccessRoles( user, bundle );

            user.setOrganisationUnits( (Set<OrganisationUnit>) userReferenceMap.get( "organisationUnits" ) );
            user.setDataViewOrganisationUnits(
                (Set<OrganisationUnit>) userReferenceMap.get( "dataViewOrganisationUnits" ) );
            user
                .setCreatedBy( (User) userReferenceMap.get( BaseIdentifiableObject_.CREATED_BY ) );

            if ( user.getCreatedBy() == null )
            {
                user.setCreatedBy( bundle.getUser() );
            }

            user.setLastUpdatedBy( bundle.getUser() );

            preheatService.connectReferences( user, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            // preheatService.connectReferences( user, bundle.getPreheat(),
            // bundle.getPreheatIdentifier() );

            sessionFactory.getCurrentSession().update( user );
            log.error( "Updated user: " + user.getUid() );
        }
    }

    /**
     * If currentUser doesn't have read access to a UserRole and it is included
     * in the payload, then that UserRole should not be removed from updating
     * User.
     *
     * @param user the updating User.
     * @param bundle the ObjectBundle.
     */
    private void handleNoAccessRoles( User user, ObjectBundle bundle )
    {
        Set<String> preHeatedRoles = bundle.getPreheat().get( PreheatIdentifier.UID, user )
            .getUserAuthorityGroups().stream().map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toSet() );

        user.getUserAuthorityGroups().stream()
            .filter( role -> !preHeatedRoles.contains( role.getUid() ) )
            .forEach( role -> {
                UserAuthorityGroup persistedRole = bundle.getPreheat().get( PreheatIdentifier.UID, role );

                if ( persistedRole == null )
                {
                    persistedRole = manager.getNoAcl( UserAuthorityGroup.class, role.getUid() );
                }

                if ( !aclService.canRead( bundle.getUser(), persistedRole ) )
                {
                    bundle.getPreheat().get( PreheatIdentifier.UID, user ).getUserAuthorityGroups()
                        .add( persistedRole );
                    bundle.getPreheat().put( PreheatIdentifier.UID, persistedRole );
                }
            } );
    }
}
