package main

// // import (
// // 	"time"

// // 	"ethz.ch/netsec/isl/handout/defense/lib"
// // 	"github.com/scionproto/scion/go/lib/slayers"
// // )

// // const (
// // 	// Global constants
// // 	MaxAddr   = 2
// // 	MaxAs     = 20
// // 	FreshTime = 750 * time.Millisecond
// // )

// // var (
// // 	// Here, you can define variables that keep state for your firewall
// // 	AddrCount     = make(map[string]int, 0)
// // 	AsCount       = make(map[string]int, 0)
// // 	LatestRefresh time.Time
// // )

// // type BlockMessage struct {
// // 	Count     int
// // 	Block     bool
// // 	StartTime time.Time
// // }

// // // This function receives all packets destined to the customer server.
// // //
// // // Your task is to decide whether to forward or drop a packet based on the
// // // headers and payload.
// // // References for the given packet types:
// // // - SCION header
// // //   https://pkg.go.dev/github.com/scionproto/scion/go/lib/slayers#SCION
// // // - UDP header
// // //   https://pkg.go.dev/github.com/scionproto/scion/go/lib/slayers#UDP
// // //

// // // func filter(scion slayers.SCION, udp slayers.UDP, payload []byte) bool {
// // // 	// Print packet contents (disable this before submitting your code)
// // // 	prettyPrintSCION(scion)
// // // 	prettyPrintUDP(udp)
// // // 	fmt.Println("FlowID:", scion.FlowID)
// // // 	fmt.Println("PayloadLen:", scion.PayloadLen)
// // // 	fmt.Println("NextHdr:", scion.NextHdr)
// // // 	return true
// // // }

// // func filter(scion slayers.SCION, udp slayers.UDP, payload []byte) bool {
// // 	Addr := string(scion.RawDstAddr)
// // 	AS := string(scion.SrcIA.A)
// // 	interval := time.Now().Sub(LatestRefresh)

// // 	if interval > FreshTime {
// // 		for item := range AddrCount {
// // 			delete(AddrCount, item)
// // 		}
// // 		for item := range AsCount {
// // 			delete(AsCount, item)
// // 		}
// // 		LatestRefresh = time.Now()
// // 	}

// // 	if AddrCount[Addr] < MaxAddr && AsCount[AS] < MaxAs {
// // 		AddrCount[Addr]++
// // 		AsCount[AS]++
// // 		return true
// // 	} else {
// // 		return false
// // 	}
// // }

// // // func filter(scion slayers.SCION, udp slayers.UDP, payload []byte) bool {
// // // 	// Print packet contents (disable this before submitting your code)
// // // 	prettyPrintSCION(scion)
// // // 	prettyPrintUDP(udp)

// // // 	// IP := scion.SrcIA.String() + string(scion.RawSrcAddr) + string(udp.SrcPort) + scion.DstIA.String() + string(scion.RawDstAddr)
// // // 	// IP := string(scion.RawSrcAddr)
// // // 	IP := scion.SrcIA.String() + string(scion.RawSrcAddr)

// // // 	RequestCount[IP]++
// // // 	if RequestCount[IP] >= MaxRequest {
// // // 		return false
// // // 	}
// // // 	// Decision
// // // 	// | true  -> forward packet
// // // 	// | false -> drop packet
// // // 	return true
// // // }

// // func init() {
// // 	// Perform any initial setup here
// // 	LatestRefresh = time.Now()
// // }

// // func main() {
// // 	// Start the firewall. Code after this line will not be executed
// // 	lib.RunFirewall(filter)
// // }

// // const (
// // 	// Global constants
// // 	MaxAddr           = 2
// // 	MaxAS             = 20
// // 	MaxPath           = 5
// // 	RefreshTime       = 750 * time.Millisecond
// // 	Path_Refresh_Time = 950 * time.Millisecond
// // )

