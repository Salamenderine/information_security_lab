GOFILES=$(wildcard *.go)
BIN=firewall
INSTALL_BIN=/usr/local/bin/firewall

default: install

.PHONY: install

install: $(INSTALL_BIN)
$(INSTALL_BIN): build/$(BIN)
	sudo cp $< $@
	sudo setcap 'cap_net_admin=+ep' $@

build/$(BIN): $(GOFILES)
	go build -o $@ .

.PHONY: clean
clean:
	rm -rf build
