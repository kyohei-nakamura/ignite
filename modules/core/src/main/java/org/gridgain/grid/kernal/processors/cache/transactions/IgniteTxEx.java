/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gridgain.grid.kernal.processors.cache.transactions;

import org.apache.ignite.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.lang.*;
import org.apache.ignite.transactions.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.apache.ignite.internal.util.lang.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Transaction managed by cache ({@code 'Ex'} stands for external).
 */
public interface IgniteTxEx<K, V> extends IgniteTx, GridTimeoutObject {
    /**
     *
     */
    @SuppressWarnings("PublicInnerClass")
    public enum FinalizationStatus {
        /** Transaction was not finalized yet. */
        NONE,

        /** Transaction is being finalized by user. */
        USER_FINISH,

        /** Recovery request is received, user finish requests should be ignored. */
        RECOVERY_WAIT,

        /** Transaction is being finalized by recovery procedure. */
        RECOVERY_FINISH
    }

    /**
     * @return Size of the transaction.
     */
    public int size();

    /**
     * @return {@code True} if transaction is allowed to use store.
     */
    public boolean storeEnabled();

    /**
     * @return {@code True} if transaction is allowed to use store and transactions spans one or more caches with
     *      store enabled.
     */
    public boolean storeUsed();

    /**
     * Checks if this is system cache transaction. System transactions are isolated from user transactions
     * because some of the public API methods may be invoked inside user transactions and internally start
     * system cache transactions.
     *
     * @return {@code True} if transaction is started for system cache.
     */
    public boolean system();

    /**
     * @return Last recorded topology version.
     */
    public long topologyVersion();

    /**
     * @return Flag indicating whether transaction is implicit with only one key.
     */
    public boolean implicitSingle();

    /**
     * @return Collection of cache IDs involved in this transaction.
     */
    public Collection<Integer> activeCacheIds();

    /**
     * Attempts to set topology version and returns the current value.
     * If topology version was previously set, then it's value will
     * be returned (but not updated).
     *
     * @param topVer Topology version.
     * @return Recorded topology version.
     */
    public long topologyVersion(long topVer);

    /**
     * @return {@code True} if transaction is empty.
     */
    public boolean empty();

    /**
     * @return {@code True} if transaction group-locked.
     */
    public boolean groupLock();

    /**
     * @return Group lock key if {@link #groupLock()} is {@code true}.
     */
    @Nullable public IgniteTxKey groupLockKey();

    /**
     * @return {@code True} if preparing flag was set with this call.
     */
    public boolean markPreparing();

    /**
     * @param status Finalization status to set.
     * @return {@code True} if could mark was set.
     */
    public boolean markFinalizing(FinalizationStatus status);

    /**
     * @param cacheCtx Cache context.
     * @param part Invalid partition.
     */
    public void addInvalidPartition(GridCacheContext<K, V> cacheCtx, int part);

    /**
     * @return Invalid partitions.
     */
    public Set<Integer> invalidPartitions();

    /**
     * Gets owned version for near remote transaction.
     *
     * @param key Key to get version for.
     * @return Owned version, if any.
     */
    @Nullable public GridCacheVersion ownedVersion(IgniteTxKey<K> key);

    /**
     * Gets ID of additional node involved. For example, in DHT case, other node is
     * near node ID.
     *
     * @return Parent node IDs.
     */
    @Nullable public UUID otherNodeId();

    /**
     * @return Event node ID.
     */
    public UUID eventNodeId();

    /**
     * Gets node ID which directly started this transaction. In case of DHT local transaction it will be
     * near node ID, in case of DHT remote transaction it will be primary node ID, in case of replicated remote
     * transaction it will be starter node ID.
     *
     * @return Originating node ID.
     */
    public UUID originatingNodeId();

    /**
     * @return Master node IDs.
     */
    public Collection<UUID> masterNodeIds();

    /**
     * @return Near transaction ID.
     */
    @Nullable public GridCacheVersion nearXidVersion();

    /**
     * @return Transaction nodes mapping (primary node -> related backup nodes).
     */
    @Nullable public Map<UUID, Collection<UUID>> transactionNodes();

    /**
     * @param entry Entry to check.
     * @return {@code True} if lock is owned.
     * @throws GridCacheEntryRemovedException If entry has been removed.
     */
    public boolean ownsLock(GridCacheEntryEx<K, V> entry) throws GridCacheEntryRemovedException;

