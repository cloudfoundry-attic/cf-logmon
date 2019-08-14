package logger

import "time"

type Logger struct {
	batchSize     int
	batchDuration time.Duration
	logFunc       func(string)
}

func NewLogger(batchSize int, batchDuration time.Duration, logFunc func(string)) *Logger {
	return &Logger{
		batchSize:     batchSize,
		batchDuration: batchDuration,
		logFunc:       logFunc,
	}
}

func (l *Logger) EmitBatch() {

	for i := 0; i < l.batchSize; i++ {
		l.logFunc("")
	}
}
