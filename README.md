#HDS Ledger

##HDS Project - Stage 1

### Authors: *Group 27*
* [David Cruz] - 89377
* [Catarina Beirolas] - 93034
* [João Luı́s] - 95607


###Instructions

**1) In order to compile and run this project, you must have JDK installed in your machine.**

```
 javac *.java src/pt/ulisboa/tecnico/meic/sirs/*.java        [Compiles all the needed classes]        
```

**2) Explaining Our tests**

In order to guarantee the required security and design specifications, we designed a set of 5 demo tests that consist of running 4 servers and
1 client, here we'll explain in depth how each one works, for running commands check the next section.
**- Correct Functionality(PuppetMaster):**
* A regular client
* Four regular servers

**- Byzantine Server Attack(ByzantinePuppetMaster):**
* A regular Client;
* Three regular Servers;
* A byzantine Server.
  The byzantine Server always proposes the string "byzantine" in the PREPARE broadcast, no matter what value the Client sent to be added to the blockchain. However, because of the IBFT consensus protocol implemented, a quorum of Servers still decides the correct value (the one proposed by the Client);

**- Duplicate Message Attack(DuplicateMessagePuppetMaster):**
* A byzantine Client that duplicates the messages sent to a specific server;
* Four regular Servers;

The Servers detect the message is a duplicate because they have already seen the messageId and nounce sent in the second message.

**- Authentication Attack(FakeSignPuppetMaster):**
* A byzantine Client that signs the message with the wrong private key;
* Four regular Servers;

The Servers detect Client is not who they say they are (the private key with which the Client signed the message is not theirs), because they can't verify the signature with the Client's public key.

**- Correct Order Message Processing(CorrectOrderPuppetMaster):**
* A Client which sends messages with unordered ids;
* 4 regular Servers.
  All the Servers process the different strings sent by the Client in the same order. The Client first sends a message with id 2 and then other with id 1, but all Servers add the message with id 1 to the blockchain before adding the message with id 2.

**- Message Waiting in Queue(PendingCommandsPuppetMaster):**
* A regular Client;
* Four regular Servers.
  The Client sends multiple messages to the Servers in little time, but all Servers only add each string to the blockchain once and the strings not yet processed are added to a queue. Servers, before starting processing a string, show in the terminal which strings are in the queue.

**3) Running the tests**

For testing we developed 6 PuppetMasters, what they do is open 5 separate windows command lines, 4 of them running servers and the other one running the Client, if you're on windows you can check how to run the Puppet Masters on 3.1) and if not, follow the instructions in 3.2).

In order to test critical cases we implemented variations of the client and server that are identical, adding/changing only snippets of code to verify the tests described more in depth in our report.


- We also recorded 6 videos running the regular case and the 5 tests, which we uploaded on this Google Drive folder:
  [Link](https://drive.google.com/drive/folders/1iY1liEoRPWTBK1gv5Wx32qDwiy3i2pS0)



**3.1) Commands with PupperMasters(Windows)**


The Correct Functionality and main one being "PuppetMaster.java" can be ran after compilling with:
```
java PuppetMaster
```

The other PuppetMasters can all be ran in a similar way, each having their own PuppetMaster Class.

```
Byzantine Server Attack - java ByzantinePuppetMaster
Duplicate Messenge Attack - java DuplicateMessagePuppetMaster
Authentication Attack - java FakeSignPuppetMaster
Correct Order Message - java CorrectOrderPuppetMaster
Message Waiting - java PendingCommandsPuppetMaster
```

**3.2) Commands (Linux)**


For every case you'll have to open 4 terminals for the servers and 1 for the client.


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

For the critical cases one of the terminals shall be running a different implementation of the client/server, they all have the same arguments for running as the regular ones, so change only the class name in the command.
The respective classes are:

Byzantine Server Attack - run one regular client, three regular servers and one Byzantine server:
```
java Server 4 1234 1234 1235 1236 1237 Joao
java Server 4 1235 1234 1235 1236 1237 Joao 
java Server 4 1236 1234 1235 1236 1237 Joao 
java ByzantineServer 4 1237 1234 1235 1236 1237 Joao 
java Client Joao 4 1234 1235 1236 1237 Joao
```

Duplicate Messenge Attack - run Duplicate Message Client and 4 regular servers:
```
java DuplicateMessageClient Joao 4 1234 1235 1236 1237 Joao
```
Authentication Attack - run a Fake Sign Client Client and 4 regular servers:
```
java FakeSignClient Joao 4 1234 1235 1236 1237 Joao
```

Correct Order Message - run a Correct Order Client and 4 regular servers:
```
java CorrectOrderClient Joao 4 1234 1235 1236 1237 Joao
```

Message Waiting - run a regular client and 4 Pending Commands servers:
```
java PendingCommandsServer 4 1234 1234 1235 1236 1237 Joao 
java PendingCommandsServer 4 1235 1234 1235 1236 1237 Joao
(etc..)
```

---
#### Note:
***The keys used for the cryptographic functions have been previously generated to simplify testing.***
They follow the naming convention: \<Client/ServerName\> + "Priv"/"Pub" + ".Key"
- Keys already generated for Servers:
  1234Priv.Key, 1234Pub.key, 1235Priv.Key, 1235Pub.key, 1236Priv.Key, 1236Pub.key, 1237Priv.Key, 1237Pub.key
- Keys already generated for Client:
  JoaoPriv.key, JoaoPub.key
- Keys generated to test fake signing:
  pub.key, priv.key

**4) Delete all the generated .class files**
```
 rm *.class src/pt/ulisboa/tecnico/meic/sirs/*.class 
```
