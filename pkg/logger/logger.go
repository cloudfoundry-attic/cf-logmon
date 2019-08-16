package logger

import (
	"fmt"
	"time"
)

type Logger struct {
	numLogs      int64
	logSize      int64
	emitDuration time.Duration
	logFunc      func([]string)
}

func NewLogger(numLogs int64, logSize int64, emitDuration time.Duration, logFunc func([] string)) *Logger {
	return &Logger{
		numLogs:      numLogs,
		logSize:      logSize,
		emitDuration: emitDuration,
		logFunc:      logFunc,
	}
}

/**
 * Generate logs at given intervals.
 * For log counts up to 1000, we can generate a reasonable delay between log production events.
 * When the log count goes over 1000, we can't guarantee sub-ms precision, so we emit multiple per
 * millisecond. Any logs that cannot be evenly distributed are front-loaded on the first M milliseconds.
 *
 * With 2 logs:
 *
 * Time: 0------------500------------1000
 *
 * Logs: 1L-----------1L-------------0L -- Complete
 *
 *
 * With 2002 logs:
 *
 * Time: 0---1---2---.....-----------1000
 *
 * Logs: 3L--3L--2L--.....-----------2L -- Complete
 */

func (l *Logger) Emit(logMsg string) {
	ticker := l.newTicker()
	defer ticker.Stop()

	var logsSent int64
	var tick int64
	for range ticker.C {
		logsSent += l.emitLogs(tick, logMsg)

		if logsSent >= l.numLogs {
			return
		}
		tick++
	}
}

func (l *Logger) newTicker() *time.Ticker {
	tickerInterval := time.Millisecond
	if l.numLogs <= int64(l.emitDuration) {
		tickerInterval = time.Duration(int64(l.emitDuration) / l.numLogs)
	}
	ticker := time.NewTicker(tickerInterval)
	return ticker
}

func (l *Logger) emitLogs(emitCount int64, logSuffix string) int64 {
	logsPerMs := l.numLogs / int64(l.emitDuration/time.Millisecond)
	remainingLogs := l.numLogs % int64(l.emitDuration/time.Millisecond)

	logsToEmit := logsPerMs
	if remainingLogs > emitCount {
		logsToEmit++
	}

	log := fmt.Sprintf("%0*s", l.logSize, logSuffix)

	logs := make([]string, logsToEmit)
	for i := int64(0); i < logsToEmit; i++ {
		logs[i] = log
	}
	l.logFunc(logs)

	return logsToEmit
}
