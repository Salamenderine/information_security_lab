package lib

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/chifflier/nfqueue-go/nfqueue"
	"github.com/google/gopacket"
	"github.com/google/gopacket/layers"
	"github.com/scionproto/scion/go/lib/slayers"
)

type callbackType func(slayers.SCION, slayers.UDP, []byte) bool

var (
	filterCallback callbackType
)

const (
	// Using literals instead of constants from the C bindings to make the linter happy
	ACCEPT = 1 // nfqueue.NF_ACCEPT
	DROP   = 0 // nfqueue.NF_DROP
)

var (
	targetAddrType = slayers.T4Ip
	// Force conversion to IPv4 to enable comparison with SCION header
	targetAddr = getCustomerIP().To4()
)

func removeEncapsulation(data []byte) ([]byte, error) {
	var ip layers.IPv4
	var udp layers.UDP
	var scion gopacket.Payload
	parser := gopacket.NewDecodingLayerParser(layers.LayerTypeIPv4,
		&ip, &udp, &scion,
	)
	decoded := []gopacket.LayerType{}
	if err := parser.DecodeLayers(data, &decoded); err != nil {
		return nil, err
	}
	return scion, nil
}

func isTargetDestination(header slayers.SCION) bool {
	return header.DstAddrType == targetAddrType &&
		bytes.Equal(header.RawDstAddr, targetAddr)
}

func parse(payload *nfqueue.Payload) int {
	scionData, err := removeEncapsulation(payload.Data)
	if err != nil {
		fmt.Println("Received unexpected packet: ", err)
		return 1
	}

	var scion slayers.SCION
	var udp slayers.UDP
	var pld gopacket.Payload
	parser := gopacket.NewDecodingLayerParser(slayers.LayerTypeSCION,
		&scion, &udp, &pld,
	)
	decoded := []gopacket.LayerType{}
	if err := parser.DecodeLayers(scionData, &decoded); err != nil {
		// fmt.Println("Error parsing packet: ", err)
		payload.SetVerdict(ACCEPT)
		return 0
	}

	// Forward/accept packets that are not sent to the server
	if !isTargetDestination(scion) {
		payload.SetVerdict(ACCEPT)
		return 0
	}

	// Accept (NF_ACCEPT) or drop (NF_DROP) the packet
	if filterCallback(scion, udp, pld) {
		payload.SetVerdict(ACCEPT)
		nPacketsAllowed.Inc()
	} else {
		payload.SetVerdict(DROP)
		nPacketsDropped.Inc()
	}

	return 0
}

func RunFirewall(filter callbackType) {
	filterCallback = filter
	go serveMetrics()

	fmt.Println("Starting firewall")
	log.SetOutput(ioutil.Discard) // netfilter is chatty by default
	q := new(nfqueue.Queue)
	q.SetCallback(parse)
	q.Init()
	q.Unbind(syscall.AF_INET)
	q.Bind(syscall.AF_INET)
	q.CreateQueue(0)

	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGINT, syscall.SIGTERM)
	stop := false
	go func() {
		for sig := range c {
			// sig is a ^C, handle it
			_ = sig
			stop = true
			q.StopLoop()
		}
	}()

	for !stop {
		q.Loop()
	}
	q.DestroyQueue()
	q.Close()
	fmt.Println("Stopping firewall")
}
