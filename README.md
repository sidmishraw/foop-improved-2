# STM with object - state separation

A improved version of FOOP, it has STM and state separation logic baked in.


There are 2 fundamental operations possible on a **`Memory Cell`**.

* Read(`readT`)

* Write(`writeT`)

The `StateManager` is responsible for handling all the boilerplate tasks for creating variables, reading from memory cells and writing to memory cells.

The StateManager is also responsible for creating you a transaction which then you can pass around and use accordingly.

The variables in `FOOP` are special in the sense that, they have their `State` separated from them. 

So, a bank account in `FOOP` will be represented as:

``` 
[AccountInfo] + [AccountBalance] 
```

where `AccountInfo` is the `Variable` and `AccountBalance` is its `State`.


> Note: The code snippet below uses [Project Lombok]() for boilerplate reduction.

In the following snippet, AccountBalance is defined as a State. 

```java
@EqualsAndHashCode(callSuper = false)
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
```

The snippet below shows the way to make variables in `FOOP`. The variables are made by the `StateManager`.

```java
// create the bank accounts to operate on
manager.make("Account1");
manager.make("Account2");
```

To add `State` to these newly created memory cells or `Variable`s, one must use the `writeT` method of the `StateManager`.

> Note: readT and writeT are to be used in a `Transaction` context. This means, they can only be used inside a transaction.

```java
manager.newTransaction(null)
	.op(
		() -> {
		    try {
		        // make initial states
		        manager.writeT("Account1", new AccountBalance(500.0F));
		        manager.writeT("Account2", new AccountBalance(1500.0F));
		    } catch (Exception e) {
		        logger.error(e.getMessage(), e);
		        return TAction.FAIL;
		    }
		    return TAction.DONE;
		}
	)
	.done()
	.execute();
```

To create a new transaction, one must use the builder pattern.

The pattern is as follows:

```java
Transaction t = manager.newTransaction("Transaction name")
					.op(operation1)
					.op(operation2)
					...
					.done();
```

The operations are chained and represent the sequential order they need to be executed in.

The `done()` is the terminal operation and returns the constructed transaction.

The transaction is executed by calling its `execute()`.


```java
manager.newTransaction("T1")
                .op(() -> deposit("Account1", 500.0F))
                .op(() -> withdraw("Account2", 500.0F))
                .done()
                .execute();
```

For chaining multiple transactions one can use:

```java
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
```

By using the latch, the main thread or the calling thread will wait till both the transctions are done processing.

> Note: The transctions themselves are actual threads and are processed concurrently.


Caveats:

* It is still boilerplate code heavy. (Might be because of Java)

* Needs better examples, worst case time complexity analysis.


`-Sid`


