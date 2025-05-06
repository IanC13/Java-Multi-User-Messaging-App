SRC_DIR   := src
BIN_DIR   := bin
JAVA      := javac
JAVAC     := $(JAVA)
JAVA_RUN  := java
PKG_SERV  := server.Server
PKG_CLNT  := client.ClientGUI
ALL_SRC   := $(shell find $(SRC_DIR) -name '*.java')
ALL_CLASS := $(patsubst $(SRC_DIR)/%.java,$(BIN_DIR)/%.class,$(ALL_SRC))

.PHONY: all compile server client clean

all: compile

compile: $(ALL_CLASS)

# path
$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	@mkdir -p $(dir $@)
	$(JAVAC) -d $(BIN_DIR) $<

# Run server
server: compile
	$(JAVA_RUN) -cp $(BIN_DIR) $(PKG_SERV)

# Run one client instance
client: compile
	$(JAVA_RUN) -cp $(BIN_DIR) $(PKG_CLNT)

# Remove all compiled classes
clean:
	rm -rf $(BIN_DIR)/*