package pogorobot.service.pure;

import javax.persistence.OptimisticLockException;

public interface LoggingHelperService {

	void logStacktraceForMethod(StackTraceElement[] stackTrace, String methodName);

	void logOptimisticLockException(OptimisticLockException ex, String methodName);

}
