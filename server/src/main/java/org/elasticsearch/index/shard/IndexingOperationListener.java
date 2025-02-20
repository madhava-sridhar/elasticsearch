/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */
package org.elasticsearch.index.shard;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.index.engine.Engine;

import java.util.List;
import java.util.function.Supplier;

/**
 * An indexing listener for indexing, delete, events.
 */
public interface IndexingOperationListener {

    /**
     * Called before the indexing occurs.
     */
    default Engine.Index preIndex(ShardId shardId, Engine.Index operation) {
        return operation;
    }

    /**
     * Called after the indexing operation occurred. Note that this is
     * also called when indexing a document did not succeed due to document
     * related failures. See {@link #postIndex(ShardId, Engine.Index, Exception)}
     * for engine level failures
     */
    default void postIndex(ShardId shardId, Engine.Index index, Engine.IndexResult result) {}

    /**
     * Called after the indexing operation occurred with engine level exception.
     * See {@link #postIndex(ShardId, Engine.Index, Engine.IndexResult)} for document
     * related failures
     */
    default void postIndex(ShardId shardId, Engine.Index index, Exception ex) {}

    /**
     * Called before the delete occurs.
     */
    default Engine.Delete preDelete(ShardId shardId, Engine.Delete delete) {
        return delete;
    }

    /**
     * Called after the delete operation occurred. Note that this is
     * also called when deleting a document did not succeed due to document
     * related failures. See {@link #postDelete(ShardId, Engine.Delete, Exception)}
     * for engine level failures
     */
    default void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {}

    /**
     * Called after the delete operation occurred with engine level exception.
     * See {@link #postDelete(ShardId, Engine.Delete, Engine.DeleteResult)} for document
     * related failures
     */
    default void postDelete(ShardId shardId, Engine.Delete delete, Exception ex) {}

    /**
     * Called when a {@link org.elasticsearch.action.bulk.TransportShardBulkAction} is about to perform index and/or delete operation(s)
     * on a primary shard.
     *
     * This is called from a transport thread and therefore the function should be lightweight and not block the thread. The acquired
     * listener(s) can be asynchronously completed on another thread at a later time.
     *
     * @param indexShard the shard the bulk is about to be performed on
     * @param proceedListenerSupplier call this immediately to get a listener which must be completed so that the bulk can proceed.
     */
    default void preBulkOnPrimary(IndexShard indexShard, Supplier<ActionListener<Void>> proceedListenerSupplier) {}

    /**
     * A Composite listener that multiplexes calls to each of the listeners methods.
     */
    final class CompositeListener implements IndexingOperationListener {
        private final List<IndexingOperationListener> listeners;
        private final Logger logger;

        public CompositeListener(List<IndexingOperationListener> listeners, Logger logger) {
            this.listeners = listeners;
            this.logger = logger;
        }

        @Override
        public Engine.Index preIndex(ShardId shardId, Engine.Index operation) {
            assert operation != null;
            for (IndexingOperationListener listener : listeners) {
                try {
                    listener.preIndex(shardId, operation);
                } catch (Exception e) {
                    logger.warn(() -> "preIndex listener [" + listener + "] failed", e);
                }
            }
            return operation;
        }

        @Override
        public void postIndex(ShardId shardId, Engine.Index index, Engine.IndexResult result) {
            assert index != null;
            for (IndexingOperationListener listener : listeners) {
                try {
                    listener.postIndex(shardId, index, result);
                } catch (Exception e) {
                    logger.warn(() -> "postIndex listener [" + listener + "] failed", e);
                }
            }
        }

        @Override
        public void postIndex(ShardId shardId, Engine.Index index, Exception ex) {
            assert index != null && ex != null;
            for (IndexingOperationListener listener : listeners) {
                try {
                    listener.postIndex(shardId, index, ex);
                } catch (Exception inner) {
                    inner.addSuppressed(ex);
                    logger.warn(() -> "postIndex listener [" + listener + "] failed", inner);
                }
            }
        }

        @Override
        public Engine.Delete preDelete(ShardId shardId, Engine.Delete delete) {
            assert delete != null;
            for (IndexingOperationListener listener : listeners) {
                try {
                    listener.preDelete(shardId, delete);
                } catch (Exception e) {
                    logger.warn(() -> "preDelete listener [" + listener + "] failed", e);
                }
            }
            return delete;
        }

        @Override
        public void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {
            assert delete != null;
            for (IndexingOperationListener listener : listeners) {
                try {
                    listener.postDelete(shardId, delete, result);
                } catch (Exception e) {
                    logger.warn(() -> "postDelete listener [" + listener + "] failed", e);
                }
            }
        }

        @Override
        public void postDelete(ShardId shardId, Engine.Delete delete, Exception ex) {
            assert delete != null && ex != null;
            for (IndexingOperationListener listener : listeners) {
                try {
                    listener.postDelete(shardId, delete, ex);
                } catch (Exception inner) {
                    inner.addSuppressed(ex);
                    logger.warn(() -> "postDelete listener [" + listener + "] failed", inner);
                }
            }
        }

        @Override
        public void preBulkOnPrimary(IndexShard indexShard, Supplier<ActionListener<Void>> proceedListenerSupplier) {
            for (IndexingOperationListener listener : listeners) {
                try {
                    listener.preBulkOnPrimary(indexShard, proceedListenerSupplier);
                } catch (Exception e) {
                    logger.warn(() -> "preBulkOnPrimary listener [" + listener + "] failed", e);
                }
            }
        }
    }
}
