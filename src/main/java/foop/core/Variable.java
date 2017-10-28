/**
 * Project: Foops_1
 * Package: foop.core
 * File: Variable.java
 * 
 * @author sidmishraw
 *         Last modified: Sep 29, 2017 8:25:09 PM
 */
package foop.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;

/**
 * <p>
 * The `<i>Variable</i>`, represents the row identifier in the state table. This
 * is also the immutable part of the `MemCell`.
 * 
 * <br>
 * 
 * <h3>Core Idea:</h3>
 * <p>
 * Separate `Object` from its `State` and `associate` them with each other with
 * the help of a table like data structure called a `stateTable`.
 *
 * <br>
 * <br>
 * 
 * <code>
 *              Object := Immutable part called `Variable` since `Object` is reserved in Java
 *                      |   Mutable part called `State`
 * </code>
 * 
 * <p>
 * It is supposed to contain all the values that represent the <em> Object </em>
 * uniquely in the world. It is associated with its <em> State </em> which is
 * maintained in the StateTable.
 * 
 * <br>
 * <br>
 * <br>
 * 
 * @author sidmishraw
 *
 *         Qualified Name: foop.core.Variable
 *
 */
public class Variable {
    
    // TODO :: Implement a proper id for Variables -- later
    // not thought out completely yet, might add an identifier field
    // in the future when polishing the model
    // private int id;
    
    private @Getter String      name;
    
    private Map<String, Object> immutableProperties;
    
    /**
     * Makes a simple <i>Variable</i> that has no other properties except for
     * its
     * `name`.
     * 
     * @param name
     *            The name of the <i>Variable</i> or the immutable part of the
     *            `MemCell`
     */
    public Variable(String name) {
        
        this.name = name;
        this.immutableProperties = new HashMap<>();
    }
    
    /**
     * Makes the complex `<i>Variable</i>` with immutable Properties
     * 
     * @param name
     *            The name of the `<i>Variable</i>`
     * @param immutableProperties
     *            The properties of the <i>Variable</i> that can make into the
     *            immutable part of the `MemCell`
     */
    @SafeVarargs
    public Variable(String name, Map.Entry<String, Object>... immutableProperties) {
        
        this.name = name;
        this.immutableProperties = new HashMap<>();
        
        // add the properties into the properties of the Value
        Arrays.asList(immutableProperties)
                .forEach(prop -> this.immutableProperties.put(prop.getKey(), prop.getValue()));
    }
    
    /**
     * <p>
     * Gets you the value of the propery of the `Variable` if it exists else
     * gives you nothing.
     * 
     * @param propertyName
     *            The name of the property you want
     * 
     * @return An optional value which is empty if no value exists for the
     *         property name, else you get the value.
     */
    public Optional<Object> getValue(String propertyName) {
        
        return Optional.ofNullable(this.immutableProperties.get(propertyName));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Variable)) {
            return false;
        }
        Variable other = (Variable) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
