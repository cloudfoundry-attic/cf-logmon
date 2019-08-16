package logger_test

import (
	"code.cloudfoundry.org/cf-logmon/pkg/logger"
	"code.cloudfoundry.org/go-loggregator/rpc/loggregator_v2"
	"code.cloudfoundry.org/log-cache/pkg/client"
	"errors"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	"golang.org/x/net/context"
	"time"
)

var _ = Describe("Collector", func() {
	It("reads by app id", func() {
		reader := newFakeReader()

		reader.envs <- []*loggregator_v2.Envelope{
			buildLogEnvelope("some-log-message"),
		}

		c := logger.NewCollector(reader.ctx, "some-log-message", "some-source-id", reader.Read)
		Expect(c.CollectLogCount(time.Now())).To(BeEquivalentTo(1))
		Expect(reader.sourceId).To(Equal("some-source-id"))
	})

	It("filters logs by messages", func() {
		reader := newFakeReader()

		reader.envs <- []*loggregator_v2.Envelope{
			buildLogEnvelope("some-prefix some-log-message"),
			buildLogEnvelope("some-other-log-message"),
			buildLogEnvelope("some-log-message"),
		}

		c := logger.NewCollector(reader.ctx, "some-log-message", "some-source-id", reader.Read)
		Expect(c.CollectLogCount(time.Now())).To(BeEquivalentTo(2))
	})
})

func buildLogEnvelope(msg string) *loggregator_v2.Envelope {
	return &loggregator_v2.Envelope{
		SourceId: "some-source-id",
		Message: &loggregator_v2.Envelope_Log{
			Log: &loggregator_v2.Log{
				Payload: []byte(msg),
			},
		},
	}
}

type fakeReader struct {
	ctx      context.Context
	envs     chan []*loggregator_v2.Envelope
	sourceId string
}

func newFakeReader() *fakeReader {
	return &fakeReader{
		ctx:  context.Background(),
		envs: make(chan []*loggregator_v2.Envelope, 100),
	}
}

func (fr *fakeReader) Read(
	ctx context.Context,
	sourceID string,
	start time.Time,
	opts ...client.ReadOption,
) ([]*loggregator_v2.Envelope, error) {
	if ctx.Err() != nil {
		return nil, ctx.Err()
	}

	if fr.sourceId == "" {
		fr.sourceId = sourceID
	}
	if fr.sourceId != sourceID {
		panic(errors.New("read called with different source IDs"))
	}

	select {
	case env := <-fr.envs:
		return env, nil
	default:
		return []*loggregator_v2.Envelope{
			{
				SourceId: "some-source-id",
				Message: &loggregator_v2.Envelope_Log{
					Log: &loggregator_v2.Log{
						Payload: []byte("now"),
					},
				},
				Timestamp: time.Now().UnixNano(),
			},
		}, nil
	}
}
