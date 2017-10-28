/**
 * Project: Foops_1
 * Package: foop.utils
 * File: Counter.java
 * 
 * @author sidmishraw
 *         Last modified: Sep 29, 2017 1:52:22 PM
 */
package foop.utils;

/**
 * <p>
 * Counter utility
 * 
 * @author sidmishraw
 *
 *         Qualified Name: foop.utils.Counter
 *
 */
public class Counter {
    
    /**
     * The value of the counter
     */
    private Integer value;
    
    /**
     * @param value
     *            the initial value of the counter
     */
    public Counter(Integer value) {
        
        this.value = value;
    }
    
    /**
     * <p>
     * Increments the value of the counter
     */
    public void increment() {
        
        this.value++;
    }
    
    /**
     * <p>
     * Decrements the value of the counter
     */
    public void decrement() {
        
        this.value--;
    }
    
    /**
     * <p>
     * Gets the current value of the counter
     * 
     * @return the current value of the counter
     */
    public Integer getValue() {
        
        return value;
    }
}
