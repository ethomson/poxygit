Poxy Git
========
The "poxy git" server is a simple HTTP-based Git server that supports
a variety of interesting configurations useful for integration
testing.

## Authentication

This server can support NTLM authentication emulating IIS
(with connection affinity) and NTLM authentication emulating Apache
(without connection affinity), and Basic authentication with a server
that doesn't support keep-alive.

## Redirects

The server can redirect at the beginning of a request (the `info/refs`
stage) or subsequently (during the actual push/fetch). Generally, git
clients will allow the _initial_ redirect and reject the _subsequent_
redirects.

## Keep-alive

The server can close keep-alive connections after the first request
to ensure that clients successfully reconnect and send the next
response on a new socket.

## Speed

The server can throttle the speed to a variety of speeds for testing
timeouts and low-bandwidth connections.

This is not a general purpose Git server.

History
-------
The "poxy git" server is derived from the poxy proxy, a test proxy
server developed for Microsoft Team Explorer Everywhere.  The name
derives from the term "pox"; an HTTP proxy is a pox upon your
network.

Copyright
---------
Copyright (c) Edward Thomson.  
Copyright (c) Microsoft Corporation.  All rights reserved.

Available under an MIT license.  Please see the included file `LICENSE`
for additional details.

