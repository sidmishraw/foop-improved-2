/**
 * Project: Foops_1
 * Package: foop.core
 * File: StateManager.java
 * 
 * @author sidmishraw
 *         Last modified: Sep 28, 2017 7:51:42 PM
 */
package foop.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import foop.utils.InstanceFactory;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * The manager is responsible for maintaing the object-state mappings, uses STM
 * to achieve this.
 * 
 * <br>
 * <br>
 * 
 * <p>
 * Extending the idea of `MemCell` assuming that the `MemCell` is the object
 * that we were continuously updating, we break it apart into 2 parts:
 * <ul>
 * <li>Immutable part -- Variable (Since `Object` is reserved by Java)
 * <li>Mutable part -- State
 * </ul>
 * 
 * and `associate` both parts with each other using a `<b>stateTable</b>`.
 * For now, let the `<i>Variable</i>` names be unique.
 * 
 * <br>
 * <br>
 * 
 * So, we have:
 * 
 * <br>
 * <br>
 * 
 * <code>
 * <b>Map&lt;String, Variable&gt; memory</b>
 * </code>
 * 
 * <br>
 * <br>
 * 
 * where <i>memory</i> is the collection of `MemCell`s.
 * 
 * <br>
 * <br>
 * 
 * Note: Need to hold the <i>Variable</i> objects some where else GC will
 * collect them xD
 * 
 * <br>
 * <br>
 * 
 * <code><b>Map&lt;String, State&gt; stateTable</b></code>
 * 
 * <br>
 * <br>
 * 
 * where <i>stateTable</i> `associates` <i>Variable</i> to <i>State</i> using
 * <i>Variable</i>'s `name` as key.
 * 
 * <br>
 * <br>
 * 
 * Now, since immutable part of `<i>MemCell</i>` represents the `<i>MemCell</i>`
 * itself, the <b>stm</b> becomes:
 * 
 * <br>
 * <br>
 * 
 * <code><b>Map&lt;String, Transaction&gt; stm</b></code>
 * 
 * <br>
 * <br>
 * 
 * The `<i>stm</i>` `associates` <i>Variable</i> to `<i>Transaction</i>` that
 * owns it during a particular time frame using the `<i>Variable</i>`'s `name`
 * as key.
 * 
 * <br>
 * <br>
 * 
 * So, `<b>effectively</b>` the collection of `MemCell`s is now represented by
 * the 2 Maps `stateTable` and `memory` (logically) and the `<i>stm</i>` still
 * represents the relation between the `<i>MemCell</i>`s and the
 * <i>Transaction</i>s.
 * 
 * <br>
 * <br>
 * 
 * The <i>stm</i>, <i>memory</i> and <i>stateTable</i> are managed by the
 * <i>StateManager</i>.
 * 
 * <br>
 * <br>
 * 
 * @author sidmishraw
 *
 *         Qualified Name: foop.core.StateManager
 */
public class StateManager {
    
    /** logging stuff **/
    private static final Logger               logger           = LoggerFactory.getLogger(StateManager.class);
    /** logging stuff **/
    
    // # Transactions Utility for making transctions
    private static volatile Transactions      ts               = InstanceFactory.getInstance(Transactions.class);
    // # Transactions Utility for making transctions
    
    // # For Locking and Synchronization
    /**
     * <p>
     * The <i>stateManagerLock</i> will be used for granular locking of
     * resources in methods that will be exposed to multiple threads.
     * 
     * <p>
     * This approach is being taken because ideally there will be one
     * StateManager. Even though the StateManager is using locks internally, it
     * is still hiding away this complexity from the user of the framework.
     */
    private volatile ReadWriteLock            stateManagerLock = null;
    // # For Locking and Synchronization
    
    /**
     * <p>
     * The `memory` is used to hold the `Variable` object references so that
     * they don't get GC'ed. Moreover, the memory represents part of the
     * `MemCell` collections.
     */
    private volatile Map<String, Variable>    memory;
    
    /**
     * <p>
     * The <i>stateTable</i> `associates` the immutable part of the `MemCell` to
     * its mutable part. It uses the <i>Variable</i>'s name as the key.
     */
    private volatile Map<String, State>       stateTable;
    
