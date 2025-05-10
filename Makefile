# Project directories
SRC_DIR   := src
BIN_DIR   := bin
LIB_DIR   := lib

# JDBC driver version and path
JDBC_VERSION := 3.49.1.0
JDBC_JAR     := $(LIB_DIR)/sqlite-jdbc-$(JDBC_VERSION).jar

# Classpaths for compilation and execution
CLASSPATH := $(BIN_DIR):$(JDBC_JAR)

# Main classes
PKG_SERV  := server.Server
PKG_CLNT  := client.ClientGUI

# All Java source files
ALL_SRC   := $(shell find $(SRC_DIR) -name '*.java')

.PHONY: all compile server client clean

# Default target
all: compile

# Compile every .java at once, so inter-dependencies resolve
compile:
	@mkdir -p $(BIN_DIR)
	javac \
	  -cp "$(CLASSPATH)" \
	  -sourcepath $(SRC_DIR) \
	  -d $(BIN_DIR) \
	  $(ALL_SRC)

# Run the server (with JDBC driver on classpath)
server: compile
	@echo "Starting server..."
	java -cp "$(CLASSPATH)" $(PKG_SERV)

# Run one GUI client instance
client: compile
	@echo "Starting client..."
	java -cp "$(CLASSPATH)" $(PKG_CLNT)

# Clean up compiled classes
clean:
	rm -rf $(BIN_DIR)/*