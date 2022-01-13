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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1006;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1076;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1077;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1084;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1085;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1090;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1112;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.Error;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.util.Constant;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.attribute.TrackedAttributeValidationService;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityAttributeValidationHook extends AttributeValidationHook
{
    private final DhisConfigurationProvider dhisConfigurationProvider;

    public TrackedEntityAttributeValidationHook( TrackedAttributeValidationService teAttrService,
        DhisConfigurationProvider dhisConfigurationProvider )
    {
        super( teAttrService );
        checkNotNull( dhisConfigurationProvider );
        this.dhisConfigurationProvider = dhisConfigurationProvider;
    }

    @Override
    public void validateTrackedEntity( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackedEntity trackedEntity )
    {
        TrackedEntityType trackedEntityType = context.getTrackedEntityType( trackedEntity.getTrackedEntityType() );
        TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
        OrganisationUnit organisationUnit = context.getBundle().getPreheat()
            .getOrganisationUnit( trackedEntity.getOrgUnit(), context.bundle );

        validateMandatoryAttributes( report, trackedEntity, trackedEntityType );
        validateAttributes( report, context, trackedEntity, tei, organisationUnit, trackedEntityType );
    }

    private void validateMandatoryAttributes( TrackerValidationReport report, TrackedEntity trackedEntity,
        TrackedEntityType trackedEntityType )
    {
        if ( trackedEntityType != null )
        {
            Set<String> trackedEntityAttributes = trackedEntity.getAttributes()
                .stream()
                .map( Attribute::getAttribute )
                .collect( Collectors.toSet() );

            trackedEntityType.getTrackedEntityTypeAttributes()
                .stream()
                .filter( trackedEntityTypeAttribute -> Boolean.TRUE.equals( trackedEntityTypeAttribute.isMandatory() ) )
                .map( TrackedEntityTypeAttribute::getTrackedEntityAttribute )
                .map( BaseIdentifiableObject::getUid )
                .filter( mandatoryAttributeUid -> !trackedEntityAttributes.contains( mandatoryAttributeUid ) )
                .forEach(
                    attribute -> {
                        Error error = Error.builder()
                            .uid( ((TrackerDto) trackedEntity).getUid() )
                            .trackerType( ((TrackerDto) trackedEntity).getTrackerType() )
                            .errorCode( E1090 )
                            .addArg( attribute )
                            .addArg( trackedEntityType.getUid() )
                            .addArg( trackedEntity.getTrackedEntity() )
                            .build();
                        report.addError( error );
                    } );
        }
    }

    protected void validateAttributes( TrackerValidationReport report,
        TrackerImportValidationContext context,
        TrackedEntity trackedEntity, TrackedEntityInstance tei, OrganisationUnit orgUnit,
        TrackedEntityType trackedEntityType )
    {
        checkNotNull( trackedEntity, TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL );
        checkNotNull( trackedEntityType, TrackerImporterAssertErrors.TRACKED_ENTITY_TYPE_CANT_BE_NULL );

        Map<String, TrackedEntityAttributeValue> valueMap = new HashMap<>();
        if ( tei != null )
        {
            valueMap = tei.getTrackedEntityAttributeValues()
                .stream()
                .collect( Collectors.toMap( v -> v.getAttribute().getUid(), v -> v ) );
        }

        for ( Attribute attribute : trackedEntity.getAttributes() )
        {
            TrackedEntityAttribute tea = context.getTrackedEntityAttribute( attribute.getAttribute() );

            if ( tea == null )
            {
                Error error = Error.builder()
                    .uid( trackedEntity.getUid() )
                    .trackerType( trackedEntity.getTrackerType() )
                    .errorCode( E1006 )
                    .addArg( attribute.getAttribute() )
                    .build();
                report.addError( error );
                continue;
            }

            if ( attribute.getValue() == null )
            {
                Optional<TrackedEntityTypeAttribute> optionalTea = Optional.of( trackedEntityType )
                    .map( tet -> tet.getTrackedEntityTypeAttributes().stream() )
                    .flatMap( tetAtts -> tetAtts.filter(
                        teaAtt -> teaAtt.getTrackedEntityAttribute().getUid().equals( attribute.getAttribute() )
                            && teaAtt.isMandatory() != null && teaAtt.isMandatory() )
                        .findFirst() );

                if ( optionalTea.isPresent() )
                {
                    Error error = Error.builder()
                        .uid( ((TrackerDto) trackedEntity).getUid() )
                        .trackerType( ((TrackerDto) trackedEntity).getTrackerType() )
                        .errorCode( E1076 )
                        .addArg( TrackedEntityAttribute.class.getSimpleName() )
                        .addArg( attribute.getAttribute() )
                        .build();
                    report.addError( error );
                }

                continue;
            }

            validateAttributeValue( report, trackedEntity, tea, attribute.getValue() );
            validateAttrValueType( report, context, trackedEntity, attribute, tea );
            validateOptionSet( report, trackedEntity, tea,
                attribute.getValue() );

            validateAttributeUniqueness( report, context, trackedEntity, attribute.getValue(), tea, tei, orgUnit );

            validateFileNotAlreadyAssigned( report, context, trackedEntity, attribute, valueMap );
        }
    }

    public void validateAttributeValue( TrackerValidationReport report, TrackedEntity te, TrackedEntityAttribute tea,
        String value )
    {
        checkNotNull( tea, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );
        checkNotNull( value, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );

        // Validate value (string) don't exceed the max length
        report.addErrorIf( () -> value.length() > Constant.MAX_ATTR_VALUE_LENGTH, () -> Error.builder()
            .uid( ((TrackerDto) te).getUid() )
            .trackerType( ((TrackerDto) te).getTrackerType() )
            .errorCode( E1077 )
            .addArg( value )
            .addArg( Constant.MAX_ATTR_VALUE_LENGTH )
            .build() );

        // Validate if that encryption is configured properly if someone sets
        // value to (confidential)
        boolean isConfidential = tea.isConfidentialBool();
        boolean encryptionStatusOk = dhisConfigurationProvider.getEncryptionStatus().isOk();
        report.addErrorIf( () -> isConfidential && !encryptionStatusOk, () -> Error.builder()
            .uid( ((TrackerDto) te).getUid() )
            .trackerType( ((TrackerDto) te).getTrackerType() )
            .errorCode( E1112 )
            .addArg( value )
            .build() );

        // Uses ValidationUtils to check that the data value corresponds to the
        // data value type set on the attribute
        final String result = dataValueIsValid( value, tea.getValueType() );
        report.addErrorIf( () -> result != null, () -> Error.builder()
            .uid( ((TrackerDto) te).getUid() )
            .trackerType( ((TrackerDto) te).getTrackerType() )
            .errorCode( E1085 )
            .addArg( tea )
            .addArg( result )
            .build() );
    }

    protected void validateFileNotAlreadyAssigned( TrackerValidationReport report,
        TrackerImportValidationContext context, TrackedEntity te,
        Attribute attr, Map<String, TrackedEntityAttributeValue> valueMap )
    {
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );

        boolean attrIsFile = attr.getValueType() != null && attr.getValueType().isFile();
        if ( !attrIsFile )
        {
            return;
        }

        TrackedEntityAttributeValue trackedEntityAttributeValue = valueMap.get( attr.getAttribute() );

        // Todo: how can this be possible? is this acceptable?
        if ( trackedEntityAttributeValue != null &&
            !trackedEntityAttributeValue.getAttribute().getValueType().isFile() )
        {
            return;
        }

        FileResource fileResource = context.getFileResource( attr.getValue() );

        report.addErrorIf( () -> fileResource == null, () -> Error.builder()
            .uid( te.getUid() )
            .trackerType( te.getTrackerType() )
            .errorCode( E1084 )
            .addArg( attr.getValue() )
            .build() );
        report.addErrorIf( () -> fileResource != null && fileResource.isAssigned(), () -> Error.builder()
            .uid( ((TrackerDto) te).getUid() )
            .trackerType( ((TrackerDto) te).getTrackerType() )
            .errorCode( E1009 )
            .addArg( attr.getValue() )
            .build() );
    }
}