    /**
     * Represents the Software Transactional Memory(STM)
     * 
     * The `<i>stm</i>` `associates` <i>Variable</i> to `<i>Transaction</i>`
     * that owns it during a particular time frame using the `<i>Variable</i>`'s
     * `name` as key.
     */
    private volatile Map<String, Transaction> stm;
    
    /**
     * Initializes the StateManager with empty tables for each of the memory,
     * stm and stateTable.
     */
    public StateManager() {
        
        this.memory = new HashMap<>();
        this.stm = new HashMap<>();
        this.stateTable = new HashMap<>();
        
        // # locking and synchronization
        this.stateManagerLock = new ReentrantReadWriteLock();
        // # locking and synchronization
    }
    
    /**
     * <p>
     * Gets the copy of the state table, this snapshot acts as the old values.
     * 
     * @return The copy of state table is the old values
     */
    Map<String, State> getStateTable() {
        
        return new HashMap<>(this.stateTable);
    }
    
    /****** STM operations START ********/
    /**
     * <p>
     * Gets the owner of the `MemCell`,
     * 
     * @param variableName
     *            The name of the `<i>Variable</i>` or `MemCell` that you want
     *            to <b>own</b>.
     * 
     * @return an Optional Transaction, which may be empty if the `MemCell` is
     *         not owned by any <i>Transaction</i>.
     */
    @ReadLocked
    public Optional<Transaction> getOwner(String variableName) {
        
        Lock readLock = this.stateManagerLock.readLock();
        
        readLock.lock(); // get a read lock, since nothing is being modified, it
                         // is well and good
        
        Transaction owner = null;
        
        try {
            
            owner = this.stm.get(variableName);
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
        } finally {
            
            if (!Objects.isNull(readLock)) {
                
                readLock.unlock();
            }
        }
        
        return Optional.ofNullable(owner);
    }
    
    /**
     * <p>
     * Sets the owner of the `MemCell`,
     * 
     * @param variableName
     *            The name of the `<i>Variable</i>` or `MemCell` that you want
     *            to <b>own</b>.
     * 
     * @param owner
     *            The Transaction that now owns the `MemCell`.
     */
    @WriteLocked
    public void setOwner(String variableName, Transaction owner) {
        
        Lock writeLock = this.stateManagerLock.writeLock();
        
        writeLock.lock();
        
        try {
            
            this.stm.put(variableName, owner);
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
        } finally {
            
            if (!Objects.isNull(writeLock)) {
                
                writeLock.unlock();
            }
        }
    }
    
    /**
     * <p>
     * Removes the owner transaction reference for the `MemCell`
     * 
     * @param variableName
     *            The name of the `<i>Variable</i>` or `MemCell` that you want
     *            to free from ownership
     * 
     */
    @WriteLocked
    public void releaseOwnership(String variableName) {
        
        Lock writeLock = this.stateManagerLock.writeLock();
        
        writeLock.lock();
        
        try {
            
            this.stm.remove(variableName);
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
        } finally {
            
            if (!Objects.isNull(writeLock)) {
                
                writeLock.unlock();
            }
        }
    }
    /****** STM operations END ********/
    
    /****** Object - State, stateTable related START *******/
    /**
     * <p>
     * Makes you a brand new `<i>Variable</i>` or `MemCell` that is allocated in
     * the memory.(JK!)
     * 
     * @param variableName
     *            The name of the <i>Variable</i>
     * @param props
     *            The properties that can be present in the immutable part of
     *            the `MemCell` or <i>Variable</i>
     * 
     * @return The new <i>Variable</i>
     */
    @WriteLocked
    public Variable make(String variableName, @SuppressWarnings("unchecked") Map.Entry<String, Object>... props) {
        
        Lock writeLock = this.stateManagerLock.writeLock();
        
        writeLock.lock();
        
        Variable var = null;
        
        try {
            
            var = new Variable(variableName, props);
            
            // add the var to the memory
            this.memory.put(variableName, var);
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
        } finally {
            
            if (!Objects.isNull(writeLock)) {
                
                writeLock.unlock();
            }
        }
        
        return var;
    }
    
