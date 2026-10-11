package base;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BusinessIdGenerator {

    private static final List<IdSequence> SEQUENCES = List.of(
            new IdSequence("isibo_member_id_seq", "members", "member_id", "MEM"),
            new IdSequence("isibo_plan_id_seq", "insurance_plans", "insurance_plan_id", "PLN"),
            new IdSequence("isibo_coverage_id_seq", "coverages", "coverage_id", "COV"),
            new IdSequence("isibo_claim_id_seq", "claims", "claim_id", "CLM"),
            new IdSequence("isibo_payment_id_seq", "payments", "payment_id", "PAY"),
            new IdSequence("isibo_provider_id_seq", "healthcare_providers", "provider_id", "HCP"),
            new IdSequence("isibo_officer_id_seq", "insurance_officers", "officer_id", "OFF")
    );

    private final JdbcTemplate jdbcTemplate;
    private final boolean postgres;

    public BusinessIdGenerator(JdbcTemplate jdbcTemplate, DataSource dataSource) throws SQLException {
        this.jdbcTemplate = jdbcTemplate;
        try (var connection = dataSource.getConnection()) {
            this.postgres = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        }
    }

    @PostConstruct
    public void initializeSequences() {
        for (IdSequence sequence : SEQUENCES) {
            long nextValue = jdbcTemplate.queryForList(
                            "SELECT " + sequence.column() + " FROM " + sequence.table(), String.class)
                    .stream()
                    .mapToLong(value -> numericSuffix(value, sequence.prefix()))
                    .max()
                    .orElse(0L) + 1L;
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS " + sequence.name() + " START WITH " + nextValue);
        }
    }

    public String nextMemberId() {
        return next("isibo_member_id_seq", "MEM");
    }

    public String nextPlanId() {
        return next("isibo_plan_id_seq", "PLN");
    }

    public String nextCoverageId() {
        return next("isibo_coverage_id_seq", "COV");
    }

    public String nextClaimId() {
        return next("isibo_claim_id_seq", "CLM");
    }

    public String nextPaymentId() {
        return next("isibo_payment_id_seq", "PAY");
    }

    public String nextProviderId() {
        return next("isibo_provider_id_seq", "HCP");
    }

    public String nextOfficerId() {
        return next("isibo_officer_id_seq", "OFF");
    }

    private String next(String sequenceName, String prefix) {
        String sql = postgres
                ? "SELECT nextval('" + sequenceName + "')"
                : "SELECT NEXT VALUE FOR " + sequenceName;
        long value = jdbcTemplate.queryForObject(sql, Long.class);
        return prefix + "-" + String.format("%03d", value);
    }

    private long numericSuffix(String value, String prefix) {
        if (value == null) {
            return 0;
        }
        String normalized = value.trim();
        Matcher matcher = Pattern.compile("^" + Pattern.quote(prefix) + "-([0-9]+)$", Pattern.CASE_INSENSITIVE)
                .matcher(normalized);
        return matcher.matches() ? Long.parseLong(matcher.group(1)) : 0;
    }

    private record IdSequence(String name, String table, String column, String prefix) {
    }
}
