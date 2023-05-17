/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.catalog.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.catalog.spi.FederatedCacheStore;
import org.eclipse.edc.catalog.store.sql.schema.ContractOfferStatements;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;

/**
 * An ephemeral SQL cache store.
 */
public class SqlFederatedCacheStore extends AbstractSqlStore implements FederatedCacheStore {

    private final ContractOfferStatements statements;

    private final AssetIndex assetIndex;

    private final String contractOfferExistsMessage = "Contract Offer with ID %s already exists";

    public SqlFederatedCacheStore(DataSourceRegistry dataSourceRegistry,
                                  String dataSourceName,
                                  TransactionContext transactionContext,
                                  ObjectMapper objectMapper,
                                  ContractOfferStatements statements,
                                  AssetIndex assetIndex) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper);
        this.statements = statements;
        this.assetIndex = assetIndex;
    }

    @Override
    public void save(ContractOffer contractOffer) {
        // TODO Validate not null attributes

        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (existsById(connection, contractOffer.getId())) {
                    throw new EdcPersistenceException(String.format(contractOfferExistsMessage, contractOffer.getId()));
                }

                executeQuery(connection, statements.getInsertTemplate(),
                        contractOffer.getId(),
                        toJson(contractOffer.getPolicy()),
                        contractOffer.getAsset().getId(),
                        contractOffer.getProvider() != null ? contractOffer.getProvider().toString() : null,
                        contractOffer.getConsumer() != null ? contractOffer.getConsumer().toString() : null,
                        mapFromZonedDateTime(contractOffer.getOfferStart()),
                        mapFromZonedDateTime(contractOffer.getOfferEnd()),
                        mapFromZonedDateTime(contractOffer.getContractStart()),
                        mapFromZonedDateTime(contractOffer.getContractEnd())
                );

            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    private boolean existsById(Connection connection, String definitionId) {
        var sql = statements.getCountTemplate();
        try (var stream = executeQuery(connection, false, this::mapCount, sql, definitionId)) {
            return stream.findFirst().orElse(0L) > 0;
        }
    }

    private long mapCount(ResultSet resultSet) throws SQLException {
        return resultSet.getLong(1);
    }

    @Override
    public Collection<ContractOffer> query(List<Criterion> query) {
        Objects.requireNonNull(query);

        return transactionContext.execute(() -> {
            try {
                var statement = statements.createQuery(QuerySpec.Builder.newInstance().filter(query).build());
                return executeQuery(getConnection(), true, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void deleteExpired() {
    }

    @Override
    public void expireAll() {
    }

    private ContractOffer mapResultSet(ResultSet resultSet) throws Exception {

        var policy = fromJson(resultSet.getString(statements.getPolicyColumn()), Policy.class);
        // TODO Implement invalid policy json error

        var assetId = resultSet.getString(statements.getAssetIdColumn());
        Asset asset = assetIndex.findById(assetId);
        // TODO Implement assert not found error

        return ContractOffer.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .policy(policy)
                .asset(asset)
                .provider(mapToUri(resultSet, statements.getUriProviderColumn()))
                .consumer(mapToUri(resultSet, statements.getUriConsumerColumn()))
                .offerStart(mapToZonedDateTime(resultSet, statements.getOfferStartColumn()))
                .offerEnd(mapToZonedDateTime(resultSet, statements.getOfferEndColumn()))
                .contractStart(mapToZonedDateTime(resultSet, statements.getContractStartColumn()))
                .contractEnd(mapToZonedDateTime(resultSet, statements.getContractEndColumn()))
                .build();
    }

    private Long mapFromZonedDateTime(ZonedDateTime zonedDateTime) throws Exception {
        return zonedDateTime != null ?
                zonedDateTime.toEpochSecond() :
                null;
    }

    private URI mapToUri(ResultSet resultSet, String column) throws Exception {
        return resultSet.getString(column) != null ?
                new URI(resultSet.getString(column)) :
                null;
    }

    private ZonedDateTime mapToZonedDateTime(ResultSet resultSet, String column) throws Exception {
        return resultSet.getString(column) != null ?
                Instant.ofEpochSecond(resultSet.getLong(statements.getOfferStartColumn())).atZone(ZoneId.of("Z")) :
                null;
    }
}
