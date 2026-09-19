package coverage.service;

import coverage.domain.Coverage;

import java.util.List;

public interface CoverageService {
    Coverage registerCoverage(Coverage theCoverage);
    Coverage updateCoverage(Coverage theCoverage);
    Coverage deleteCoverage(Coverage theCoverage);
    Coverage findAllCoverageById(Coverage theCoverage);
    List<Coverage> findAllCoverage();
}
