package server

import (
	"encoding/json"
	logging "log"
)

/*
// meow
*/
const MaxBufferSize = 8192

//var ServerPorts
var ServerPorts []uint64 = []uint64{8090, 8091}

// Supported Queries
type Query string

const (
	First  Query = "1"
	Second Query = "2"
	Third  Query = "3"
)

// Message format
type RequestHeader struct {
	Id uint64
	F  Flags
}

type RequestBody struct {
	Query Query
}

type request struct {
	H RequestHeader
	B RequestBody
}

type Flags struct {
	H bool
	V bool
	M bool
	D bool
}

// The meow server will always answer with a string

// Constructor for Request
func NewRequest(q Query, flags ...bool) *JsonRequest {
	if len(flags) > 4 {
		logging.Fatalf("Too many flags!")
	}
	var f = Flags{}
	for i, flag := range flags {
		switch i {
		case 0:
			f.H = flag
		case 1:
			f.V = flag
		case 2:
			f.M = flag
		case 3:
			f.D = flag
		}
	}
	header := RequestHeader{
		Id: 0,
		F:  f,
	}
	body := RequestBody{
		Query: q,
	}
	internalRequest := request{
		H: header,
		B: body,
	}
	answ := JsonRequest{
		jsonD: internalRequest,
	}
	return &answ
}

// Setter
func SetID(id uint64) func(r *JsonRequest) {
	return func(r *JsonRequest) {
		r.jsonD.H.Id = id
	}
}

// Getters
func (r *JsonRequest) ID() uint64 {
	return r.jsonD.H.Id
}

func (r *JsonRequest) Flags() Flags {
	return r.jsonD.H.F
}

func (r *JsonRequest) Query() Query {
	return r.jsonD.B.Query
}

// Serialization
type JsonRequest struct {
	jsonD request
}

func (jr *JsonRequest) MarshalJSON() ([]byte, error) {
	return json.Marshal(&request{
		H: jr.jsonD.H,
		B: jr.jsonD.B,
	})
}
