# Use an unchecked AnalyticsException

## Question

Should `AnalyticsException`, the single exception type of the error model, be checked or unchecked?

## Answer

Unchecked: `AnalyticsException` extends `RuntimeException`. Each layer catches it only when it needs
to add context or decide something based on the error; otherwise it propagates unchanged to
`ExceptionHandlingInterceptor` on the server or to the caller of the SDK.

## Rationale

- **No wrapping chains**: with a checked exception, every layer boundary has to either declare it
  or catch and rethrow it. Most layers do nothing with an error except pass it on, so those
  declarations and wrappers add classes and noise and hide the original error.
- **It matches where errors are handled**: on the server, `ExceptionHandlingInterceptor` is the one
  place that converts errors into gRPC status codes. Intermediate layers have no error-handling
  responsibility for most failures.
- **Consistency with ScalarDB**: ScalarDB also uses unchecked exceptions for its error model.

## Alternatives

A checked exception would make the compiler force every caller to acknowledge errors, at the cost
of `throws` clauses or catch-and-rethrow code at every layer boundary.

## Consequences

The compiler does not force callers to handle errors, so handling them is a matter of discipline.
An overly broad `catch` in an intermediate layer could swallow an `AnalyticsException`; such catch
blocks are kept out through code review, with the interceptor as the intended catch point.