    /**
     * <p>
     * Package scope, not to be used lightly
     * Fetches the current state of the <i>Variable</i> or `MemCell`
     * 
     * @param variableName
     *            The name of the `<i>Variable</i>` whose current state is
     *            needed
     * 
     * @return The current state of the `<i>Variable</i>` which may be empty if
     *         the Variable never had any state, i.e The `MemCell` has not yet
     *         been initialized.
     */
    @ReadLocked
    Optional<State> read(String variableName) {
        
        Lock readLock = this.stateManagerLock.readLock();
        
        readLock.lock();
        
        State s = null;
        
        try {
            
            logger.info(String.format("Variable :: name: %s, has state: %s", variableName,
                    Optional.ofNullable(this.stateTable.get(variableName))));
            
            s = this.stateTable.get(variableName);
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
        } finally {
            
            if (!Objects.isNull(readLock)) {
                
                readLock.unlock();
            }
        }
        
        return Optional.ofNullable(s);
    }
    
    /**
     * <p>
     * Package scoped, not to be used lightly
     * Writes the new state of the <i>Variable</i>, updating its state in the
     * `stateTable`.
     * <br>
     * This action symbolizes that the `MemCell`'s contents were updated.
     * 
     * @param variableName
     *            The name of the `<i>Variable</i>` whose state needs to be
     *            updated
     * 
     * @param state
     *            The new state of the `<i>Variable</i>`. This symbolizes that
     *            the `MemCell`'s contents have been updated to this value since
     *            `<i>State</i>` represents the `mutable` part of the `MemCell`.
     */
    @WriteLocked
    void write(String variableName, State state) {
        
        Lock writeLock = this.stateManagerLock.writeLock();
        
        writeLock.lock();
        
        try {
            if (logger.isInfoEnabled()) {
                
                State oldState = Optional.ofNullable(this.stateTable.get(variableName)).orElse(null);
                
                logger.info(
                        String.format("Updating Variable :: name: %s with current state: %s", variableName, oldState));
            }
            
            this.stateTable.put(variableName, state);
            
            logger.info(String.format("Updated Variable :: name: %s to new state: %s", variableName, state));
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
        } finally {
            
            if (!Objects.isNull(writeLock)) {
                
                writeLock.unlock();
            }
        }
    }
    
    // # STM improvement
    /**
     * <p>
     * <b>Transaction Context</b>
     * Fetches the current state of the <i>Variable</i> or `MemCell`. Also, adds
     * the variable to the transaction's readSet.
     * 
     * @param variableName
     *            The name of the `<i>Variable</i>` whose current state is
     *            needed
     * 
     * @return The current state of the `<i>Variable</i>` which may be empty if
     *         the Variable never had any state, i.e The `MemCell` has not yet
     *         been initialized.
     */
    public Optional<State> readT(String variableName) throws Exception {
        
        Transaction t = null;
        
        if (Objects.isNull(t = ts.getT())) {
            // Objects.isNull(t = ts.getT()) is shorthand for (t = ts.getT();
            // Objects.isNull(t))
            
            throw new Exception("The `read` operation can only be used when within a Transaction context");
        }
        
        t.addReadSetMembers(variableName); // add variable to read-set of the
                                           // transaction t
        
        logger.info(String.format("Variable :: name: %s, has state: %s", variableName,
                Optional.ofNullable(this.stateTable.get(variableName))));
        
        return Optional.ofNullable(this.stateTable.get(variableName));
    }
    
    /**
     * <p>
     * <b>Transaction Context</b>
     * Writes the new state of the <i>Variable</i>, updating its state in the
     * `stateTable`. Also, adds the variable to the transaction's writeSet.
     * <br>
     * This action symbolizes that the `MemCell`'s contents were updated.
     * 
     * @param variableName
     *            The name of the `<i>Variable</i>` whose state needs to be
     *            updated
     * 
     * @param state
     *            The new state of the `<i>Variable</i>`. This symbolizes that
     *            the `MemCell`'s contents have been updated to this value since
     *            `<i>State</i>` represents the `mutable` part of the `MemCell`.
     */
    public void writeT(String variableName, State state) throws Exception {
        
        Transaction t = null;
        
        if (Objects.isNull(t = ts.getT())) {
            
            throw new Exception("The `write` operation can only be used when within a Transaction context");
        }
        
        t.addWriteSetMembers(variableName); // add to write-set
        
        if (logger.isInfoEnabled()) {
            
            State oldState = Optional.ofNullable(this.stateTable.get(variableName)).orElse(null);
            
            logger.info(String.format("Updating Variable :: name: %s with current state: %s", variableName, oldState));
        }
        
        this.stateTable.put(variableName, state);
        
        logger.info(String.format("Updated Variable :: name: %s to new state: %s", variableName, state));
    }
    // # STM improvement
    
