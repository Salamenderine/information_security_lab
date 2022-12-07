package client

import (
	"bufio"
	"encoding/json"
	"io/ioutil"
	"log"
	"net"
	"os"
	"strconv"
	"strings"
	"time"

	"gopkg.in/yaml.v2"
)

// Use these helpers in your solution

const (
	DynamicConfigsFile = "IPs.yml"
	StaticConfigFile   = "static_configs.yml"
	ConfigPath         = "/etc/isl"
	DynamicConfigs     = ConfigPath + "/" + DynamicConfigsFile
	StaticConfigs      = ConfigPath + "/" + StaticConfigFile
)

// static
type ClientConstants struct {
	// Attack task
	AttackSeconds int    `yaml:"attack_seconds"`
	VictimScionIA string `yaml:"victim_scion_IA"`
	VictimIP      string `yaml:"victim_IP"`
	// SCION installation
	DispatcherPort          string `yaml:"scion_dispatcher_port"`
	DispatcherConfig        string `yaml:"default_dispatcher_config"`
	DefaultDispatcherSocket string `yaml:"default_dispatcher_socket"`
	ScionTopology           string `yaml:"scion_topology"`
	SciondConfig            string `yaml:"sciond_config"`
}

// dynamic
type StudentConstants struct {
	VictimPort    int    `yaml:"victim_port"`
	LocalVictimIP string `yaml:"local_victim_ip"`
	MeowIP        string `yaml:"server_bridge_ip"`
}

var clientConstants ClientConstants
var studentConstants StudentConstants

// this function will run once when you import the client package,
// and initialize the clientConstants & studentConstants variables
// from the configuration on your VM
func init() {

	if err := os.Chdir(ConfigPath); err != nil {
		log.Fatalf("Changing to configuration directory didn't work: %v", err)
	}
	dynamicConfigs, err := ioutil.ReadFile(DynamicConfigs)
	if err != nil {
		log.Fatalf("Error while reading dynamic configuration: %v", err)
	}
	staticConfigs, err := ioutil.ReadFile(StaticConfigs)
	if err != nil {
		log.Fatalf("Error while reading static configuration: %v", err)
	}
	err = yaml.Unmarshal(dynamicConfigs, &studentConstants)
	if err != nil {
		log.Fatalf("Error while unmarshalling the dynamic configuration: %v", err)
	}
	err = yaml.Unmarshal(staticConfigs, &clientConstants)
	if err != nil {
		log.Fatalf("Error while unmarshalling the static configuration: %v", err)
	}
}

/* MeowServerIP returns the student-specific IP address of the meow server */
func MeowServerIP() net.IP {
	return net.ParseIP(studentConstants.MeowIP)
}

/* LocalVictimIP loads the local victim IP defined in IPs.yml.
 */
func LocalVictimIP() net.IP {
	return net.ParseIP(studentConstants.LocalVictimIP)
}

/*
Returns the remote victim IP
*/
func RemoteVictimIP() net.IP {
	return net.ParseIP(clientConstants.VictimIP)
}

/* VictimPort loads the port defined in IP.yml. The remote victim will multiplex on these ports
when reporting the attack volume back to you.
*/
func VictimPort() int {
	return studentConstants.VictimPort
}

func AttackDuration() time.Duration {
	return time.Second * time.Duration(clientConstants.AttackSeconds)
}

func AttackSeconds() int {
	return clientConstants.AttackSeconds
}

func DispatcherPort() int {
	port, err := strconv.Atoi(clientConstants.DispatcherPort)
	if err != nil {
		log.Fatal("Error while converting the dispatcher port to an int:", err)
	}
	return port
}

func VictimScionIA() string {
	return clientConstants.VictimScionIA
}

/* DispatcherSocket finds the path of the dispatcher socket from the config file.
 */
func DispatcherSocket() (string, error) {
	// Automatically opens the correct namespace configuration depending on where it is run
	file, err := os.Open(clientConstants.DispatcherConfig)
	if err != nil {
		return "", err
	}
	// Read file line by line
	scanner := bufio.NewScanner(file)
	scanner.Split(bufio.ScanLines)
	var socket = ""
	for scanner.Scan() {
		// Find Socket
		line := scanner.Text()
		if strings.Contains(line, "application_socket") {
			socket_start_idx := strings.Index(line, `"`) + 1
			socket = line[socket_start_idx : len(line)-1]
		}
	}
	// If no socket was specified in the config, return default socket
	if socket == "" {
		socket = clientConstants.DefaultDispatcherSocket
	}
	return socket, nil
}

/* SCIONDAddress finds the address of the scion daemon from the config file
 */
func SCIONDAddress() string {
	file, err := os.Open(clientConstants.SciondConfig)
	if err != nil {
		log.Fatal("Error while opening the SciondConfig: ", err)
	}
	// Read file line by line
	scanner := bufio.NewScanner(file)
	scanner.Split(bufio.ScanLines)
	var addr = ""
	for scanner.Scan() {
		// Find addr
		line := scanner.Text()
		if strings.Contains(line, "address") {
			addr_start_idx := strings.Index(line, `"`) + 1
			addr = line[addr_start_idx : len(line)-1]
		}
	}
	return addr
}

/* Extracts the Isolation Domain (ISD) and AS number from the topology configuration.
 */
func ISD_AS() (string, string) {
	var topology map[string]interface{}
	topo_byte, err := ioutil.ReadFile(clientConstants.ScionTopology)
	if err != nil {
		log.Fatalf("Error reading topology file: %v", err)
	}
	json.Unmarshal([]byte(string(topo_byte)), &topology)
	isd_as := topology["isd_as"].(string)

	return strings.Split(isd_as, "-")[0], strings.Split(isd_as, "-")[1]
}
