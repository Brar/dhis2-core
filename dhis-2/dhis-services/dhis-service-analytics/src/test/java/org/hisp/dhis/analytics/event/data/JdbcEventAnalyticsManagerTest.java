/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.analytics.event.data.JdbcEventAnalyticsManager.ExceptionHandler.handle;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.feedback.ErrorCode.E7132;
import static org.hisp.dhis.feedback.ErrorCode.E7133;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;
import static org.junit.Assert.assertThrows;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.postgresql.util.PSQLState.BAD_DATETIME_FORMAT;
import static org.postgresql.util.PSQLState.DIVISION_BY_ZERO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams.Builder;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class JdbcEventAnalyticsManagerTest
{
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StatementBuilder statementBuilder;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder;

    @Rule
    public MockitoRule mockitoRule = rule();

    private JdbcEventAnalyticsManager jdbcEventAnalyticsManager;

    @Before
    public void setUp()
    {
        jdbcEventAnalyticsManager = new JdbcEventAnalyticsManager( jdbcTemplate, statementBuilder,
            programIndicatorService, programIndicatorSubqueryBuilder );
    }

    @Test
    public void testHandlingDataIntegrityExceptionWhenDivisionByZero()
    {
        // Given
        final DataIntegrityViolationException aDivisionByZeroException = mockDataIntegrityExceptionDivisionByZero();

        // When
        assertThrows( E7132.getMessage(), QueryRuntimeException.class, () -> handle( aDivisionByZeroException ) );
    }

    @Test
    public void testHandlingAnyOtherDataIntegrityException()
    {
        // Given
        final DataIntegrityViolationException anyDataIntegrityException = mockAnyOtherDataIntegrityException();

        // When
        assertThrows( E7133.getMessage(), QueryRuntimeException.class, () -> handle( anyDataIntegrityException ) );
    }

    @Test
    public void testHandlingWhenExceptionIsNull()
    {
        // Given
        final DataIntegrityViolationException aNullException = null;

        // When
        assertThrows( E7133.getMessage(), QueryRuntimeException.class, () -> handle( aNullException ) );
    }

    @Test
    public void testHandlingWhenExceptionCauseNull()
    {
        // Given
        final DataIntegrityViolationException aNullExceptionCause = new DataIntegrityViolationException( "null",
            null );

        assertThrows( E7133.getMessage(), QueryRuntimeException.class, () -> handle( aNullExceptionCause ) );
    }

    @Test
    public void testHandlingWhenExceptionCauseIsNotPSQLException()
    {
        // Given
        final ArrayIndexOutOfBoundsException aRandomCause = new ArrayIndexOutOfBoundsException();
        final DataIntegrityViolationException aNonPSQLExceptionCause = new DataIntegrityViolationException(
            "not caused by PSQLException", aRandomCause );

        // When
        assertThrows( E7133.getMessage(), QueryRuntimeException.class, () -> handle( aNonPSQLExceptionCause ) );
    }

    @Test
    public void testWhereClauseWhenThereAreNoDimensionsAndOneNonAuthorizedCategoryOptionIsPresent()
    {
        // Given
        final CategoryOption aCategoryOptionA = stubCategoryOption( "cat-option-A", "uid-opt-A" );
        final CategoryOption aCategoryOptionB = stubCategoryOption( "cat-option-B", "uid-opt-B" );

        final List<CategoryOption> someCategoryOptions = newArrayList( aCategoryOptionA, aCategoryOptionB );
        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A", someCategoryOptions );

        final Map<String, List<CategoryOption>> nonAuthorizedCategoryOptions = ImmutableMap
            .<String, List<CategoryOption>> builder()
            .put( aCategoryA.getUid(), newArrayList( aCategoryOptionA ) )
            .build();

        final EventQueryParams theEventQueryParams = stubEventQueryParamsWithoutDimensions(
            newArrayList( aCategoryA ), nonAuthorizedCategoryOptions );
        final String theExpectedSql = " and ax.\"uid-cat-A\" not in ('uid-opt-A') ";

        // When
        final String actualSql = jdbcEventAnalyticsManager.getWhereClause( theEventQueryParams );

        // Then
        assertThat( actualSql, containsString( theExpectedSql ) );
    }

    @Test
    public void testTheWhereClauseWhenThereAreNoDimensionsAndMultipleNonAuthorizedCategoryOptionsIsPresent()
    {
        // Given
        final CategoryOption aCategoryOptionA = stubCategoryOption( "cat-option-A", "uid-opt-A" );
        final CategoryOption aCategoryOptionB = stubCategoryOption( "cat-option-B", "uid-opt-B" );
        final List<CategoryOption> someCategoryOptions = newArrayList( aCategoryOptionA, aCategoryOptionB );

        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A",
            someCategoryOptions );
        final Category aCategoryB = stubCategory( "cat-B", "uid-cat-B", someCategoryOptions );

        final Map<String, List<CategoryOption>> nonAuthorizedCategoryOptions = ImmutableMap
            .<String, List<CategoryOption>> builder()
            .put( aCategoryA.getUid(), newArrayList( aCategoryOptionA, aCategoryOptionB ) )
            .put( aCategoryB.getUid(), newArrayList( aCategoryOptionA, aCategoryOptionB ) )
            .build();

        final EventQueryParams theEventQueryParams = stubEventQueryParamsWithoutDimensions(
            newArrayList( aCategoryA, aCategoryB ), nonAuthorizedCategoryOptions );
        final String theExpectedSql = " and ax.\"uid-cat-A\" not in ('uid-opt-A', 'uid-opt-B')  or ax.\"uid-cat-B\" not in ('uid-opt-A', 'uid-opt-B') ";

        // When
        final String actualSql = jdbcEventAnalyticsManager.getWhereClause( theEventQueryParams );

        // Then
        assertThat( actualSql, containsString( theExpectedSql ) );
    }

    @Test
    public void testFilterOutNotAuthorizedCategoryOptionEventsWithEmptyCategoryOptionsMap()
    {
        // Given
        final Map<String, List<CategoryOption>> emptyCategoryOptionsMap = Maps.newHashMap();
        final EventQueryParams theEventQueryParamsWithEmptyCategoryList = stubEventQueryParamsWithoutDimensions(
            newArrayList(), emptyCategoryOptionsMap );

        // When
        final String actualSql = jdbcEventAnalyticsManager
            .filterOutNotAuthorizedCategoryOptionEvents( theEventQueryParamsWithEmptyCategoryList );

        // Then
        assertThat( actualSql, isEmptyString() );
    }

    @Test
    public void testFilterOutNotAuthorizedCategoryOptionEventsWithNoCategoryOptions()
    {
        // Given
        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A", newArrayList() );
        final Category aCategoryB = stubCategory( "cat-B", "uid-cat-B", newArrayList() );
        final EventQueryParams theEventQueryParams = stubEventQueryParamsWithoutDimensions(
            newArrayList( aCategoryA, aCategoryB ), new HashMap<>() );

        // When
        final String actualSql = jdbcEventAnalyticsManager
            .filterOutNotAuthorizedCategoryOptionEvents( theEventQueryParams );

        // Then
        assertThat( actualSql, isEmptyString() );
    }

    private DataIntegrityViolationException mockDataIntegrityExceptionDivisionByZero()
    {
        final PSQLException psqlException = new PSQLException( "ERROR: division by zero", DIVISION_BY_ZERO );

        return new DataIntegrityViolationException(
            "ERROR: division by zero; nested exception is org.postgresql.util.PSQLException: ERROR: division by zero",
            psqlException );
    }

    private DataIntegrityViolationException mockAnyOtherDataIntegrityException()
    {
        final PSQLException psqlException = new PSQLException( "ERROR: bad time format", BAD_DATETIME_FORMAT );

        return new DataIntegrityViolationException(
            "ERROR: bad time format; nested exception is org.postgresql.util.PSQLException: ERROR: bad time format",
            psqlException );
    }

    private Category stubCategory( final String name, final String uid, final List<CategoryOption> categoryOptions )
    {
        final Category category = new Category( name, ATTRIBUTE );
        category.setCategoryOptions( newArrayList( categoryOptions ) );
        category.setUid( uid );
        return category;
    }

    private CategoryOption stubCategoryOption( final String name, final String uid )
    {
        final CategoryOption categoryOption = new CategoryOption( name );
        categoryOption.setUid( uid );
        return categoryOption;
    }

    private EventQueryParams stubEventQueryParamsWithoutDimensions( final List<Category> categories,
        final Map<String, List<CategoryOption>> nonAuthorizedCategoryOptions )
    {
        final DimensionalObject doA = new BaseDimensionalObject( ORGUNIT_DIM_ID, ORGANISATION_UNIT, newArrayList() );
        final DimensionalObject doC = new BaseDimensionalObject( "Cz3WQznvrCM", PROGRAM_ATTRIBUTE, newArrayList() );

        final Period period = getPeriodFromIsoString( "2019Q2" );

        final CategoryCombo categoryCombo = new CategoryCombo( "cat-combo", ATTRIBUTE );
        categoryCombo.setCategories( categories );

        final Program program = new Program( "program", "a program" );
        program.setCategoryCombo( categoryCombo );

        final QueryExclusion exclusion = new QueryExclusion();
        exclusion.setNonAuthorizedCategoryOptions( nonAuthorizedCategoryOptions );

        final DataQueryParams dataQueryParams = newBuilder().addDimension( doA ).addDimension( doC )
            .withPeriods( Lists.newArrayList( period ) ).withPeriodType( period.getPeriodType().getIsoFormat() )
            .withExclusion( exclusion )
            .build();

        final Builder eventQueryParamsBuilder = new Builder( dataQueryParams );
        final EventQueryParams eventQueryParams = eventQueryParamsBuilder.withProgram( program ).build();

        return eventQueryParams;
    }
}
