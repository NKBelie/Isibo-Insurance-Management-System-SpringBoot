package coverage.service;

import coverage.domain.Coverage;

import java.util.List;
import java.util.UUID;

public interface CoverageService {
    Coverage registerCoverage(Coverage coverage);
    Coverage updateCoverage(UUID coverageId, Coverage coverage);
    Coverage deleteCoverage(UUID coverageId);
    Coverage findCoverageById(UUID coverageId);
    List<Coverage> findAllCoverage();
}
