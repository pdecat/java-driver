/*
 * Copyright DataStax, Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.driver.api.core.cql.continuous.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.dse.driver.DseSessionMetric;
import com.datastax.dse.driver.api.core.DseSession;
import com.datastax.dse.driver.api.core.cql.continuous.ContinuousPagingITBase;
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveRow;
import com.datastax.dse.driver.api.testinfra.session.DseSessionRuleBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metrics.DefaultNodeMetric;
import com.datastax.oss.driver.api.testinfra.DseRequirement;
import com.datastax.oss.driver.api.testinfra.ccm.CcmRule;
import com.datastax.oss.driver.api.testinfra.session.SessionRule;
import com.datastax.oss.driver.api.testinfra.session.SessionUtils;
import com.datastax.oss.driver.categories.ParallelizableTests;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.reactivex.Flowable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@DseRequirement(
    min = "5.1.0",
    description = "Continuous paging is only available from 5.1.0 onwards")
@Category(ParallelizableTests.class)
@RunWith(DataProviderRunner.class)
public class ContinuousPagingReactiveIT extends ContinuousPagingITBase {

  private static CcmRule ccmRule = CcmRule.getInstance();

  private static SessionRule<DseSession> sessionRule =
      new DseSessionRuleBuilder(ccmRule)
          .withConfigLoader(
              SessionUtils.configLoaderBuilder()
                  .withStringList(
                      DefaultDriverOption.METRICS_SESSION_ENABLED,
                      Collections.singletonList(DseSessionMetric.CONTINUOUS_CQL_REQUESTS.getPath()))
                  .withStringList(
                      DefaultDriverOption.METRICS_NODE_ENABLED,
                      Collections.singletonList(DefaultNodeMetric.CQL_MESSAGES.getPath()))
                  .build())
          .build();

  @ClassRule public static TestRule chain = RuleChain.outerRule(ccmRule).around(sessionRule);

  @BeforeClass
  public static void setUp() {
    initialize(sessionRule.session(), sessionRule.slowProfile());
  }

  @Test
  @UseDataProvider("pagingOptions")
  public void should_execute_reactively(Options options) {
    DseSession session = sessionRule.session();
    SimpleStatement statement = SimpleStatement.newInstance("SELECT v from test where k=?", KEY);
    DriverExecutionProfile profile = options.asProfile(session);
    ContinuousReactiveResultSet rs =
        session.executeContinuouslyReactive(statement.setExecutionProfile(profile));
    List<ReactiveRow> results = Flowable.fromPublisher(rs).toList().blockingGet();
    assertThat(results).hasSize(options.expectedRows);
    Set<ExecutionInfo> expectedExecInfos = new LinkedHashSet<>();
    for (int i = 0; i < results.size(); i++) {
      ReactiveRow row = results.get(i);
      assertThat(row.getInt("v")).isEqualTo(i);
      expectedExecInfos.add(row.getExecutionInfo());
    }

    List<ExecutionInfo> execInfos =
        Flowable.<ExecutionInfo>fromPublisher(rs.getExecutionInfos()).toList().blockingGet();
    // DSE may send an empty page as it can't always know if it's done paging or not yet.
    // See: CASSANDRA-8871. In this case, this page's execution info appears in
    // rs.getExecutionInfos(), but is not present in expectedExecInfos since the page did not
    // contain any rows.
    assertThat(execInfos).containsAll(expectedExecInfos);

    List<ColumnDefinitions> colDefs =
        Flowable.<ColumnDefinitions>fromPublisher(rs.getColumnDefinitions()).toList().blockingGet();
    ReactiveRow first = results.get(0);
    assertThat(colDefs).hasSize(1).containsExactly(first.getColumnDefinitions());

    List<Boolean> wasApplied = Flowable.fromPublisher(rs.wasApplied()).toList().blockingGet();
    assertThat(wasApplied).hasSize(1).containsExactly(first.wasApplied());

    validateMetrics(session);
  }
}