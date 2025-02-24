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
package org.hisp.dhis.tracker.converter;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

/**
 * @author Enrico Colasante
 */
@Service
public class RelationshipTrackerConverterService
    implements TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship>
{

    @Override
    public Relationship to( org.hisp.dhis.relationship.Relationship relationship )
    {
        List<Relationship> relationships = to( Collections.singletonList( relationship ) );

        if ( relationships.isEmpty() )
        {
            return null;
        }

        return relationships.get( 0 );
    }

    @Override
    public List<Relationship> to( List<org.hisp.dhis.relationship.Relationship> relationships )
    {
        return relationships.stream().map( fromRelationship -> {

            Relationship toRelationship = new Relationship();
            toRelationship.setRelationship( fromRelationship.getUid() );
            toRelationship.setBidirectional( fromRelationship.getRelationshipType().isBidirectional() );
            toRelationship.setCreatedAt( DateUtils.instantFromDate( fromRelationship.getCreated() ) );
            toRelationship.setFrom( convertRelationshipType( fromRelationship.getFrom() ) );
            toRelationship.setTo( convertRelationshipType( fromRelationship.getTo() ) );
            toRelationship.setUpdatedAt( DateUtils.instantFromDate( fromRelationship.getLastUpdated() ) );
            toRelationship.setRelationshipType( fromRelationship.getRelationshipType().getUid() );

            return toRelationship;
        } ).collect( Collectors.toList() );
    }

    private RelationshipItem convertRelationshipType( org.hisp.dhis.relationship.RelationshipItem from )
    {
        RelationshipItem relationshipItem = new RelationshipItem();
        RelationshipItem.Enrollment enrollment = RelationshipItem.Enrollment.builder()
            .enrollment( from.getProgramInstance() != null ? from.getProgramInstance().getUid() : null )
            .build();
        relationshipItem.setEnrollment( enrollment );
        RelationshipItem.Event event = RelationshipItem.Event.builder()
            .event( from.getProgramStageInstance() != null ? from.getProgramStageInstance().getUid() : null )
            .build();
        relationshipItem.setEvent( event );
        RelationshipItem.TrackedEntity trackedEntity = RelationshipItem.TrackedEntity.builder()
            .trackedEntity( from.getTrackedEntityInstance() != null ? from.getTrackedEntityInstance().getUid() : null )
            .build();
        relationshipItem.setTrackedEntity( trackedEntity );
        return relationshipItem;
    }

    @Override
    public org.hisp.dhis.relationship.Relationship from( TrackerPreheat preheat, Relationship fromRelationship )
    {
        org.hisp.dhis.relationship.Relationship toRelationship = preheat
            .getRelationship( TrackerIdScheme.UID, fromRelationship );
        return from( preheat, fromRelationship, toRelationship );
    }

    @Override
    public List<org.hisp.dhis.relationship.Relationship> from( TrackerPreheat preheat,
        List<Relationship> fromRelationships )
    {
        return fromRelationships
            .stream()
            .map( r -> from( preheat, r ) )
            .collect( Collectors.toList() );
    }

    private org.hisp.dhis.relationship.Relationship from( TrackerPreheat preheat, Relationship fromRelationship,
        org.hisp.dhis.relationship.Relationship toRelationship )
    {
        org.hisp.dhis.relationship.RelationshipType relationshipType = preheat
            .get( RelationshipType.class, fromRelationship.getRelationshipType() );
        org.hisp.dhis.relationship.RelationshipItem fromItem = new org.hisp.dhis.relationship.RelationshipItem();
        org.hisp.dhis.relationship.RelationshipItem toItem = new org.hisp.dhis.relationship.RelationshipItem();

        if ( toRelationship == null )
        {
            Date now = new Date();

            toRelationship = new org.hisp.dhis.relationship.Relationship();
            toRelationship.setUid( fromRelationship.getRelationship() );
            toRelationship.setCreated( now );
            toRelationship.setLastUpdated( now );
        }

        toRelationship.setRelationshipType( relationshipType );

        if ( fromRelationship.getRelationship() != null )
        {
            toRelationship.setUid( fromRelationship.getRelationship() );
        }

        // FROM
        fromItem.setRelationship( toRelationship );

        if ( relationshipType.getFromConstraint().getRelationshipEntity().equals( TRACKED_ENTITY_INSTANCE ) )
        {
            fromItem.setTrackedEntityInstance( preheat.getTrackedEntity( TrackerIdScheme.UID,
                fromRelationship.getFrom().getTrackedEntity().getTrackedEntity() ) );
        }
        else if ( relationshipType.getFromConstraint().getRelationshipEntity().equals( PROGRAM_INSTANCE ) )
        {
            fromItem.setProgramInstance(
                preheat.getEnrollment( TrackerIdScheme.UID,
                    fromRelationship.getFrom().getEnrollment().getEnrollment() ) );
        }
        else if ( relationshipType.getFromConstraint().getRelationshipEntity().equals( PROGRAM_STAGE_INSTANCE ) )
        {
            fromItem.setProgramStageInstance(
                preheat.getEvent( TrackerIdScheme.UID, fromRelationship.getFrom().getEvent().getEvent() ) );
        }

        // TO
        toItem.setRelationship( toRelationship );

        if ( relationshipType.getToConstraint().getRelationshipEntity().equals( TRACKED_ENTITY_INSTANCE ) )
        {
            toItem.setTrackedEntityInstance( preheat.getTrackedEntity( TrackerIdScheme.UID,
                fromRelationship.getTo().getTrackedEntity().getTrackedEntity() ) );
        }
        else if ( relationshipType.getToConstraint().getRelationshipEntity().equals( PROGRAM_INSTANCE ) )
        {
            toItem.setProgramInstance(
                preheat.getEnrollment( TrackerIdScheme.UID,
                    fromRelationship.getTo().getEnrollment().getEnrollment() ) );
        }
        else if ( relationshipType.getToConstraint().getRelationshipEntity().equals( PROGRAM_STAGE_INSTANCE ) )
        {
            toItem.setProgramStageInstance(
                preheat.getEvent( TrackerIdScheme.UID, fromRelationship.getTo().getEvent().getEvent() ) );
        }

        toRelationship.setFrom( fromItem );
        toRelationship.setTo( toItem );
        toRelationship.setKey( RelationshipUtils.generateRelationshipKey( toRelationship ) );
        toRelationship.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( toRelationship ) );

        return toRelationship;
    }
}
