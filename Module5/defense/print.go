package main

import (
	"fmt"
	"os"

	"github.com/jedib0t/go-pretty/v6/table"
	"github.com/scionproto/scion/go/lib/slayers"
	spath "github.com/scionproto/scion/go/lib/slayers/path/scion"
)

func prettyPrintSCION(pkt slayers.SCION) {
	t := table.NewWriter()
	t.SetOutputMirror(os.Stdout)
	t.SetTitle("SCION")
	t.AppendRows([]table.Row{
		{"SrcIA", pkt.SrcIA, "SrcAddrType", pkt.SrcAddrType, "RawSrcAddr", pkt.RawSrcAddr},
		{"DstIA", pkt.DstIA, "DstAddrType", pkt.DstAddrType, "RawDstAddr", pkt.RawDstAddr},
	})
	t.Render()

	// Reset the table state to circumvent restriction that multi-column cells are not possible
	t.SetTitle("")
	t.ResetHeaders()
	t.ResetRows()
	t.Style()

	if pkt.PathType == spath.PathType {
		// Decode the path to allow programmatic access to its fields
		raw := make([]byte, pkt.Path.Len())
		pkt.Path.SerializeTo(raw)
		path := &spath.Decoded{}
		path.DecodeFromBytes(raw)

		// Print in table format
		for i, info := range path.InfoFields {
			t.AppendRow(table.Row{
				fmt.Sprintf("InfoFields[%d]", i),
				fmt.Sprintf("{Peer: %v, SegID: %d, Timestamp: %v}",
					info.Peer, info.SegID, info.Timestamp),
			})
		}
		for i, hop := range path.HopFields {
			t.AppendRow(table.Row{
				fmt.Sprintf("HopFields[%d]", i),
				fmt.Sprintf("%v", hop),
			})
		}
	} else {
		t.AppendRow(table.Row{
			"Path", "[non-standard path type]",
		})
	}
	t.Render()
}

func prettyPrintUDP(pkt slayers.UDP) {
	t := table.NewWriter()
	t.SetOutputMirror(os.Stdout)
	t.SetTitle("UDP")
	t.AppendRows([]table.Row{
		{"SrcPort", pkt.SrcPort, "DstPort", pkt.DstPort},
		{"Length", pkt.Length, "Checksum", pkt.Checksum},
	})
	t.Render()
}
