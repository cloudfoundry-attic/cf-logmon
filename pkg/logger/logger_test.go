package logger_test

import (
	"code.cloudfoundry.org/cf-logmon/pkg/logger"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
	"time"
)

var _ = Describe("Logger", func() {
	It("emits the passed string as a log message", func() {
		loggr:= logger.NewLogger(1, 100, time.Millisecond, func(logs []string) {
			for _, log := range logs {
				Expect(log).To(ContainSubstring("Some-log"))
			}
		})

		loggr.Emit("Some-log")
	})

	It("emits log messages of specified size", func(){
		loggr:= logger.NewLogger(1, 15, time.Millisecond, func(logs []string) {
			for _, log := range logs {
				Expect(log).To(HaveLen(15))
			}
		})

		loggr.Emit("Some-log")
	})

	It("emits logs at a constant rate", func() {
		last := time.Now()
		var emitTimeNanos []int64
		loggr := logger.NewLogger(1000, 8, 1000*time.Millisecond, func(log []string) {
			dur := time.Since(last)
			emitTimeNanos = append(emitTimeNanos, dur.Nanoseconds())
			last = time.Now()
		})

		loggr.Emit("Some-log")

		Expect(emitTimeNanos).To(HaveLen(1000))

		var avg int64
		for _, nanos := range emitTimeNanos {
			avg = avg + nanos
		}
		avgMillis := float64(avg) / float64(len(emitTimeNanos)) / 1000000.0 //Nanos Per milli
		Expect(avgMillis).To(BeNumerically("~", 1, 0.01))
	})

	It("batches emissions when logs Per millisecond > 1", func() {
		var emittedLogs [][]string
		loggr := logger.NewLogger(2002, 8, 1000*time.Millisecond, func(logs []string) {
			emittedLogs = append(emittedLogs, logs)
		})
		loggr.Emit("Some-log")

		Expect(emittedLogs).To(HaveLen(1000))
		for i, logs := range emittedLogs {
			if i < 2 {
				Expect(logs).To(HaveLen(3))
			} else {
				Expect(logs).To(HaveLen(2))
			}
		}
	})

	It("batches emissions when logs Per millisecond <= 1", func() {
		var emittedLogs [][]string
		loggr := logger.NewLogger(2, 8, 1000*time.Millisecond, func(logs []string) {
			emittedLogs = append(emittedLogs, logs)
		})
		go loggr.Emit("Some-log")

		Consistently(func() int {
			return len(emittedLogs)
		}, 450*time.Millisecond).Should(BeNumerically("<=", 1)) //The logs should be spread across the time range evenly

		Eventually(func() int {
			return len(emittedLogs)
		}).Should(Equal(2))
	})
})
