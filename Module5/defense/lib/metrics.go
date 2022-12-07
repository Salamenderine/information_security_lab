package lib

import (
	"net/http"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	nPackets = promauto.NewCounterVec(
		prometheus.CounterOpts{
			Namespace: "isl",
			Subsystem: "defense",
			Name:      "firewall_pkts",
			Help:      "Packets received by the firewall",
		},
		[]string{"verdict"},
	)

	nPacketsAllowed = nPackets.WithLabelValues("accept")
	nPacketsDropped = nPackets.WithLabelValues("drop")
)

func serveMetrics() {
	http.Handle("/metrics", promhttp.Handler())
	http.ListenAndServe("127.0.0.1:9094", nil)
}
