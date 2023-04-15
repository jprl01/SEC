#HDS Ledger

##HDS Project - Stage 2

### Authors: *Group 27*
* [Catarina Beirolas] - 93034
* [João Luı́s] - 95607


###Instructions

**1) In order to compile and run this project, you must have JDK installed in your machine.**

```
 javac *.java src/pt/ulisboa/tecnico/meic/sirs/*.java        [Compiles all the needed classes]        
```

**2) Explaining Our tests**

In order to guarantee the required security and design specifications, we designed a set of demo tests that consist of running 4 servers and
4 clients, here we'll explain in depth how each one works, for running commands check the next section.

Byzantine Server send in the block a request signed by them instead of the Client

For this test, the leader server will be byzantine: when it receives a request from a client, it signs it, so when the other servers receive the pre-prepare message, they verify the request was not signed by the client and so they let the client know their request is not going to be processed.

Byzantine Server sends a wrong value for a Strong Check Balance

For this test, the one Byzantine Server always replied with a fixed (and wrong) value when a client makes a Strong Check Balance request. If the Client receives the three correct (and equal) values first, they'll accept that value. Instead, if the value sent by the Byzantine Server is not the last, the Client will request a second phase and the request will be processed in a block.

**3) Running the tests**

For testing we developed 2 PuppetMasters, what they do is open 8 separate windows command lines, 4 of them running servers and the other four running the Clients, if you're on windows you can check how to run the Puppet Masters on 3.1) and if not, follow the instructions in 3.2).

In order to test critical cases we implemented variations of the clients and servers that are identical, adding/changing only snippets of code to verify the tests described more in depth in our report.


- We also recorded videos running the regular case and the tests, which we uploaded on this Google Drive folder:
  [Link](https://drive.google.com/drive/folders/18De8T_xisLuJnLJUp-fBcpxh8Z6n85T9?usp=share_link)



**3.1) Commands with PupperMasters(Windows)**


The Correct Functionality and main one being "PuppetMaster.java" can be ran after compilling with:
```
java PuppetMaster
```

The other PuppetMasters can all be ran in a similar way, each having their own PuppetMaster Class.

```
Fake Sign Byzantine Server Attack - java FakeSignByzantinePuppetMaster

Strong Check Balance Byzantine Server Attack - java StrongCheckBalanceByzantinePuppetMaster

```

**3.2) Commands (Linux)**


For every case you'll have to open 4 terminals for the servers and 4 for the client.


A regular Server running command is like this:
```
java Server <Nr of Ports> <self port> [list of all ports(including self)] <Client Name>


Suggestion:
java Server 4 1234 1234 1235 1236 1237 Joao 
java Server 4 1235 1234 1235 1236 1237 Joao
(etc..)
```


A regular Client can be ran with the following command:
```
java Client <Client Name> <Nr of Ports> [list of all ports] <Client Name>

Suggestion:
java Client Joao 4 1234 1235 1236 1237 Joao
```
#### Note:
***The keys used for the cryptographic functions have been previously generated to simplify testing.***
They follow the naming convention: \<Client/ServerName\> + "Priv"/"Pub" + ".Key"
- Keys already generated for Servers:
  1234Priv.Key, 1234Pub.key, 1235Priv.Key, 1235Pub.key, 1236Priv.Key, 1236Pub.key, 1237Priv.Key, 1237Pub.key
- Keys already generated for Client:
  JoaoPriv.key, JoaoPub.key, CatarinaPriv.key, CatarinaPub.key, JoaquimPriv.key, JoaquimPub.key, ManuelPriv.key, ManuelPub.key, 
- Keys generated to test fake signing:
  pub.key, priv.key

**4) Delete all the generated .class files**
```
 rm *.class src/pt/ulisboa/tecnico/meic/sirs/*.class 
```
