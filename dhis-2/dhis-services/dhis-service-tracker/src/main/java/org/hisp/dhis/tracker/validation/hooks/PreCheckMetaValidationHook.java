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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1005;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1010;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1011;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1013;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1068;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1069;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1070;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4006;
import static org.hisp.dhis.tracker.validation.hooks.ValidationUtils.trackedEntityInstanceExist;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.Error;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckMetaValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateTrackedEntity( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackedEntity tei )
    {
        OrganisationUnit organisationUnit = context.getBundle().getPreheat().getOrganisationUnit( tei.getOrgUnit(),
            context.bundle );
        if ( organisationUnit == null )
        {
            Error error = Error.builder()
                .uid( tei.getUid() )
                .trackerType( tei.getTrackerType() )
                .errorCode( TrackerErrorCode.E1049 )
                .addArg( tei.getOrgUnit() )
                .build();
            report.addError( error );
        }

        TrackedEntityType entityType = context.getTrackedEntityType( tei.getTrackedEntityType() );
        if ( entityType == null )
        {
            Error error = Error.builder()
                .uid( tei.getUid() )
                .trackerType( tei.getTrackerType() )
                .errorCode( E1005 )
                .addArg( tei.getTrackedEntityType() )
                .build();
            report.addError( error );
        }
    }

    @Override
    public void validateEnrollment( TrackerValidationReport report, TrackerImportValidationContext context,
        Enrollment enrollment )
    {
        OrganisationUnit organisationUnit = context.getBundle().getPreheat()
            .getOrganisationUnit( enrollment.getOrgUnit(), context.bundle );
        report.addErrorIf( () -> organisationUnit == null, () -> Error.builder()
            .uid( ((TrackerDto) enrollment).getUid() )
            .trackerType( ((TrackerDto) enrollment).getTrackerType() )
            .errorCode( E1070 )
            .addArg( enrollment.getOrgUnit() )
            .build() );

        Program program = context.getProgram( enrollment.getProgram() );
        report.addErrorIf( () -> program == null, () -> Error.builder()
            .uid( ((TrackerDto) enrollment).getUid() )
            .trackerType( ((TrackerDto) enrollment).getTrackerType() )
            .errorCode( E1069 )
            .addArg( enrollment.getProgram() )
            .build() );

        report.addErrorIf( () -> !trackedEntityInstanceExist( context, enrollment.getTrackedEntity() ),
            () -> Error.builder()
                .uid( ((TrackerDto) enrollment).getUid() )
                .trackerType( ((TrackerDto) enrollment).getTrackerType() )
                .errorCode( E1068 )
                .addArg( enrollment.getTrackedEntity() )
                .build() );
    }

    @Override
    public void validateEvent( TrackerValidationReport report, TrackerImportValidationContext context, Event event )
    {
        OrganisationUnit organisationUnit = context.getBundle().getPreheat().getOrganisationUnit( event.getOrgUnit(),
            context.bundle );
        report.addErrorIf( () -> organisationUnit == null, () -> Error.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1011 )
            .addArg( event.getOrgUnit() )
            .build() );

        Program program = context.getProgram( event.getProgram() );
        report.addErrorIf( () -> program == null, () -> Error.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1010 )
            .addArg( event.getProgram() )
            .build() );

        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        report.addErrorIf( () -> programStage == null, () -> Error.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1013 )
            .addArg( event.getProgramStage() )
            .build() );
    }

    @Override
    public void validateRelationship( TrackerValidationReport report, TrackerImportValidationContext context,
        Relationship relationship )
    {
        RelationshipType relationshipType = context.getRelationShipType( relationship.getRelationshipType() );

        report.addErrorIf( () -> relationshipType == null, () -> Error.builder()
            .uid( ((TrackerDto) relationship).getUid() )
            .trackerType( ((TrackerDto) relationship).getTrackerType() )
            .errorCode( E4006 )
            .addArg( relationship.getRelationshipType() )
            .build() );
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
