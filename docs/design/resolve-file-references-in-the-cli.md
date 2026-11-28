# Resolve file references in the CLI

## Question

Where and when are `${file:<path>}` references in a provider payload replaced with file contents?

## Answer

The CLI replaces them while running `data-source register`, before it sends the payload to the
server. `FilePrefixSubstitutor` in `client/cli` walks the payload as a JSON tree and replaces every
string that is exactly `${file:<path>}`:

- A `.json` file replaces the string with its JSON value, which may be of any JSON type.
- A `.properties` file replaces the string with an object of string values, which fits fields such
  as the ScalarDB provider's `configs` map.
- Any other extension, a missing file, a directory, or invalid JSON fails the command.

References inside a referenced file are not expanded. The server stores the payload it receives,
with all references already resolved.

## Rationale

- The referenced files live on the machine where the user runs the CLI. The catalog server and the
  Spark driver do not need to share a file system with it, and the stored payload is
  self-contained.
- Replacing nodes in the JSON tree, rather than substituting text, avoids escaping and quoting
  problems and lets a `.json` file supply any structure, which the codec then validates as part of
  normal decoding.
- Expanding only one level keeps configuration dependencies explicit; nested references had no
  practical use case that justified the added complexity.

## Consequences

Secrets supplied through a referenced file are stored in the metadata store like any other payload
value. The payload cannot refer to values that only exist where the server or Spark runs, such as
their environment variables.
