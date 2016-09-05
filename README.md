# RewardApp

## The Problem

A company is planning a way to reward customers for inviting their friends. They're planning a reward system that will give a customer points for each confirmed invitation they played a part into. The definition of a confirmed invitation is one where another invitation's invitee invited someone.

The inviter gets (1/2)^k points for each confirmed invitation, where k is the level of the invitation: 
level 0 (people directly invited) yields 1 point, 
level 1 (people invited by someone invited by the original customer) gives 1/2 points, 
level 2 invitations (people invited by someone on level 1) awards 1/4 points and so on. 
Only the first invitation counts: multiple invites sent to the same person don't produce any further points, even if they come from different inviters.

Also, to count as a valid invitation, the invited customer must have invited someone (so customers that didn't invite anyone don't count as points for the customer that invited them)

## The Solution

The problem establishes a well-defined hirearchy structure which can be stored in a very straightforward fashion.
In this solution, each customer stores:

1. an identification code (id)
2. a score
3. the identification code of the customer that invited him (let's define it as "father", for simplicity)
4. a flag marking if this customer have already invited someone else (let's define as a "valid customer")

Each customer can only award points to his father (grandfather, and so on) once. It is done in the moment the customer invites someone else for the first time, so that these points are not counted twice.

So, when processing an invitation, we check:
```
if it is the father first indication:
	if so, award its father (1/2)^k points (and its father, until the first customer)
	if not, don't
	either way, that customer is now valid.

After that, we check if the customer invited is new
	if so, we set the customer that just invited him as his father
	if not, we keep the former father
```
	
`k` is the value that represents the distance between the customers in tree, minus one. It starts at 0, as the father must gain 1 point for inviting a valid customer, and increases as the recursive step goes on.

## How to Build and Run

The system is a finatra server that can be built importing its build.sbt file by some IDE (It was developed in scala using IntelliJ IDEA Community Edition) or using sbt via command line.

After imported, the project structure should all be displayed: resources, configuration files, test files, scala classes and objects. The main program is RewardApp.scala and before running it, one should add the flag '-admin.port=:10000' at Program Arguments in Edit Configuration window. 
This configuration can be done via command line as well.

The server has the following http endpoints:

1. `/create`: create a database to store the customers
2. `/drop`: to drop the database erasing all its database
3. `/invite/x/y`: input and process a single invitation (x and y are both parameters, where x invites y)
4. `/scores`: list a json with the scores of all customers 
5. `/upload`: upload and process a file of indications

Endpoints 1-4 can be acessed using the cURL syntax: `curl -i http://localhost:8080/create` for example
Endpoint 5 can be acesses via: `curl --form "file=@filename.txt" http://localhost:8080/upload`