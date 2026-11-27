#!/usr/bin/env bash
# Copyright Scalar, Inc.
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

until healthcheck.sh FREEPDB1 "container status ignore"; do
  sleep 1
done
