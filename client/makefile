CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -pthread

all:
	mkdir bin
	g++ $(CFLAGS) -o bin/echoClient.o src/echoClient.cpp $(LDFLAGS)
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp $(LDFLAGS)
	g++ -o bin/BGSClient bin/connectionHandler.o bin/echoClient.o $(LDFLAGS)

	
.PHONY: clean
clean:
	rm -rf bin
