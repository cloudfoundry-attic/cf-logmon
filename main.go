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
	"time"
)

func main() {
	l := log.New(os.Stderr, "", 1)
	cfg := LoadConfig(l)

	loggr := logger.NewLogger(cfg.LogMessages, cfg.LogSize, cfg.EmitDuration, func(s []string) {
		for _, msg := range s {
			l.Println(msg)
		}
	})

	logSuffix := "cf-logmon-test"

	ctx := context.Background()
	lgClient := logCacheClient(cfg)

	c := logger.NewCollector(ctx, logSuffix, cfg.Vcap.ApplicationID, lgClient.Read)

	var pastReliability []reliabilityStats
	go func() {
		ticker := time.Tick(cfg.RunInterval)

		for range ticker {
			startTime := time.Now()
			loggr.Emit(logSuffix)

			time.Sleep(cfg.LogTransitWait)

			receivedLogCount := c.CollectLogCount(startTime)
			reliability := calculateReliability(cfg.LogMessages, receivedLogCount)

			pastReliability = append(pastReliability,
				reliabilityStats{
					Percent:      reliability,
					EmittedLogs:  cfg.LogMessages,
					ReceivedLogs: receivedLogCount,
					Timestamp:    startTime,
				})
		}
	}()

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		response, err := json.Marshal(pastReliability)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}

		w.Write(response)
	})
	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", cfg.Port), nil))
}

type reliabilityStats struct {
	Percent      float64
	EmittedLogs  int64
	ReceivedLogs int64
	Timestamp    time.Time
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
