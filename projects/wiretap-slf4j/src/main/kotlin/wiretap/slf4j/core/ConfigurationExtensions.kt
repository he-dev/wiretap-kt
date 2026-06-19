package wiretap.slf4j.core

import org.slf4j.ILoggerFactory
import org.slf4j.LoggerFactory
import wiretap.slf4j.util.LoggerAdapter
import wiretap.util.Configuration

fun Configuration.useDiagnosticsLogger(
    loggerFactory: ILoggerFactory = LoggerFactory.getILoggerFactory(),
    name: String = "wiretap.diagnostics",
): Configuration =
    logDiagnosticsWith(LoggerAdapter(loggerFactory.getLogger(name)))
