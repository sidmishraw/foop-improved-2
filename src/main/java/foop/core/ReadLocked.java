/**
 * Project: foop-improved-2
 * Package: foop.core
 * File: ReadLocked.java
 * 
 * @author sidmishraw
 *         Last modified: Oct 27, 2017 9:12:41 PM
 */
package foop.core;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

@Documented
@Target(METHOD)
/**
 * <p>
 * This annotation is used as a marker to denote that the method uses ReadLocks
 * inside.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: foop.core.ReadLocked
 *
 */
public @interface ReadLocked {
    
}
