#!/usr/bin/env bash
# Copyright Scalar, Inc.
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

readonly required_copyright="Copyright Scalar, Inc."
readonly required_license="SPDX-License-Identifier: Apache-2.0"
status=0

while IFS= read -r -d '' file; do
  case "$file" in
    *.java | *.scala | *.kt | *.kts | *.py | *.mill)
      header=$(sed -n '1,12p' "$file")
      if ! grep -Fq "$required_copyright" <<<"$header"; then
        printf 'Missing copyright header: %s\n' "$file" >&2
        status=1
      fi
      if ! grep -Fq "$required_license" <<<"$header"; then
        printf 'Missing SPDX license identifier Apache-2.0: %s\n' "$file" >&2
        status=1
      fi
      ;;
  esac
done < <(git ls-files -z)

exit "$status"
