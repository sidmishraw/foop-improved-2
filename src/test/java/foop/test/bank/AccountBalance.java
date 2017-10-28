/**
 * Project: Foops_1
 * Package: foop.test.bank
 * File: AccountBalance.java
 * 
 * @author sidmishraw
 *         Last modified: Oct 3, 2017 4:36:58 PM
 */
package foop.test.bank;

import foop.core.State;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author sidmishraw
 *
 *         Qualified Name: foop.test.bank.AccountBalance
 *
 */
@EqualsAndHashCode(
        callSuper = false)
@ToString
public class AccountBalance extends State {
    
    private @Getter float balance;
    
    /**
     * @param balance
     */
    public AccountBalance(float balance) {
        
        this.balance = balance;
    }
}
