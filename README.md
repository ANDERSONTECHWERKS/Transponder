# Transponder
Generic Java networking subsystem, oriented around objectStream. Multithreaded.

*** ALL BEHAVIOR IS SUBJECT TO CHANGE ***
*** THIS README WILL BE UPDATED AS OFTEN AS POSSIBLE, BUT WHEN IN DOUBT: APPROACH WITH CAUTION! ***

Contains the following objects and behavior:
- TransponderTCP: a TCP Transponder object that wraps a single tServer and multiple tClient objects. 
    TransponderTCP objects have three modes: 1) Server 2) Client 3) Client-Server [Not yet implemented].

- tServer: A simple server that allows you to transmit Payload objects via an objectStream. 
    Transmits Payload objects automatically when clients connect to it. tServer implements Runnable.
    
- tClient: A simple client that connects to the assigned server and accepts the Payload object transferred to it
    by the tServer that it is connected to. tClient implements Runnable.
    
- Payload: The intended object to be transferred. The Payload class is, effectively, a placeholder for the moment.
    However, we will continue to maintain the Payload class, and likely utilize generics at some point in the future
    in order to generalize the types of data that can be sent via Transponder.

- menu: Console-based input system used to create ServerSockets, Sockets, configure the mode Transponder operates in,
    and acts as general interface to Transponder instances. (Until we write the GUI, of course!)
    
- debugObj: Object used for debugging the input / output of tServers and tClients.
    Intended to be passed into the Transponder object. 
