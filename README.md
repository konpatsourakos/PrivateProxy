# PrivateProxy
NOTE: this code is for demonstrative purposes. To productionize this, we should use a framework such as NETTY to take advantage of high performance network interactions.


## High level designs:

A client talks to the proxy server using tls. Sending a message as such as defined by [1]:

CONNECT DOMAIN:PORT

Then the server validates the request and either rejects the request (401) or accepts it with a `200 OK`.
If the server accepts the request, the client has a tunnel connection to the server. At this point it can initiate a
tls request and establish a secure anonymous connection that the proxy server cannot decipher.


## Performance
There are a lot of inefficiencies on this code and there is need for testing and benchmarking. A few questions that need
further investigation are:
- How well it scales with large number of clients
- How much garbage is created on a single connection.

We need to measure actual customer traffic to improve performance still, here are some reasonable assumptions are:
- Each customer will perform a small number of queries and close the connection
- There will be heavy hitters, we could cache the authentication of those users


## Testing
There has been minimal testing of up to 2 customers and some minimal unit tests. We should add integration testing
for E2E testing and expand unit test to capture more edge cases.

[1] Luotonen, A., "Tunneling TCP based protocols through Web proxy servers," Work in Progress. [jg647]
