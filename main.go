package main

import (
	"code.cloudfoundry.org/cf-logmon/pkg/logger"
	"code.cloudfoundry.org/log-cache/pkg/client"
	"context"
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
	startTime := time.Now()
	loggr.Emit(logSuffix)

	ctx := context.Background()
	lgClient := logCacheClient(cfg)

	c := logger.NewCollector(ctx, logSuffix, cfg.Vcap.ApplicationID, lgClient.Read)
	time.Sleep(10 * time.Second)
	receivedLogCount := c.CollectLogCount(startTime)

	reliability := calculateReliability(cfg.LogMessages, receivedLogCount)

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "Log Cache Reliability: %f", reliability)
	})

	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", cfg.Port), nil))
}

func logCacheClient(cfg Config) *client.Client {
	logCacheAddr := strings.Replace(cfg.Vcap.CfApiEndpoint, "api", "log-cache", 1)
	uaaAddr := strings.Replace(cfg.Vcap.CfApiEndpoint, "api", "uaa", 1)

	oauthClient := client.NewOauth2HTTPClient(
		uaaAddr,
		"cf",
		"",
		client.WithOauth2HTTPUser(cfg.LogUsername, cfg.LogPassword))
	return client.NewClient(logCacheAddr, client.WithHTTPClient(oauthClient))
}

func calculateReliability(emittedLogs, receivedLogs int64) float64 {
	return float64(receivedLogs) / float64(emittedLogs) * 100
}