// // var (
// // 	// Here, you can define variables that keep state for your firewall
// // 	SrcAddrMap      map[string]int = make(map[string]int, 0)
// // 	SrcASMap        map[string]int = make(map[string]int, 0)
// // 	LastFreshTime   time.Time
// // 	LastRefreshPath time.Time
// // 	PathCount       = make(map[string]int)
// // )

// // For def3
// func filter(scion slayers.SCION, udp slayers.UDP, payload []byte) bool {
// 	// Compute path
// 	raw := make([]byte, scion.Path.Len())
// 	scion.Path.SerializeTo(raw)
// 	path := string(raw)
// 	// Compute Addr and AS

// 	SinceRefresh := time.Since(LastRefreshPath)

// 	if SinceRefresh > Path_Refresh_Time {
// 		PathCount = make(map[string]int)
// 		LastRefreshPath = time.Now()
// 	}

// 	if PathCount[path] >= MaxPath {
// 		return false
// 	} else {
// 		PathCount[path]++
// 		return true
// 	}
// }

// // This function receives all packets destined to the customer server.
// //
// // Your task is to decide whether to forward or drop a packet based on the
// // headers and payload.
// // References for the given packet types:
// // - SCION header
// //   https://pkg.go.dev/github.com/scionproto/scion/go/lib/slayers#SCION
// // - UDP header
// //   https://pkg.go.dev/github.com/scionproto/scion/go/lib/slayers#UDP
// //
// // func filter(scion slayers.SCION, udp slayers.UDP, payload []byte) bool {

// // 	SrcAddr_str := string(scion.RawSrcAddr)
// // 	SrcAS_str := string(scion.SrcIA.A)
// // 	interval := time.Since(LastFreshTime)
// // 	// interval := time.Now().Sub(LastFreshTime)

// // 	if interval > RefreshTime {
// // 		SrcAddrMap = make(map[string]int)
// // 		SrcASMap = make(map[string]int)
// // 		LastFreshTime = time.Now()
// // 	}

// // 	if SrcAddrMap[SrcAddr_str] < MaxAddr && SrcASMap[SrcAS_str] < MaxAS {
// // 		SrcAddrMap[SrcAddr_str]++
// // 		SrcASMap[SrcAS_str]++
// // 		return true
// // 	} else {
// // 		return false
// // 	}
// // }

// // func filter(scion slayers.SCION, udp slayers.UDP, payload []byte) bool {
// // 	// Compute path
// // 	raw := make([]byte, scion.Path.Len())
// // 	scion.Path.SerializeTo(raw)
// // 	path := string(raw)
// // 	// Compute Addr and AS
// // 	SrcAddr_str := string(scion.RawSrcAddr)
// // 	SrcAS_str := string(scion.SrcIA.A)
// // 	interval := time.Since(LastFreshTime)

// // 	SinceRefresh := time.Since(LastRefreshPath)

// // 	// Refresh IP
// // 	if SinceRefresh > Path_Refresh_Time {
// // 		PathCount = make(map[string]int)
// // 		LastRefreshPath = time.Now()
// // 	}

// // 	// Refresh Path
// // 	interval = time.Since(LastFreshTime)

// // 	if interval > RefreshTime {
// // 		SrcAddrMap = make(map[string]int)
// // 		SrcASMap = make(map[string]int)
// // 		LastFreshTime = time.Now()
// // 	}

// // 	if SrcAddrMap[SrcAddr_str] <= 1 {
// // 		if PathCount[path] >= MaxPath {
// // 			return false
// // 		} else {
// // 			PathCount[path]++
// // 			return true
// // 		}
// // 	} else {
// // 		if SrcAddrMap[SrcAddr_str] < MaxAddr && SrcASMap[SrcAS_str] < MaxAS {
// // 			SrcAddrMap[SrcAddr_str]++
// // 			SrcASMap[SrcAS_str]++
// // 			return true
// // 		} else {
// // 			return false
// // 		}
// // 	}

// // }

// // func init() {
// // 	// Perform any initial setup here
// // }

// // func main() {
// // 	// Start the firewall. Code after this line will not be executed
// // 	lib.RunFirewall(filter)
// // }