    /**
     * @param entry Entry to check.
     * @return {@code True} if lock is owned.
     */
    public boolean ownsLockUnsafe(GridCacheEntryEx<K, V> entry);

    /**
     * For Partitioned caches, this flag is {@code false} for remote DHT and remote NEAR
     * transactions because serializability of transaction is enforced on primary node. All
     * other transaction types must enforce it.
     *
     * @return Enforce serializable flag.
     */
    public boolean enforceSerializable();

    /**
     * @return {@code True} if near transaction.
     */
    public boolean near();

    /**
     * @return {@code True} if DHT transaction.
     */
    public boolean dht();

    /**
     * @return {@code True} if dht colocated transaction.
     */
    public boolean colocated();

    /**
     * @return {@code True} if transaction is local, {@code false} if it's remote.
     */
    public boolean local();

    /**
     * @return {@code True} if transaction is replicated.
     */
    public boolean replicated();

    /**
     * @return Subject ID initiated this transaction.
     */
    public UUID subjectId();

    /**
     * Task name hash in case if transaction was initiated within task execution.
     *
     * @return Task name hash.
     */
    public int taskNameHash();

    /**
     * @return {@code True} if transaction is user transaction, which means:
     * <ul>
     *     <li>Explicit</li>
     *     <li>Local</li>
     *     <li>Not DHT</li>
     * </ul>
     */
    public boolean user();

    /**
     * @return {@code True} if transaction is configured with synchronous commit flag.
     */
    public boolean syncCommit();

    /**
     * @return {@code True} if transaction is configured with synchronous rollback flag.
     */
    public boolean syncRollback();

    /**
     * @param key Key to check.
     * @return {@code True} if key is present.
     */
    public boolean hasWriteKey(IgniteTxKey<K> key);

    /**
     * @return Read set.
     */
    public Set<IgniteTxKey<K>> readSet();

    /**
     * @return Write set.
     */
    public Set<IgniteTxKey<K>> writeSet();

    /**
     * @return All transaction entries.
     */
    public Collection<IgniteTxEntry<K, V>> allEntries();

    /**
     * @return Write entries.
     */
    public Collection<IgniteTxEntry<K, V>> writeEntries();

    /**
     * @return Read entries.
     */
    public Collection<IgniteTxEntry<K, V>> readEntries();

    /**
     * @return Transaction write map.
     */
    public Map<IgniteTxKey<K>, IgniteTxEntry<K, V>> writeMap();

    /**
     * @return Transaction read map.
     */
    public Map<IgniteTxKey<K>, IgniteTxEntry<K, V>> readMap();

    /**
     * Gets pessimistic recovery writes, i.e. values that have never been sent to remote nodes with lock requests.
     *
     * @return Collection of recovery writes.
     */
    public Collection<IgniteTxEntry<K, V>> recoveryWrites();

    /**
     * Gets a list of entries that needs to be locked on the next step of prepare stage of
     * optimistic transaction.
     *
     * @return List of tx entries for optimistic locking.
     */
    public Collection<IgniteTxEntry<K, V>> optimisticLockEntries();

    /**
     * Seals transaction for updates.
     */
    public void seal();

    /**
     * @param key Key for the entry.
     * @return Entry for the key (either from write set or read set).
     */
    @Nullable public IgniteTxEntry<K, V> entry(IgniteTxKey<K> key);

    /**
     * @param ctx Cache context.
     * @param failFast Fail-fast flag.
     * @param key Key to look up.
     * @param filter Filter to check.
     * @return Current value for the key within transaction.
     * @throws GridCacheFilterFailedException If filter failed and failFast is {@code true}.
     */
     @Nullable public GridTuple<V> peek(
         GridCacheContext<K, V> ctx,
         boolean failFast,
         K key,
         @Nullable IgnitePredicate<GridCacheEntry<K, V>>[] filter) throws GridCacheFilterFailedException;

    /**
     * @return Start version.
     */
    public GridCacheVersion startVersion();

    /**
     * @return Transaction version.
     */
    public GridCacheVersion xidVersion();

    /**
     * @return Version created at commit time.
     */
    public GridCacheVersion commitVersion();

    /**
     * @param commitVer Commit version.
     * @return {@code True} if version was set.
     */
    public boolean commitVersion(GridCacheVersion commitVer);

