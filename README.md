Poxy Git
========
The "poxy git" server is a simple HTTP-based Git server that supports
a variety of interesting configurations useful for integration
testing.  The proxy can support NTLM authentication emulating IIS
(with connection affinity) and NTLM authentication emulating Apache
(without connection affinity), and Basic authentication with a server
that doesn't support keep-alive.

This is not a general purpose Git server.

History
-------
The "poxy git" server is derived from the poxy proxy, a test proxy server.
The name derives from the term "pox"; an HTTP proxy is a pox upon your
network.

Copyright
---------
Copyright (c) Edward Thomson.  
Copyright (c) Microsoft Corporation.  All rights reserved.

Available under an MIT license.  Please see the included file `LICENSE`
for additional details.

