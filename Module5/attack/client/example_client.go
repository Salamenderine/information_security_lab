package client

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	// "strings"
	"time"

	"inet.af/netaddr"

	"ethz.ch/netsec/isl/handout/attack/server"
	"github.com/netsec-ethz/scion-apps/pkg/pan"
)

// Example on how to generate a payload with the public meow API
func GenerateClientPayload() []byte {
	// Choose which request to send
	// var q server.Query = server.First
	// Use API to build request
	request := server.NewRequest("4")
	// serialize the request with the API Marshal function
	d, err := request.MarshalJSON()
	if err != nil {
		fmt.Println(err)
		return make([]byte, 0) // empty paiload on fail
	}
	return d
}

// Client is a simple udp-client example which speaks udp over scion through the pan API.
// The payload is sent to the given address exactly once and the answer is printed to
// standard output.
func Client(ctx context.Context, serverAddrPort string, payload []byte) (err error) {

	/* Pan is a high level API provided by the scionlab team which facilitates sending and
	receiving scion traffic. The most common use cases are covered, but solving this lab exercise
	will need more fine grained control than pan provides.
	*/
	serverAddr, err := pan.ParseUDPAddr(serverAddrPort)
	if err != nil {
		log.Fatal(err)
	}
	conn, err := pan.DialUDP(ctx, netaddr.IPPort{}, serverAddr, nil, nil)
	if err != nil {
		fmt.Println("CLIENT: Dial produced an error.", err)
		return
	}
	defer conn.Close()
	n, err := conn.Write(payload)
	if err != nil {
		fmt.Println("CLIENT: Write produced an error.", err)
		return
	}

	fmt.Printf("CLIENT: Packet-written: bytes=%d addr=%s\n", n, serverAddr.String())
	buffer := make([]byte, server.MaxBufferSize)

	// Setting a read deadline makes sure the program doesn't get stuck waiting for an
	// answer from the server for too long.
	deadline := time.Now().Add(time.Second * 3)
	err = conn.SetReadDeadline(deadline)
	if err != nil {
		fmt.Println("CLIENT: SetReadDeadline produced an error.", err)
		return
	}

	nRead, _, err := conn.ReadVia(buffer)
	if err != nil {
		fmt.Println("CLIENT: Error reading from connection.", err)
		return
	}

	fmt.Printf("CLIENT: Packet-received: bytes=%d from=%s\n",
		nRead, conn.RemoteAddr())
	var answer string
	json.Unmarshal(buffer[:nRead], &answer)
	// if !(strings.Contains(answer, "Bad Requet:")) {
	fmt.Printf("CLIENT:The answer was: \n%s", answer)
	// }

	return
}
