package main

import (
	"encoding/json"
	"log"
	"time"

	"code.cloudfoundry.org/go-envstruct"
)

type VcapApplication struct {
	ApplicationID string `json:"application_id"`
	CfApiEndpoint string `json:"cf_api"`
}

func (a *VcapApplication) UnmarshalEnv(data string) error {
	return json.Unmarshal([]byte(data), a)
}

type Config struct {
	LogMessages    int64           `env:"LOG_MESSAGES_PER_BATCH, report"`
	LogSize        int64           `env:"LOG_SIZE_BYTES, report"`
	EmitDuration   time.Duration   `env:"BATCH_EMIT_DURATION, report"`
	RunInterval    time.Duration   `env:"RUN_INTERVAL, report"`
	Vcap           VcapApplication `env:"VCAP_APPLICATION, required"`
	LogUsername    string          `env:"LOGMON_CONSUMPTION_USERNAME, required"`
	LogPassword    string          `env:"LOGMON_CONSUMPTION_PASSWORD, required"`
	SkipCertVerify bool            `env:"SKIP_CERT_VERIFY, required, report"`
	Port           int             `env:"PORT, required, report"`
}

func LoadConfig(log *log.Logger) Config {
	cfg := Config{
		LogMessages:  1000,
		LogSize:      256,
		EmitDuration: time.Second,
		RunInterval:  5 * time.Minute,
	}

	if err := envstruct.Load(&cfg); err != nil {
		log.Fatal(err)
	}

	envstruct.WriteReport(&cfg)

	return cfg
}
