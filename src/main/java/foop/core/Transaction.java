/**
 * Project: Foops_1
 * Package: foop.core
 * File: Transaction.java
 * 
 * @author sidmishraw
 *         Last modified: Sep 28, 2017 7:59:04 PM
 */
package foop.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import foop.core.StateManager.TAction;
import foop.core.StateManager.TOperation;
import lombok.Getter;
import lombok.Setter;

/**
 * @author sidmishraw
 *
 *         Qualified Name: foop.core.Transaction
 *
 */
public class Transaction extends Thread {
    
    /*** Log and administrative stuff *****/
    private static final Logger            logger              = LoggerFactory.getLogger(Transaction.class);
    private static final long              MAX_SLEEP_WAIT_TIME = 1000;
    
    // for scheduling purposes, to make sure that the main thread
    // waits till the transaction is done processing!
    private @Getter @Setter CountDownLatch latch;
    /*** Log and administrative stuff *****/
    
    /**
     * <p>
     * record holds the metadata of the transaction
     */
    private @Getter @Setter Record         record;
    
    /**
     * <p>
     * The list of operations that need to be done when executing the
     * transaction t.
     */
    private @Getter List<TOperation>       operations          = new ArrayList<>();
    
    /**
     * <p>
     * <i>operation</i>: The functional interface that is used to define the
     * transaction's operational logic(execution logic).
     */
    private boolean operate() {
        
        // for each operation, do the operation
        for (TOperation to : this.getOperations()) {
            
            TAction act = to.go(); // execute the operation
            
            if (act.equals(TAction.FAIL)) {
                
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * <p>
     * The reference to the StateManager that takes care of global operations.
     */
    private @Setter StateManager manager;
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        
        logger.debug("Transaction:: " + this.getName() + " has started...");
        
        while (!this.record.getStatus()) {
            
            try {
                
                logger.debug(String.format("Initiating transaction:: %s", this.getName()));
                
                /**
                 * <p>
                 * Apply the transaction's operational logic to the writeSet and readSet members
                 * 
                 * <p>
                 * This newer version takes care of adding the members to the readSet and writeSet automatically.
                 * It also takes the backup of the variable states before modifying it in the State table.
                 * 
                 */
                Boolean operationStatus = this.operate();
                
                if (!operationStatus) {
                    
                    // failed to operate successfully, this transaction is
                    // flawed, bailing out
                    logger.error(String.format(
                            "CRITICAL:: Transaction:: %s has faulty operational logic, bailing out after rolling back",
                            this.getName()));
                    
                    this.rollback();
                    
                    this.record.setStatus(true);
                    
                    break;
                }
                
                logger.debug(String.format("Transaction:: %s operation completed, moving to commit changes...",
                        this.getName()));
                
                // commit changes
                Boolean commitStatus = this.commit();
                
                if (!commitStatus) {
                    
                    // failed to commit changes to the writeSet, hence rolling
                    // back
                    // and then retrying
                    logger.error(String.format(
                            "MODERATE:: Transaction:: %s couldn't commit its changes, rolling back and retrying...",
                            this.getName()));
                    
                    this.rollback();
                    
                    try {
                        
                        Thread.sleep(MAX_SLEEP_WAIT_TIME);
                    } catch (InterruptedException e) {
                        
                        logger.error(e.getMessage(), e);
                    }
                    
                    continue;
                }
                
                logger.debug(String.format(
                        "transaction:: %s has successfully committed its changes made to the writeSet members, marking transaction as completed.",
                        this.getName()));
                
                // since the commit was successful, the transaction releases all
                // its
                // writeSet members of its ownership and marks itself as
                // complete
                this.releaseOwnership();
                
                // marks itself as complete
                this.record.setStatus(true);
            } catch (Exception e) {
                
                logger.error(e.getMessage(), e);
            }
        }
        
        // countdown the latches to indicate that the transaction is done
        // processing
        if (!Objects.isNull(this.latch)) {
            
            this.latch.countDown();
        }
        
        logger.debug("Transaction:: " + this.getName() + " has ended...");
    }
    
    /**
     * <p>
     * Rolls back all changes made by the transaction and releases ownerships of
     * the writeSet members as well.
     */
    private void rollback() {
        
        logger.debug("Initiating rollback for transaction:: " + this.getName());
        
        Queue<String> writeSet = new LinkedBlockingQueue<>(this.record.getWriteSet());
        
        while (!writeSet.isEmpty()) {
            
            String variableName = writeSet.poll();
            
            // fetch the backup
            foop.core.State backup = this.record.getOldValues().get(variableName);
            
            // restore the backup
            this.manager.write(variableName, backup);
        }
        
        // release all the writeSet members from ownership
        this.releaseOwnership();
        
        logger.debug("Rollback complete for transaction:: " + this.getName());
    }
    
    /**
     * <p>
     * Commits the changes made by the transaction to its writeSet members after
     * referring to the state's of its readSet members.
     * 
     * @return true if commit was successful else returns false
     * @throws Exception
     */
    private Boolean commit() {
        
        logger.debug(String.format("Initiating commit for transaction:: %s", this.getName()));
        
        Boolean status = true;
        
        Queue<String> readSet = new LinkedBlockingQueue<>(this.record.getReadSet());
        
        while (!readSet.isEmpty()) {
            
            String variableName = readSet.poll();
            
            Optional<foop.core.State> currentState = this.manager.read(variableName);
            
            foop.core.State backup = this.record.getOldValues().get(variableName);
            
            if (currentState.isPresent()) {
                
                // there is some non-null state in the statetable.
                if (null == backup) {
                    
                    // this means that when the backup was taken, the readSet
                    // member was un-initiaized but now it has some non-null
                    // state this means that it has been modified in some way
                    // and this
                    // might not be good since the new state of the readSet
                    // member might cause some
                    // problem with states of the writeSet members.
                    
                    // commit failed
                    status = false;
                    break;
                } else if (!currentState.get().equals(backup)) {
                    
                    // backup is not null, now check if their values are equal
                    status = false;
                    break;
                } else {
                    
                    // backup of the readSet member matches its current state
                    status = true;
                }
            } else {
                
                // currentstate is empty or null, if old state was not null,
                // then there has been a change in state
                if (null == backup) {
                    
                    // means that both currentState of the readSet member and
                    // its backup were empty
                    status = true;
                } else {
                    
                    status = false;
                }
            }
        }
        
        logger.debug(String.format("Completing commit for transaction:: %s", this.getName()));
        
        return status;
    }
    
    /**
     * <p>
     * Releases the ownerships of all the writeSet member `MemCells`
     */
    private void releaseOwnership() {
        
        logger.debug(String.format("Initiating release of ownership of writeSet members of transaction:: %s",
                this.getName()));
        
        Queue<String> writeSet = new LinkedBlockingQueue<>(this.record.getWriteSet());
        
        while (!writeSet.isEmpty()) {
            
            String variableName = writeSet.poll();
            
            if (this.manager.getOwner(variableName).isPresent()
                    && this.manager.getOwner(variableName).get().equals(this)) {
                
                // release ownership only if this transaction owns it
                // this is to prevent race conditions(?)
                this.manager.releaseOwnership(variableName);
            }
        }
        
        logger.debug(
                String.format("Finished release of ownership of writeSet members of transaction:: %s", this.getName()));
    }
    
    /*** Book keeping methods **/
    
    /**
     * <p>
     * Adds the member <i>Variable</i> or `MemCell`s names to the writeSet of
     * the transaction.
     * 
     * @param variableNames
     *            The names of the `MemCell`s that this transaction intends to
     *            modify/write
     */
    public final void addWriteSetMembers(String... variableNames) {
        
        Set<String> writeSet = this.record.getWriteSet();
        
        for (String variableName : variableNames) {
            
            writeSet.add(variableName);
        }
    }
    
    /**
     * <p>
     * Adds the member <i>Variable</i> or `MemCell`s names to the `readSet` of
     * the transaction.
     * 
     * @param variableNames
     *            The names of the `MemCell`s that this transaction intends to
     *            read from.
     */
    public final void addReadSetMembers(String... variableNames) {
        
        Set<String> readSet = this.record.getReadSet();
        
        for (String variableName : variableNames) {
            
            // since the variables that are needed by the transaction in its
            // writeSet are going to be updated anyways
            // it would be a better idea to have them owned only once, hence the
            // variables that are already a part of the writeSet are not going
            // to be added to the readSet
            if (!this.record.getWriteSet().contains(variableName)) {
                
                readSet.add(variableName);
            }
        }
    }
    
    /*** Book keeping methods **/
    
    // # for executing the transaction
    /**
     * <p>
     * Executes the transaction, it takes the {@linkplain CountDownLatch} to
     * make the calling thread wait till this transaction is done executing.
     * 
     * @param latch
     *            The countdown latch
     */
    public void execute(CountDownLatch latch) {
        
        this.setLatch(latch);
        
        this.start();
        
        try {
            
            this.join();
        } catch (InterruptedException e) {
            
            logger.error(e.getMessage(), e);
        }
    }
    
    /**
     * <p>
     * The simple transaction execution logic without a countdown latch
     */
    public void execute() {
        
        this.start();
        
        try {
            
            this.join();
        } catch (InterruptedException e) {
            
            logger.error(e.getMessage(), e);
        }
    }
    // # for executing the transaction
}
