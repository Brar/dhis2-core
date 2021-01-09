package org.hisp.dhis.tracker.preprocess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class DuplicateRelationshipsPreProcessorTest
{
    private DuplicateRelationshipsPreProcessor preProcessor;

    private TrackerPreheat preheat;

    private final String REL_TYPE_BIDIRECTIONAL_UID = CodeGenerator.generateUid();

    private final String REL_TYPE_NONBIDIRECTIONAL_UID = CodeGenerator.generateUid();

    @Before
    public void setUp()
    {
        preheat = new TrackerPreheat();

        RelationshipType relationshipTypeBidirectional = new RelationshipType();
        relationshipTypeBidirectional.setUid( REL_TYPE_BIDIRECTIONAL_UID );
        relationshipTypeBidirectional.setBidirectional( true );

        RelationshipType relationshipTypeNonBidirectional = new RelationshipType();
        relationshipTypeNonBidirectional.setUid( REL_TYPE_NONBIDIRECTIONAL_UID );

        preheat.put( TrackerIdentifier.UID, relationshipTypeBidirectional );
        preheat.put( TrackerIdentifier.UID, relationshipTypeNonBidirectional );

        this.preProcessor = new DuplicateRelationshipsPreProcessor();
    }


    @Test
    public void test_relationshipIsIgnored_on_null_relType()
    {
        String relType = CodeGenerator.generateUid();
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        Relationship relationship2 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }

    /*
     * Verifies that:
     * 
     * - given 2 identical relationships
     * 
     * - one is removed
     */
    @Test
    public void test_on_identical_rels_1_is_removed()
    {
        String relType = REL_TYPE_NONBIDIRECTIONAL_UID;
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        Relationship relationship2 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 non-identical relationships
     *
     * - none is removed
     */
    @Test
    public void test_on_different_rels_none_is_removed()
    {
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( REL_TYPE_NONBIDIRECTIONAL_UID )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        Relationship relationship2 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( REL_TYPE_NONBIDIRECTIONAL_UID )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .enrollment( toTeiUid )
                .build() )
            .build();

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 relationships having identical but "inverted" data
     *
     * - none is removed
     */
    @Test
    public void test_on_identical_but_inverted_rels_none_is_removed()
    {
        String relType = REL_TYPE_NONBIDIRECTIONAL_UID;
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .bidirectional( false )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        Relationship relationship2 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .bidirectional( false )
            .from( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .build();

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 identical relationships having identical but "inverted" data
     * 
     * - and relationship type's bidirectional property = true
     *
     * - none is removed
     */
    @Test
    public void test_on_identical_rels_but_inverted_type_bi_1_is_removed()
    {
        String relType = REL_TYPE_BIDIRECTIONAL_UID;
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        Relationship relationship2 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .from( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .build();

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 identical relationships
     *
     * - and relationship type's bidirectional property = true
     *
     * - one is removed
     */
    @Test
    public void test_on_identical_rels_relType_bi_1_is_removed()
    {
        String relType = REL_TYPE_BIDIRECTIONAL_UID;
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .bidirectional( true )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        Relationship relationship2 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .bidirectional( true )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }
}