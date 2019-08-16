package logger

import (
	"code.cloudfoundry.org/go-loggregator/rpc/loggregator_v2"
	"code.cloudfoundry.org/log-cache/pkg/client"
	"code.cloudfoundry.org/log-cache/pkg/rpc/logcache_v1"
	"golang.org/x/net/context"
	"log"
	"os"
	"strings"
	"time"
)

type Collector struct {
	ctx      context.Context
	message  string
	sourceId string
	reader   client.Reader
}

func NewCollector(
	ctx context.Context,
	message string,
	sourceId string,
	reader client.Reader,
) *Collector {
	return &Collector{
		ctx:      ctx,
		message:  message,
		sourceId: sourceId,
		reader:   reader,
	}
}

func (c *Collector) CollectLogCount(startTime time.Time) int64 {
	var logCount int64
	visitor := func(envs []*loggregator_v2.Envelope) bool {
		for _, env := range envs {
			if strings.Contains(string(env.GetLog().GetPayload()), c.message) {
				logCount++
			}
		}

		return true
	}

	client.Walk(c.ctx, c.sourceId, visitor, c.reader,
		client.WithWalkEnvelopeTypes(logcache_v1.EnvelopeType_LOG),
		client.WithWalkLogger(log.New(os.Stdout, "walk: ", 0)),
		client.WithWalkStartTime(startTime),
		client.WithWalkEndTime(time.Now()),
		client.WithWalkBackoff(client.NewRetryBackoffOnErr(time.Millisecond, 3)),
		client.WithWalkLimit(1000),
	)

	return logCount
}
