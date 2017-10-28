/**
 * Project: foop-improved-2
 * Package: foop.core
 * File: WriteLocked.java
 * 
 * @author sidmishraw
 *         Last modified: Oct 27, 2017 9:10:41 PM
 */
package foop.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
/**
 * <p>
 * This annotation means that the method uses Write Lock within it for
 * synchronization purposes.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: foop.core.WriteLocked
 *
 */
public @interface WriteLocked {
    
}
