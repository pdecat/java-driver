/*
 * Copyright DataStax, Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.driver.api.core.metadata.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.dse.driver.api.core.DseSession;
import com.datastax.dse.driver.api.testinfra.session.DseSessionRuleBuilder;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.metadata.schema.FunctionMetadata;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.testinfra.DseRequirement;
import com.datastax.oss.driver.api.testinfra.ccm.CcmRule;
import com.datastax.oss.driver.api.testinfra.session.SessionRule;
import java.util.Optional;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

@DseRequirement(min = "6.0")
public class DseFunctionMetadataIT extends AbstractMetadataIT {

  private static final CcmRule CCM_RULE = CcmRule.getInstance();

  private static final SessionRule<DseSession> SESSION_RULE =
      new DseSessionRuleBuilder(CCM_RULE).build();

  @ClassRule
  public static final TestRule CHAIN = RuleChain.outerRule(CCM_RULE).around(SESSION_RULE);

  @Override
  public SessionRule<DseSession> getSessionRule() {
    return DseFunctionMetadataIT.SESSION_RULE;
  }

  @Test
  public void should_parse_function_without_deterministic_or_monotonic() throws Exception {
    String cqlFunction =
        "CREATE FUNCTION nondetf(i int) RETURNS NULL ON NULL INPUT RETURNS int LANGUAGE java AS 'return new java.util.Random().nextInt(i);';";
    execute(cqlFunction);
    DseKeyspaceMetadata keyspace = getKeyspace();
    Optional<FunctionMetadata> functionOpt = keyspace.getFunction("nondetf", DataTypes.INT);
    assertThat(functionOpt.map(DseFunctionMetadata.class::cast))
        .hasValueSatisfying(
            function -> {
              assertThat(function.isDeterministic()).isFalse();
              assertThat(function.isMonotonic()).isFalse();
              assertThat(function.getMonotonicArgumentNames()).isEmpty();
              assertThat(function.getLanguage()).isEqualTo("java");
              assertThat(function.getReturnType()).isEqualTo(DataTypes.INT);
              assertThat(function.getBody()).isEqualTo("return new java.util.Random().nextInt(i);");
              assertThat(function.describe(false))
                  .isEqualTo(
                      String.format(
                          "CREATE FUNCTION \"%s\".\"nondetf\"(\"i\" int) RETURNS NULL ON NULL INPUT RETURNS int LANGUAGE java AS 'return new java.util.Random().nextInt(i);';",
                          keyspace.getName().asInternal()));
            });
  }

  @Test
  public void should_parse_function_with_deterministic() throws Exception {
    String cqlFunction =
        "CREATE FUNCTION detf(i int, y int) RETURNS NULL ON NULL INPUT RETURNS int DETERMINISTIC LANGUAGE java AS 'return i+y;';";
    execute(cqlFunction);
    DseKeyspaceMetadata keyspace = getKeyspace();
    Optional<FunctionMetadata> functionOpt =
        keyspace.getFunction("detf", DataTypes.INT, DataTypes.INT);
    assertThat(functionOpt.map(DseFunctionMetadata.class::cast))
        .hasValueSatisfying(
            function -> {
              assertThat(function.isDeterministic()).isTrue();
              assertThat(function.isMonotonic()).isFalse();
              assertThat(function.getMonotonicArgumentNames()).isEmpty();
              assertThat(function.getLanguage()).isEqualTo("java");
              assertThat(function.getReturnType()).isEqualTo(DataTypes.INT);
              assertThat(function.getBody()).isEqualTo("return i+y;");
              assertThat(function.describe(false))
                  .isEqualTo(
                      String.format(
                          "CREATE FUNCTION \"%s\".\"detf\"(\"i\" int,\"y\" int) RETURNS NULL ON NULL INPUT RETURNS int DETERMINISTIC LANGUAGE java AS 'return i+y;';",
                          keyspace.getName().asInternal()));
            });
  }

  @Test
  public void should_parse_function_with_monotonic() throws Exception {
    String cqlFunction =
        "CREATE FUNCTION monotonic(dividend int, divisor int) CALLED ON NULL INPUT RETURNS int MONOTONIC LANGUAGE java AS 'return dividend / divisor;';";
    execute(cqlFunction);
    DseKeyspaceMetadata keyspace = getKeyspace();
    Optional<FunctionMetadata> functionOpt =
        keyspace.getFunction("monotonic", DataTypes.INT, DataTypes.INT);
    assertThat(functionOpt.map(DseFunctionMetadata.class::cast))
        .hasValueSatisfying(
            function -> {
              assertThat(function.isDeterministic()).isFalse();
              assertThat(function.isMonotonic()).isTrue();
              assertThat(function.getMonotonicArgumentNames())
                  .containsExactly(
                      CqlIdentifier.fromCql("dividend"), CqlIdentifier.fromCql("divisor"));
              assertThat(function.getLanguage()).isEqualTo("java");
              assertThat(function.getReturnType()).isEqualTo(DataTypes.INT);
              assertThat(function.getBody()).isEqualTo("return dividend / divisor;");
              assertThat(function.describe(false))
                  .isEqualTo(
                      String.format(
                          "CREATE FUNCTION \"%s\".\"monotonic\"(\"dividend\" int,\"divisor\" int) CALLED ON NULL INPUT RETURNS int MONOTONIC LANGUAGE java AS 'return dividend / divisor;';",
                          keyspace.getName().asInternal()));
            });
  }

  @Test
  public void should_parse_function_with_monotonic_on() throws Exception {
    String cqlFunction =
        "CREATE FUNCTION monotonic_on(dividend int, divisor int) CALLED ON NULL INPUT RETURNS int MONOTONIC ON \"dividend\" LANGUAGE java AS 'return dividend / divisor;';";
    execute(cqlFunction);
    DseKeyspaceMetadata keyspace = getKeyspace();
    Optional<FunctionMetadata> functionOpt =
        keyspace.getFunction("monotonic_on", DataTypes.INT, DataTypes.INT);
    assertThat(functionOpt.map(DseFunctionMetadata.class::cast))
        .hasValueSatisfying(
            function -> {
              assertThat(function.isDeterministic()).isFalse();
              assertThat(function.isMonotonic()).isFalse();
              assertThat(function.getMonotonicArgumentNames())
                  .containsExactly(CqlIdentifier.fromCql("dividend"));
              assertThat(function.getLanguage()).isEqualTo("java");
              assertThat(function.getReturnType()).isEqualTo(DataTypes.INT);
              assertThat(function.getBody()).isEqualTo("return dividend / divisor;");
              assertThat(function.describe(false))
                  .isEqualTo(
                      String.format(
                          "CREATE FUNCTION \"%s\".\"monotonic_on\"(\"dividend\" int,\"divisor\" int) CALLED ON NULL INPUT RETURNS int MONOTONIC ON \"dividend\" LANGUAGE java AS 'return dividend / divisor;';",
                          keyspace.getName().asInternal()));
            });
  }

  @Test
  public void should_parse_function_with_deterministic_and_monotonic() throws Exception {
    String cqlFunction =
        "CREATE FUNCTION det_and_monotonic(dividend int, divisor int) CALLED ON NULL INPUT RETURNS int DETERMINISTIC MONOTONIC LANGUAGE java AS 'return dividend / divisor;';";
    execute(cqlFunction);
    DseKeyspaceMetadata keyspace = getKeyspace();
    Optional<FunctionMetadata> functionOpt =
        keyspace.getFunction("det_and_monotonic", DataTypes.INT, DataTypes.INT);
    assertThat(functionOpt.map(DseFunctionMetadata.class::cast))
        .hasValueSatisfying(
            function -> {
              assertThat(function.isDeterministic()).isTrue();
              assertThat(function.isMonotonic()).isTrue();
              assertThat(function.getMonotonicArgumentNames())
                  .containsExactly(
                      CqlIdentifier.fromCql("dividend"), CqlIdentifier.fromCql("divisor"));
              assertThat(function.getLanguage()).isEqualTo("java");
              assertThat(function.getReturnType()).isEqualTo(DataTypes.INT);
              assertThat(function.getBody()).isEqualTo("return dividend / divisor;");
              assertThat(function.describe(false))
                  .isEqualTo(
                      String.format(
                          "CREATE FUNCTION \"%s\".\"det_and_monotonic\"(\"dividend\" int,\"divisor\" int) CALLED ON NULL INPUT RETURNS int DETERMINISTIC MONOTONIC LANGUAGE java AS 'return dividend / divisor;';",
                          keyspace.getName().asInternal()));
            });
  }

  @Test
  public void should_parse_function_with_deterministic_and_monotonic_on() throws Exception {
    String cqlFunction =
        "CREATE FUNCTION det_and_monotonic_on(dividend int, divisor int) CALLED ON NULL INPUT RETURNS int DETERMINISTIC MONOTONIC ON \"dividend\" LANGUAGE java AS 'return dividend / divisor;';";
    execute(cqlFunction);
    DseKeyspaceMetadata keyspace = getKeyspace();
    Optional<FunctionMetadata> functionOpt =
        keyspace.getFunction("det_and_monotonic_on", DataTypes.INT, DataTypes.INT);
    assertThat(functionOpt.map(DseFunctionMetadata.class::cast))
        .hasValueSatisfying(
            function -> {
              assertThat(function.isDeterministic()).isTrue();
              assertThat(function.isMonotonic()).isFalse();
              assertThat(function.getMonotonicArgumentNames())
                  .containsExactly(CqlIdentifier.fromCql("dividend"));
              assertThat(function.getLanguage()).isEqualTo("java");
              assertThat(function.getReturnType()).isEqualTo(DataTypes.INT);
              assertThat(function.getBody()).isEqualTo("return dividend / divisor;");
              assertThat(function.describe(false))
                  .isEqualTo(
                      String.format(
                          "CREATE FUNCTION \"%s\".\"det_and_monotonic_on\"(\"dividend\" int,\"divisor\" int) CALLED ON NULL INPUT RETURNS int DETERMINISTIC MONOTONIC ON \"dividend\" LANGUAGE java AS 'return dividend / divisor;';",
                          keyspace.getName().asInternal()));
            });
  }
}