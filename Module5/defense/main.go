package main

import (
	"time"

	"ethz.ch/netsec/isl/handout/defense/lib"
	"github.com/scionproto/scion/go/lib/slayers"
)

const (
	// Global constants
	MaxAddr           = 2
	MaxAS             = 22
	MaxPath           = 5
	RefreshTime       = 774 * time.Millisecond
	Path_Refresh_Time = 960 * time.Millisecond
)

var (
	// Here, you can define variables that keep state for your firewall
	AddrCount       map[string]int = make(map[string]int, 0)
	ASCount         map[string]int = make(map[string]int, 0)
	LastFreshTime   time.Time
	LastRefreshPath time.Time
	PathCount       = make(map[string]int)
)

func filter(scion slayers.SCION, udp slayers.UDP, payload []byte) bool {
	// Compute path
	raw := make([]byte, scion.Path.Len())
	scion.Path.SerializeTo(raw)
	path := string(raw)
	// Compute Addr and AS
	SrcAddr_str := string(scion.RawSrcAddr)
	SrcAS_str := string(scion.SrcIA.A)
	forget := time.Since(LastFreshTime)

	if forget > RefreshTime {
		AddrCount = make(map[string]int)
		ASCount = make(map[string]int)
		LastFreshTime = time.Now()
	}

	SinceRefresh := time.Since(LastRefreshPath)

	// Refresh IP
	if SinceRefresh > Path_Refresh_Time {
		PathCount = make(map[string]int)
		LastRefreshPath = time.Now()
	}

	if AddrCount[SrcAddr_str] < 1 {
		if PathCount[path] >= MaxPath {
			return false
		} else {
			if AddrCount[SrcAddr_str] < MaxAddr && ASCount[SrcAS_str] < MaxAS {
				AddrCount[SrcAddr_str]++
				ASCount[SrcAS_str]++
			} else {
				return false
			}
			PathCount[path]++
			return true
		}
	} else {
		if AddrCount[SrcAddr_str] < MaxAddr && ASCount[SrcAS_str] < MaxAS {
			AddrCount[SrcAddr_str]++
			ASCount[SrcAS_str]++
			return true
		} else {
			return false
		}
	}

}

func init() {
	// Perform any initial setup here
}

func main() {
	// Start the firewall. Code after this line will not be executed
	lib.RunFirewall(filter)
}
