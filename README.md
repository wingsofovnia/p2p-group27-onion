# Onion Forwarding Module

The Onion Forwarding module is concerned with handling the API connections and P2P connections. It forwards data between API connections and onion tunnels.

## Onion Forward In-Memory Example
There is an example of Netty-based Onion Forward implementation which you can run locally to test data forwarding without required remote Onion Authorizer and RPS. All onion are run locally with in-memory Onion Authorizer and RPS fakes.
```
# Compile
./gradlew clean jar

# Run example
java -jar samples/build/libs/onion-forwarding-samples-0.1-SNAPSHOT.jar ${NUMBER_OF_ONIONS_>3}

# Example output:
$ java -jar samples/build/libs/onion-forwarding-samples-0.1-SNAPSHOT.jar 3
Number of local onions = 3, intermediate hops = 1
Random local peers = localhost/127.0.0.1:16453, localhost/127.0.0.1:6247, localhost/127.0.0.1:39516
Please select origin onion to build a tunnel
0) localhost/127.0.0.1:16453
1) localhost/127.0.0.1:6247
2) localhost/127.0.0.1:39516
Type: 0
Please select destination onion to build a tunnel
1) localhost/127.0.0.1:6247
2) localhost/127.0.0.1:39516
Type: 2
Building the tunnel ...
Listener: Onion (localhost/127.0.0.1:39516) incoming tunnel #909156460
Tunnel #909156460 has been successfully built

Feel free to type any string to forward it from the (localhost/127.0.0.1:16453) -> (localhost/127.0.0.1:39516). Type 'exit' to exit.
> test
Sending "test" from (localhost/127.0.0.1:16453) to (localhost/127.0.0.1:39516) via tunnel #909156460
Listener: Datum "test" arrived to (localhost/127.0.0.1:39516) via tunnel #909156460
> exit
Closing onions ...
$ ...
```

## Authors

The Team \#24
 - **Eiler Poulsen** (0369a2108), Informatics (M.Sc.);
 - **Illia Ovchynnikov** (03692268), Informatics (M.Sc.);

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
