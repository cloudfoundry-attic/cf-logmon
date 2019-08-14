package logger_test

import (
	"code.cloudfoundry.org/cf-logmon/pkg/logger"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	"time"
)

var _ = Describe("Logger", func() {
	It("emits the correct number of logs", func() {
		var logsEmitted int
		loggr := logger.NewLogger(17, time.Nanosecond, func(log string) {
			logsEmitted++
		})

		loggr.EmitBatch()
		Expect(logsEmitted).To(Equal(17))
	})

	It("emits logs at the given frequency", func() {
		var logsEmitted int
		loggr := logger.NewLogger(1000, 2 * time.Second, func(log string) {
			logsEmitted++
		})

		startTime := time.Now()
		loggr.EmitBatch()
		endTime := time.Now()

		Expect(endTime).To(BeTemporally("~", startTime.Add(2 * time.Second), time.Second))
	})
})
