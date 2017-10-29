/**
 * Project: Foops_1
 * Package: foop.test.bank
 * File: BankDriver.java
 * 
 * @author sidmishraw
 *         Last modified: Oct 3, 2017 4:35:05 PM
 */
package foop.test.bank;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import foop.core.StateManager;
import foop.core.StateManager.TAction;
import foop.utils.InstanceFactory;

/**
 * @author sidmishraw
 *
 *         Qualified Name: foop.test.bank.BankDriver
 *
 */
public class BankDriver {
    
    private static final Logger logger  = LoggerFactory.getLogger(BankDriver.class);
    
    private static StateManager manager = InstanceFactory.getInstance(StateManager.class);
    
    /**
     * <p>
     * A probable method for taking care of bank account operations
     * Deposits the amount into the bank account
     * 
     * @param account
     *            The bank account name
     * @param amount
     *            The amount to be deposited
     */
    private static TAction deposit(String account, float amount) {
        
        try {
            
            logger.info(String.format("Depositing amount:: %f into bank account:: %s", amount, account));
            
            AccountBalance b = manager.readT(account).isPresent() ? (AccountBalance) manager.readT(account).get()
                    : new AccountBalance(0);
            
            AccountBalance bnew = new AccountBalance(b.getBalance() + amount);
            
            manager.writeT(account, bnew);
            
            logger.info(String.format("Deposited amount:: %f into bank account:: %s, new amount:: %f", amount, account,
                    ((AccountBalance) manager.readT(account).get()).getBalance()));
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
            
            return TAction.FAIL;
        }
        
        return TAction.DONE;
    }
    
    /**
     * <p>
     * Another probable method for taking care of bank account operations
     * Withdraws the specified amount from bank accounts
     * 
     * @param account
     *            The bank account name
     * 
     * @param amount
     *            The amount of money to be withdrawn
     */
    private static TAction withdraw(String account, float amount) {
        
        try {
            
            logger.info(String.format("Withdrawing amount:: %f into bank account:: %s", amount, account));
            
            AccountBalance b = manager.readT(account).isPresent() ? (AccountBalance) manager.readT(account).get()
                    : new AccountBalance(0);
            
            AccountBalance bnew = new AccountBalance(b.getBalance() - amount);
            
            manager.writeT(account, bnew);
            
            logger.info(String.format("Withdrew amount:: %f into bank account:: %s, new amount:: %f", amount, account,
                    ((AccountBalance) manager.readT(account).get()).getBalance()));
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
            
            return TAction.FAIL;
        }
        
        return TAction.DONE;
    }
    
    /**
     * Initializer of the simulation
     */
    @SuppressWarnings("unchecked")
    private static void setupBankAccounts12() throws Exception {
        
        // create the bank accounts to operate on
        manager.make("Account1");
        manager.make("Account2");
        
        manager.newTransaction(null).op(() -> {
            
            try {
                
                // make initial states
                manager.writeT("Account1", new AccountBalance(500.0F));
                manager.writeT("Account2", new AccountBalance(1500.0F));
            } catch (Exception e) {
                
                logger.error(e.getMessage(), e);
                return TAction.FAIL;
            }
            
            return TAction.DONE;
        }).done().execute();
    }
    
    /**
     * <p>
     * Testing out the FOOP using a single thread
     */
    @Test
    public void testDrive1() throws Exception {
        
        setupBankAccounts12();
        
        logMsg("HARMLESS:: Initially");
        
        manager.newTransaction("T1")
                .op(() -> deposit("Account1", 500.0F))
                .op(() -> withdraw("Account2", 500.0F))
                .done()
                .execute();
        
        logMsg("HARMLESS:: Finally");
    }
    
    /**
     * <p>
     * `logAfterState` logging after state
     */
    private static void logMsg(String msg) {
        
        // logger -- after state
        manager.newTransaction("LoggerTransaction").op(() -> {
            
            try {
                
                logger.error(String.format(msg + " :: Acc1:: %f, Acc2:: %f",
                        ((AccountBalance) manager.readT("Account1").get()).getBalance(),
                        ((AccountBalance) manager.readT("Account2").get()).getBalance()));
            } catch (Exception e) {
                
                logger.error(e.getMessage(), e);
                return TAction.FAIL;
            }
            
            return TAction.DONE;
        }).done().execute();
    }
    
    /**
     * <p>
     * Test drive 2: This time we have 2 transactions that will be taking part
     * in the workflow.
     * T1 :: withdraw 500 from Account2 and deposit into Account1
     * T2 :: withdraw 100 from Account1 and deposit into Account2
     * 
     * Since both these transactions are conflicing in nature, it will be a good
     * show!!!
     */
    @Test
    public void testDrive2() {
        
        try {
            
            logger.info(String.format("Initiating test driver 2..."));
            
            // set up the bank accounts Account1 and Account2
            setupBankAccounts12();
            
            // logger -- initial state
            logMsg("HARMLESS:: Initially");
            
            CountDownLatch latch = new CountDownLatch(2);
            
            manager.newTransaction("T1")
                    .op(() -> deposit("Account1", 500F))
                    .op(() -> withdraw("Account2", 500F))
                    .done()
                    .execute(latch);
            
            manager.newTransaction("T2")
                    .op(() -> deposit("Account2", 100F))
                    .op(() -> withdraw("Account1", 100F))
                    .done()
                    .execute(latch);
            
            try {
                
                // wait till all the transactions are done
                latch.await();
            } catch (InterruptedException e) {
                
                logger.error(e.getMessage(), e);
            }
            
            // logger -- after state
            logMsg("HARMLESS:: Finally");
            
            logger.info(String.format("Finishing up test driver 2..."));
        } catch (Exception e) {
            
            logger.error(e.getMessage(), e);
        }
    }
}
