package pogorobot.service.pure;

import javax.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("loggingHelperService")
public class LogginghelperServiceImpl implements LoggingHelperService {

	private static Logger logger = LoggerFactory.getLogger(LoggingHelperService.class);
	
	@Override
	public void logOptimisticLockException(OptimisticLockException ex, String methodName) {
		logger.error("{} caused OptimisticLockException: ", methodName);
		logger.error("{}", ex.getMessage());
		logStacktraceForMethod(ex.getStackTrace(), methodName);
		if (ex.getCause() != null) {
			logger.error("caused by {}: {}", ex.getCause().getClass().getSimpleName(), ex.getCause().getMessage());
		}
		if (ex.getEntity() != null) {
			logger.error("Entity with problems {}: {}", ex.getEntity().getClass().getSimpleName(), ex.getEntity());
			
		}
	}

	@Override
	public void logStacktraceForMethod(StackTraceElement[] stackTrace, String methodName) {
		boolean lastLineMatched = false;
		for (StackTraceElement stackTraceElement : stackTrace) {
			if (stackTraceElement.getMethodName().contains(methodName)) {
				logStacktraceElement(stackTraceElement);
				lastLineMatched = true;
			} else if (lastLineMatched) {
				logStacktraceElement(stackTraceElement);
				lastLineMatched = false;
			}
		}
	}

	private void logStacktraceElement(StackTraceElement stackTraceElement) {
		logger.error("tracelog: {}.{} (line {})", stackTraceElement.getClassName(), stackTraceElement.getMethodName(), stackTraceElement.getLineNumber());
	}
}
