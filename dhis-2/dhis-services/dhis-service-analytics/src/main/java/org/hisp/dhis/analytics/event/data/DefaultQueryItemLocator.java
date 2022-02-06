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
package org.hisp.dhis.analytics.event.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObject.ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObject.PROGRAMSTAGE_SEP;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class DefaultQueryItemLocator
    implements QueryItemLocator
{
    private final ProgramStageService programStageService;

    private final DataElementService dataElementService;

    private final TrackedEntityAttributeService attributeService;

    private final ProgramIndicatorService programIndicatorService;

    private final LegendSetService legendSetService;

    private final RelationshipTypeService relationshipTypeService;

    private static final String PS_INDEX_REGEX = "\\[-?\\d+\\]";

    private static final String PS_ASTERISK_REGEX = "\\[\\*\\]";

    private static final String PS_INDEX_COUNT_REGEX = "\\[-?\\d+,\\s*\\d+\\]";

    private static final String PS_INDEX_COUNT_START_DATE_END_DATE_REGEX = "\\[-?\\d+,\\s*\\d+,\\s*\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01]),\\s*\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])\\]";

    private static final String PS_START_DATE_END_DATE_REGEX = "\\[\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01]),\\s*\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])\\]";

    private static final Pattern[] PS_PARAMS_PATTERN_LIST = {
        Pattern.compile( PS_INDEX_REGEX ),
        Pattern.compile( PS_ASTERISK_REGEX ),
        Pattern.compile( PS_INDEX_COUNT_REGEX ),
        Pattern.compile( PS_INDEX_COUNT_START_DATE_END_DATE_REGEX ),
        Pattern.compile( PS_START_DATE_END_DATE_REGEX ) };

    public DefaultQueryItemLocator( ProgramStageService programStageService, DataElementService dataElementService,
        TrackedEntityAttributeService attributeService, ProgramIndicatorService programIndicatorService,
        LegendSetService legendSetService, RelationshipTypeService relationshipTypeService )
    {
        this.programStageService = programStageService;
        this.dataElementService = dataElementService;
        this.attributeService = attributeService;
        this.programIndicatorService = programIndicatorService;
        this.legendSetService = legendSetService;
        this.relationshipTypeService = relationshipTypeService;
    }

    @Override
    public QueryItem getQueryItemFromDimension( String dimension, Program program, EventOutputType type )
    {
        checkNotNull( program, "Program can not be null" );

        LegendSet legendSet = getLegendSet( dimension );

        return getDataElement( dimension, program, legendSet, type )
            .orElseGet( () -> getTrackedEntityAttribute( dimension, program, legendSet )
                .orElseGet( () -> getProgramIndicator( dimension, program, legendSet )
                    .orElseThrow(
                        () -> new IllegalQueryException( new ErrorMessage( ErrorCode.E7224, dimension ) ) ) ) );
    }

    private LegendSet getLegendSet( String dimension )
    {
        dimension = removeOffset( dimension );

        String[] legendSplit = dimension.split( ITEM_SEP );

        return legendSplit.length > 1 && legendSplit[1] != null ? legendSetService.getLegendSet( legendSplit[1] )
            : null;
    }

    private String getElement( String dimension, int pos )
    {

        String dim = StringUtils.substringBefore( removeOffset( dimension ), ITEM_SEP );

        String[] dimSplit = dim.split( "\\" + PROGRAMSTAGE_SEP );

        return dimSplit.length == 1 ? dimSplit[0] : dimSplit[pos];

    }

    private String getFirstElement( String dimension )
    {
        return getElement( dimension, 0 );
    }

    private String getSecondElement( String dimension )
    {
        return getElement( dimension, 1 );
    }

    private Optional<QueryItem> getDataElement( String dimension, Program program, LegendSet legendSet,
        EventOutputType type )
    {
        QueryItem qi = null;

        ProgramStage programStage = getProgramStageOrFail( dimension );

        DataElement de = dataElementService.getDataElement( getSecondElement( dimension ) );

        if ( de != null && program.containsDataElement( de ) )
        {
            ValueType valueType = legendSet != null ? ValueType.TEXT : de.getValueType();

            qi = new QueryItem( de, program, legendSet, valueType, de.getAggregationType(), de.getOptionSet() );

            if ( programStage != null )
            {
                qi.setProgramStage( programStage );

                qi.setRepeatableStageParams( getRepeatableStageParams( dimension ) );
            }
            else if ( type != null && type.equals( EventOutputType.ENROLLMENT ) )
            {
                throwIllegalQueryEx( ErrorCode.E7225, dimension );
            }
        }

        return Optional.ofNullable( qi );
    }

    private Optional<QueryItem> getTrackedEntityAttribute( String dimension, Program program,
        LegendSet legendSet )
    {
        QueryItem qi = null;

        TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute( getSecondElement( dimension ) );

        if ( at != null && program.containsAttribute( at ) )
        {
            ValueType valueType = legendSet != null ? ValueType.TEXT : at.getValueType();

            qi = new QueryItem( at, program, legendSet, valueType, at.getAggregationType(), at.getOptionSet() );
        }

        return Optional.ofNullable( qi );
    }

    private Optional<QueryItem> getProgramIndicator( String dimension, Program program, LegendSet legendSet )
    {
        QueryItem qi = null;

        RelationshipType relationshipType = getRelationshipTypeOrFail( dimension );

        ProgramIndicator pi = programIndicatorService.getProgramIndicatorByUid( getSecondElement( dimension ) );

        // Only allow a program indicator from a different program to be added
        // when a relationship type is present

        if ( pi != null )
        {
            if ( relationshipType != null )
            {
                qi = new QueryItem( pi, program, legendSet, ValueType.NUMBER, pi.getAggregationType(), null,
                    relationshipType );
            }
            else
            {
                if ( program.getProgramIndicators().contains( pi ) )
                {
                    qi = new QueryItem( pi, program, legendSet, ValueType.NUMBER, pi.getAggregationType(), null );
                }
            }
        }

        return Optional.ofNullable( qi );
    }

    private ProgramStage getProgramStageOrFail( String dimension )
    {
        BaseIdentifiableObject baseIdentifiableObject = getIdObjectOrFail( dimension );

        return (baseIdentifiableObject instanceof ProgramStage
            ? (ProgramStage) baseIdentifiableObject
            : null);
    }

    private static String removeOffset( String dimension )
    {
        Optional<Pattern> pattern = Arrays.stream( PS_PARAMS_PATTERN_LIST )
            .filter( p -> p.matcher( dimension ).find() )
            .findFirst();

        if ( pattern.isEmpty() )
        {
            return dimension;
        }

        final Matcher matcher = pattern.get().matcher( dimension );

        if ( matcher.find() )
        {
            return dimension.replace( matcher.group( 0 ), "" );
        }

        return dimension;
    }

    private static RepeatableStageParams getRepeatableStageParams( String dimension )
    {
        Pattern pattern = Arrays.stream( PS_PARAMS_PATTERN_LIST )
            .filter( p -> p.matcher( dimension ).find() )
            .findFirst()
            .orElse( Pattern.compile( PS_INDEX_REGEX ) );

        final Matcher matcher = pattern.matcher( dimension );

        final RepeatableStageParams repeatableStageParams = new RepeatableStageParams();

        switch ( pattern.toString() )
        {
        case PS_ASTERISK_REGEX:

            repeatableStageParams.setStartIndex( 0 );

            repeatableStageParams.setCount( Integer.MAX_VALUE );

            return repeatableStageParams;

        case PS_INDEX_COUNT_REGEX:
        {
            String[] tokens = getMatchedRepeatableStageParamTokens( matcher );

            if ( tokens.length > 1 )
            {
                repeatableStageParams.setCount( Integer.parseInt( tokens[1].trim() ) );
            }

            repeatableStageParams.setStartIndex( Integer.parseInt( tokens[0] ) );
        }

            return repeatableStageParams;

        case PS_INDEX_REGEX:
        {
            if ( !matcher.find() )
            {
                return null;
            }

            String index = matcher.group( 0 )
                .replace( "[", "" )
                .replace( "]", "" );

            repeatableStageParams.setStartIndex( Integer.parseInt( index ) );

            repeatableStageParams.setCount( 1 );
        }

            return repeatableStageParams;

        case PS_INDEX_COUNT_START_DATE_END_DATE_REGEX:
        {
            String[] tokens = getMatchedRepeatableStageParamTokens( matcher );

            if ( tokens.length > 3 )
            {
                repeatableStageParams.setCount( Integer.parseInt( tokens[1].trim() ) );

                repeatableStageParams.setStartDate( DateUtils.parseDate( tokens[2].trim() ) );

                repeatableStageParams.setEndDate( DateUtils.parseDate( tokens[3].trim() ) );
            }

            repeatableStageParams.setStartIndex( Integer.parseInt( tokens[0] ) );
        }

            return repeatableStageParams;

        case PS_START_DATE_END_DATE_REGEX:
        {
            String[] tokens = getMatchedRepeatableStageParamTokens( matcher );

            if ( tokens.length > 1 )
            {
                repeatableStageParams.setStartDate( DateUtils.parseDate( tokens[0].trim() ) );

                repeatableStageParams.setEndDate( DateUtils.parseDate( tokens[1].trim() ) );
            }

            repeatableStageParams.setCount( Integer.MAX_VALUE );

            repeatableStageParams.setStartIndex( 0 );
        }

            return repeatableStageParams;

        default:

            return null;
        }
    }

    private static String[] getMatchedRepeatableStageParamTokens( Matcher matcher )
    {
        String params = "0";

        if ( matcher.find() )
        {
            params = matcher.group( 0 )
                .replace( "[", "" )
                .replace( "]", "" );
        }

        return params.split( "," );
    }

    private RelationshipType getRelationshipTypeOrFail( String dimension )
    {
        BaseIdentifiableObject baseIdentifiableObject = getIdObjectOrFail( dimension );
        return (baseIdentifiableObject instanceof RelationshipType ? (RelationshipType) baseIdentifiableObject : null);
    }

    private BaseIdentifiableObject getIdObjectOrFail( String dimension )
    {
        Stream<Supplier<BaseIdentifiableObject>> fetchers = Stream.of(
            () -> relationshipTypeService.getRelationshipType( getFirstElement( dimension ) ),
            () -> programStageService.getProgramStage( getFirstElement( dimension ) ) );

        boolean requiresIdObject = dimension.split( "\\" + PROGRAMSTAGE_SEP ).length > 1;

        Optional<BaseIdentifiableObject> found = fetchers.map( Supplier::get ).filter( Objects::nonNull ).findFirst();

        if ( requiresIdObject && found.isEmpty() )
        {
            throwIllegalQueryEx( ErrorCode.E7226, dimension );
        }

        return found.orElse( null );
    }
}
