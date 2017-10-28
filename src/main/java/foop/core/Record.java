/**
 * Project: Foops_1
 * Package: foop.core
 * File: Record.java
 * 
 * @author sidmishraw
 *         Last modified: Sep 29, 2017 1:06:59 PM
 */
package foop.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * `Record` holds the metadata for a transaction.
 * 
 * <p>
 * Contents of record are as follows:
 * <ul>
 * <li><b> status </b>: The status of the transaction.
 * 
 * <li><b> version </b>: The version counter that reflects the starting order of
 * the transaction.
 * 
 * <li><b> description </b>: The description of the transaction.
 * 
 * <li><b>writeSet</b>: The set of MemCells/addresses that the transaction
 * intends to write to.
 * 
 * <li><b> readSet </b>: The set of MemCells/addresses that the transaction
 * intends to read from.
 * 
 * <li><b> oldValues </b>: The set of oldValues of the MemCells/addresses
 * that acts as a backup incase the transaction fails to commit.
 * 
 * <br>
 * <br>
 * Uses Project Lombok for boilerplate generation
 * 
 * @author sidmishraw
 *
 *         Qualified Name: foop.core.Record
 *
 */
public class Record {
    
    /**
     * <p>
     * The status of the Transaction, false means that it has not completed,
     * else its true
     */
    private @Getter @Setter Boolean            status = false;
    private @Getter @Setter Integer            version;
    private @Getter @Setter String             description;
    
    /**
     * <p>
     * The <i>writeSet</i> contains all the names of the <i>Variable</i>s or
     * `MemCell`s that the transaction needs to access(take ownership) inorder
     * to modify.
     */
    private @Getter Set<String>                writeSet;
    
    /**
     * <p>
     * The <i>readSet</i> contains all the names of the <i>Variable</i>s or
     * `MemCell`s that the transaction needs to access in order to read the
     * values from.
     */
    private @Getter Set<String>                readSet;
    
    /**
     * <p>
     * The <i>oldValues</i> contains the backup of the state of the
     * <i>Variable</i>s or `MemCell`s so that it can restore them incase it
     * fails to commit.
     */
    private @Getter @Setter Map<String, State> oldValues;
    
    /**
     * 
     */
    public Record() {
        
        this.status = false;
        this.writeSet = new HashSet<>();
        this.readSet = new HashSet<>();
    }
}
