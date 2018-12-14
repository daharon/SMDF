# Serverless Monitoring Development Kit
### TODO
- [ ] Refactor DynamoDB table into single table.
    - https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.CoreComponents.html?shortFooter=true
    - https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html
- [ ] Refactor DSL to use Builder Pattern and `@DslMarker` annotation correctly.
    - https://kotlinlang.org/docs/reference/type-safe-builders.html#scope-control-dslmarker-since-11
    - https://proandroiddev.com/writing-kotlin-dsls-with-nested-builder-pattern-66452476d5ef
    - https://youtu.be/Rvx_BfG3NDo?t=2090
- [ ] Specify `Environment` for all AWS resources.
    - See `Deploy` class.
- [ ] Create IAM Role with specified policies for the `NotificationHandler` abstract base class.
    - Assume the appropriate IAM Role in the `NotificationProcessor`.
- [ ] Create IAM Role with the specified policies for the `ServerlessExecutor` abstract base class.    
    - Assume the appropriate IAM Role in the `ServerlessCheckProcessor`.
- [ ] Refactor logging to use a different backend.
    - Lambda cold-start time may be improved by using `tinylog` or `logback`.
