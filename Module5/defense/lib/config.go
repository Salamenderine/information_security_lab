package lib

import (
	"io/ioutil"
	"log"
	"net"

	"gopkg.in/yaml.v2"
)

type customerCfg struct {
	IP net.IP `yaml:"customer_ip"`
}

func getCustomerIP() net.IP {
	var cfg *customerCfg
	file, err := ioutil.ReadFile("/etc/isl/IPs.yml")
	if err != nil {
		log.Fatal("Error reading customer server IP.", err)
	}
	err = yaml.Unmarshal(file, &cfg)
	if err != nil {
		log.Fatal("Error decoding customer server IP.", err)
	}
	return cfg.IP
}