    /**
     * @return End version (a.k.a. <tt>'tnc'</tt> or <tt>'transaction number counter'</tt>)
     *      assigned to this transaction at the end of write phase.
     */
    public GridCacheVersion endVersion();

    /**
     * Prepare state.
     *
     * @throws IgniteCheckedException If failed.
     */
    public void prepare() throws IgniteCheckedException;

    /**
     * Prepare stage.
     *
     * @return Future for prepare step.
     */
    public IgniteFuture<IgniteTxEx<K, V>> prepareAsync();

    /**
     * @param endVer End version (a.k.a. <tt>'tnc'</tt> or <tt>'transaction number counter'</tt>)
     *      assigned to this transaction at the end of write phase.
     */
    public void endVersion(GridCacheVersion endVer);

    /**
     * @return Transaction write version. For all transactions except DHT transactions, will be equal to
     *      {@link #xidVersion()}.
     */
    public GridCacheVersion writeVersion();

    /**
     * Sets write version.
     *
     * @param ver Write version.
     */
    public void writeVersion(GridCacheVersion ver);

    /**
     * @return Future for transaction completion.
     */
    public IgniteFuture<IgniteTx> finishFuture();

    /**
     * @param state Transaction state.
     * @return {@code True} if transition was valid, {@code false} otherwise.
     */
    public boolean state(IgniteTxState state);

    /**
     * @param invalidate Invalidate flag.
     */
    public void invalidate(boolean invalidate);

    /**
     * @param sysInvalidate System invalidate flag.
     */
    public void systemInvalidate(boolean sysInvalidate);

    /**
     * @return System invalidate flag.
     */
    public boolean isSystemInvalidate();

    /**
     * Asynchronously rollback this transaction.
     *
     * @return Rollback future.
     */
    public IgniteFuture<IgniteTx> rollbackAsync();

    /**
     * Asynchronously commits this transaction by initiating {@code two-phase-commit} process.
     *
     * @return Future for commit operation.
     */
    public IgniteFuture<IgniteTx> commitAsync();

    /**
     * Callback invoked whenever there is a lock that has been acquired
     * by this transaction for any of the participating entries.
     *
     * @param entry Cache entry.
     * @param owner Lock candidate that won ownership of the lock.
     * @return {@code True} if transaction cared about notification.
     */
    public boolean onOwnerChanged(GridCacheEntryEx<K, V> entry, GridCacheMvccCandidate<K> owner);

    /**
     * @return {@code True} if transaction timed out.
     */
    public boolean timedOut();

    /**
     * @return {@code True} if transaction had completed successfully or unsuccessfully.
     */
    public boolean done();

    /**
     * @return {@code True} for OPTIMISTIC transactions.
     */
    public boolean optimistic();

    /**
     * @return {@code True} for PESSIMISTIC transactions.
     */
    public boolean pessimistic();

    /**
     * @return {@code True} if read-committed.
     */
    public boolean readCommitted();

    /**
     * @return {@code True} if repeatable-read.
     */
    public boolean repeatableRead();

    /**
     * @return {@code True} if serializable.
     */
    public boolean serializable();

    /**
     * Checks whether given key has been removed within transaction.
     *
     * @param key Key to check.
     * @return {@code True} if key has been removed.
     */
    public boolean removed(IgniteTxKey<K> key);

    /**
     * Gets allowed remaining time for this transaction.
     *
     * @return Remaining time.
     * @throws IgniteTxTimeoutException If transaction timed out.
     */
    public long remainingTime() throws IgniteTxTimeoutException;

    /**
     * @return Alternate transaction versions.
     */
    public Collection<GridCacheVersion> alternateVersions();

    /**
     * @return {@code True} if transaction needs completed versions for processing.
     */
    public boolean needsCompletedVersions();

    /**
     * @param base Base for committed versions.
     * @param committed Committed transactions relative to base.
     * @param rolledback Rolled back transactions relative to base.
     */
    public void completedVersions(GridCacheVersion base, Collection<GridCacheVersion> committed,
        Collection<GridCacheVersion> rolledback);

    /**
     * @return {@code True} if transaction has at least one internal entry.
     */
    public boolean internal();

    /**
     * @return {@code True} if transaction is a one-phase-commit transaction.
     */
    public boolean onePhaseCommit();

    /**
     * @return {@code True} if transaction has transform entries. This flag will be only set for local
     *      transactions.
     */
    public boolean hasTransforms();
}
