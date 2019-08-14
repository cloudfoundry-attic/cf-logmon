package main

import (
	"code.cloudfoundry.org/cf-logmon/pkg/logger"
	"log"
	"os"
)

func main() {
	l := log.New(os.Stderr, "some informational prefix", 1)

	logger.NewLogger(21, func(s string) {
		l.Println(s)
	})
}
