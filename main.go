package main

import (
	"code.cloudfoundry.org/cf-logmon/pkg/logger"
	"code.cloudfoundry.org/log-cache/pkg/client"
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"
)

func main() {
	l := log.New(os.Stderr, "", 1)
	cfg := LoadConfig(l)
	lock := sync.Mutex{}

	logSuffix := "cf-logmon-test"

	ctx := context.Background()
	lgClient := logCacheClient(cfg)

	c := logger.NewCollector(ctx, logSuffix, cfg.Vcap.ApplicationID, lgClient.Read)

	var pastReliability []reliabilityStats
	go func() {
		loggr := logger.NewLogger(cfg.LogMessages, cfg.LogSize, cfg.EmitDuration, func(s []string) {
			for _, msg := range s {
				l.Println(msg)
			}
		})

		lock.Lock()
		pastReliability = append(pastReliability, runTest(loggr, logSuffix, cfg, c))
		lock.Unlock()

		ticker := time.Tick(cfg.RunInterval)
		for range ticker {
			stats := runTest(loggr, logSuffix, cfg, c)

			lock.Lock()
			pastReliability = append(pastReliability, stats)
			lock.Unlock()
		}
	}()

	http.HandleFunc("/tests", func(w http.ResponseWriter, r *http.Request) {
		lock.Lock()
		response, err := json.Marshal(pastReliability)
		lock.Unlock()

		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}

		w.Write(response)
	})

	http.HandleFunc("/summary", func(w http.ResponseWriter, r *http.Request) {
		total := 0.0

		lock.Lock()
		for _, stat := range pastReliability {
			total += stat.Percent
		}
		avgTotal := total / float64(len(pastReliability))
		lock.Unlock()

		response, err := json.Marshal(summaryStats{avgTotal})
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}

		w.Write(response)
	})

	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", cfg.Port), nil))
}

func runTest(loggr *logger.Logger, logSuffix string, cfg Config, c *logger.Collector) reliabilityStats {
	startTime := time.Now()
	loggr.Emit(logSuffix)
	time.Sleep(cfg.LogTransitWait)

	receivedLogCount := c.CollectLogCount(startTime)
	reliability := calculateReliability(cfg.LogMessages, receivedLogCount)

	return reliabilityStats{
		Percent:           reliability,
		EmittedLogs:       cfg.LogMessages,
		ReceivedLogs:      receivedLogCount,
		BatchEmitDuration: cfg.EmitDuration,
		Timestamp:         startTime,
	}
}

type summaryStats struct {
	TodaysReliability float64
}

type reliabilityStats struct {
	Percent           float64
	EmittedLogs       int64
	ReceivedLogs      int64
	BatchEmitDuration time.Duration
	Timestamp         time.Time
}

func logCacheClient(cfg Config) *client.Client {
	logCacheAddr := strings.Replace(cfg.Vcap.CfApiEndpoint, "api", "log-cache", 1)
	uaaAddr := strings.Replace(cfg.Vcap.CfApiEndpoint, "api", "uaa", 1)

	httpClient := &http.Client{
		Transport: &http.Transport{
			TLSClientConfig: &tls.Config{
				InsecureSkipVerify: cfg.SkipCertVerify,
			},
		},
	}
	oauthClient := client.NewOauth2HTTPClient(
		uaaAddr,
		"cf",
		"",
		client.WithOauth2HTTPUser(cfg.LogUsername, cfg.LogPassword),
		client.WithOauth2HTTPClient(httpClient),
	)
	return client.NewClient(logCacheAddr, client.WithHTTPClient(oauthClient))
}

func calculateReliability(emittedLogs, receivedLogs int64) float64 {
	return float64(receivedLogs) / float64(emittedLogs) * 100
}