    /****** Object - State, stateTable related END *******/
    
    // # STM improvement
    
    // # Transactions Utility
    /**
     * <p>
     * Provides utilities for creating <i>Transaction</i>s.
     * Needs to be initialized using the <i>InstanceFactory</i> just like the
     * <i>StateManager</i>
     * <br>
     * <br>
     * <b>Note: This is not thread safe for creating transactions</b>
     * 
     * @author sidmishraw
     *
     *         Qualified Name: foop.core.Transactions
     *
     */
    public static final class Transactions {
        
        /**
         * <p>
         * Just for the sake of simplicity, the transaction version is going to
         * be a
         * simple int that will get updated for each transaction we make.
         */
        private @Getter int                 tVersion = 0;
        
        /**
         * <p>
         * The static Transaction is going to be used to provide a non thread
         * safe
         * builder style of making transactions by method chaining
         */
        private @Getter @Setter Transaction t        = null;
        
        /**
         * <p>
         * Updates the static version counter for the transaction
         */
        public void updateVersion() {
            
            tVersion++;
        }
    }
    // # Transactions Utility
    
    // # Operations utility
    
    /**
     * <p>
     * The {@link TAction} is
     * 
     * @author sidmishraw
     *
     *         Qualified Name: foop.core.TAction
     *
     */
    public static enum TAction {
        
        DONE, FAIL;
    }
    
    /**
     * <p>
     * The operations in a transaction T.
     * 
     * @author sidmishraw
     *
     *         Qualified Name: foop.core.TOperation
     *
     */
    public static interface TOperation {
        
        /**
         * <p>
         * Performs the operation
         * 
         * @return a TAction, DONE if successful else FAIL
         */
        public TAction go();
    }
    // # Operations utility
    
    /**
     * <p>
     * Creates a new <i>Transaction</i> and sets the description of the
     * transaction, the operational logic and the reference to the
     * <i>StateManager</i> that is in charge of the world.
     * 
     * @param description
     *            The description of the transaction
     * 
     * @param manager
     *            The reference to the <i>StateManager</i> that is in charge
     *            of
     *            the world, i.e the stateTable, memory and stm.
     * 
     * @return The <i>StateManager</i> the state manager, used for method
     *         chaining
     */
    @WriteLocked
    public final StateManager newTransaction(String description) {
        
        this.stateManagerLock.writeLock().lock();
        
        try {
            
            Record record = new Record();
            record.setDescription(description);
            record.setVersion(ts.getTVersion());
            
            ts.updateVersion();
            
            ts.setT(new Transaction());
            
            ts.getT().setName(description);
            ts.getT().setRecord(record);
            ts.getT().setManager(this);
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
            
            if (!Objects.isNull(this.stateManagerLock.writeLock())) {
                
                this.stateManagerLock.writeLock().unlock();
            }
        }
        
        return this;
    }
    
    /**
     * <p>
     * Adds the operations to the transaction
     * 
     * @param operation
     *            The operation of the transaction
     * @return
     */
    @WriteLocked
    public final StateManager op(TOperation operation) {
        
        ts.getT().getOperations().add(operation);
        
        return this;
    }
    
    /**
     * <p>
     * The terminal method of the chaining, gives the constructed
     * transaction
     * 
     * @return The constructed transaction
     */
    @WriteLocked
    public final Transaction done() {
        
        Transaction t = null;
        
        try {
            
            t = ts.getT();
        } finally {
            
            this.stateManagerLock.writeLock().unlock();
        }
        
        return t;
    }
    // # STM improvement
}
