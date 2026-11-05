package civsim.dto;

import java.util.List;

/**
 * Detailed family profile containing living and historical member lists.
 */
public record FamilyDetailDto(
        FamilySummaryDto summary,
        List<PersonSummaryDto> livingMembers,
        List<PersonSummaryDto> deceasedMembers,
        List<String> clanAffiliations
) {
    public FamilyDetailDto {
        livingMembers = (livingMembers != null) ? List.copyOf(livingMembers) : List.of();
        deceasedMembers = (deceasedMembers != null) ? List.copyOf(deceasedMembers) : List.of();
        clanAffiliations = (clanAffiliations != null) ? List.copyOf(clanAffiliations) : List.of();
    }
}

