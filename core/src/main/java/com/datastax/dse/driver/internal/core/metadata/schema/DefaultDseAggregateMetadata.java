/*
 * Copyright DataStax, Inc.
 *
 * This software can be used solely with DataStax Enterprise. Please consult the license at
 * http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.dse.driver.internal.core.metadata.schema;

import com.datastax.dse.driver.api.core.metadata.schema.DseAggregateMetadata;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.metadata.schema.FunctionSignature;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.internal.core.metadata.schema.DefaultAggregateMetadata;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import net.jcip.annotations.Immutable;

@Immutable
public class DefaultDseAggregateMetadata extends DefaultAggregateMetadata
    implements DseAggregateMetadata {

  private final boolean deterministic;

  public DefaultDseAggregateMetadata(
      @NonNull CqlIdentifier keyspace,
      @NonNull FunctionSignature signature,
      @Nullable FunctionSignature finalFuncSignature,
      @Nullable Object initCond,
      @NonNull DataType returnType,
      @NonNull FunctionSignature stateFuncSignature,
      @NonNull DataType stateType,
      @NonNull TypeCodec<Object> stateTypeCodec,
      boolean deterministic) {
    super(
        keyspace,
        signature,
        finalFuncSignature,
        initCond,
        returnType,
        stateFuncSignature,
        stateType,
        stateTypeCodec);
    this.deterministic = deterministic;
  }

  @Override
  public boolean isDeterministic() {
    return this.deterministic;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof DseAggregateMetadata) {
      DseAggregateMetadata that = (DseAggregateMetadata) other;
      return Objects.equals(this.getKeyspace(), that.getKeyspace())
          && Objects.equals(this.getSignature(), that.getSignature())
          && Objects.equals(
              this.getFinalFuncSignature().orElse(null), that.getFinalFuncSignature().orElse(null))
          && Objects.equals(this.getInitCond().orElse(null), that.getInitCond().orElse(null))
          && Objects.equals(this.getReturnType(), that.getReturnType())
          && Objects.equals(this.getStateFuncSignature(), that.getStateFuncSignature())
          && Objects.equals(this.getStateType(), that.getStateType())
          && this.deterministic == that.isDeterministic();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getKeyspace(),
        getSignature(),
        getFinalFuncSignature(),
        getInitCond(),
        getReturnType(),
        getStateFuncSignature(),
        getStateType(),
        deterministic);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Aggregate Name: ")
        .append(getSignature().getName().asCql(false))
        .append(", Keyspace: ")
        .append(getKeyspace().asCql(false))
        .append(", Return Type: ")
        .append(getReturnType().asCql(false, false))
        .append(", Deterministic: ")
        .append(deterministic);
    return sb.toString();
  }
}